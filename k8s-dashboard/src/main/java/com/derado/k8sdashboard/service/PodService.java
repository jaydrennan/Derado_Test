package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.PodInfo;
import com.derado.k8sdashboard.exception.KubernetesApiException;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PodService {

    private final CoreV1Api coreV1Api;

    public PodService(ApiClient apiClient) {
        this.coreV1Api = new CoreV1Api(apiClient);
    }

    public List<PodInfo> getPods(String namespace) {
        try {
            V1PodList podList;
            if (namespace != null && !namespace.isBlank()) {
                podList = coreV1Api.listNamespacedPod(namespace).execute();
            } else {
                podList = coreV1Api.listPodForAllNamespaces().execute();
            }
            return podList.getItems().stream()
                    .map(this::mapToPodInfo)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new KubernetesApiException("Failed to list pods", e);
        }
    }

    private PodInfo mapToPodInfo(V1Pod pod) {
        var metadata = pod.getMetadata();
        var status = pod.getStatus();
        var spec = pod.getSpec();

        String phase = status != null && status.getPhase() != null ? status.getPhase() : "Unknown";

        int restartCount = 0;
        List<PodInfo.ContainerInfo> containers = Collections.emptyList();

        if (status != null && status.getContainerStatuses() != null) {
            restartCount = status.getContainerStatuses().stream()
                    .mapToInt(V1ContainerStatus::getRestartCount)
                    .sum();
            containers = status.getContainerStatuses().stream()
                    .map(cs -> new PodInfo.ContainerInfo(
                            cs.getName(),
                            cs.getImage(),
                            Boolean.TRUE.equals(cs.getReady()),
                            getContainerState(cs)
                    ))
                    .collect(Collectors.toList());
        }

        List<V1Container> specContainers = spec != null && spec.getContainers() != null
                ? spec.getContainers() : Collections.emptyList();

        String cpuRequest = sumQuantity(specContainers, "cpu", true);
        String cpuLimit = sumQuantity(specContainers, "cpu", false);
        String memoryRequest = sumQuantity(specContainers, "memory", true);
        String memoryLimit = sumQuantity(specContainers, "memory", false);

        return new PodInfo(
                metadata != null ? metadata.getName() : "unknown",
                metadata != null ? metadata.getNamespace() : "",
                phase,
                spec != null ? spec.getNodeName() : "",
                status != null && status.getPodIP() != null ? status.getPodIP() : "",
                restartCount,
                containers,
                metadata != null && metadata.getCreationTimestamp() != null
                        ? metadata.getCreationTimestamp().toString() : "",
                cpuRequest,
                cpuLimit,
                memoryRequest,
                memoryLimit
        );
    }

    private String sumQuantity(List<V1Container> containers, String resourceName, boolean isRequest) {
        BigDecimal total = BigDecimal.ZERO;
        boolean found = false;
        for (V1Container container : containers) {
            V1ResourceRequirements resources = container.getResources();
            if (resources == null) continue;
            Map<String, Quantity> map = isRequest ? resources.getRequests() : resources.getLimits();
            if (map != null && map.get(resourceName) != null) {
                try {
                    total = total.add(map.get(resourceName).getNumber());
                    found = true;
                } catch (Exception ignored) {}
            }
        }
        if (!found) return "-";
        return new Quantity(total, Quantity.Format.DECIMAL_SI).toSuffixedString();
    }

    private String getContainerState(V1ContainerStatus cs) {
        if (cs.getState() == null) return "Unknown";
        if (cs.getState().getRunning() != null) return "Running";
        if (cs.getState().getWaiting() != null) {
            return "Waiting" + (cs.getState().getWaiting().getReason() != null
                    ? ": " + cs.getState().getWaiting().getReason() : "");
        }
        if (cs.getState().getTerminated() != null) {
            return "Terminated" + (cs.getState().getTerminated().getReason() != null
                    ? ": " + cs.getState().getTerminated().getReason() : "");
        }
        return "Unknown";
    }
}
