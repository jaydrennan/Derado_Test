package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.NodeInfo;
import com.derado.k8sdashboard.exception.KubernetesApiException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeList;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NodeService {

    private static final Set<String> PRESSURE_CONDITIONS = Set.of(
            "DiskPressure", "MemoryPressure", "PIDPressure"
    );

    private final CoreV1Api coreV1Api;

    public NodeService(ApiClient apiClient) {
        this.coreV1Api = new CoreV1Api(apiClient);
    }

    public List<NodeInfo> getAllNodes() {
        try {
            V1NodeList nodeList = coreV1Api.listNode().execute();
            return nodeList.getItems().stream()
                    .map(this::mapToNodeInfo)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new KubernetesApiException("Failed to list nodes", e);
        }
    }

    private NodeInfo mapToNodeInfo(V1Node node) {
        var metadata = node.getMetadata();
        var status = node.getStatus();
        var nodeInfo = status != null ? status.getNodeInfo() : null;

        String nodeStatus = "Unknown";
        if (status != null && status.getConditions() != null) {
            nodeStatus = status.getConditions().stream()
                    .filter(c -> "Ready".equals(c.getType()))
                    .findFirst()
                    .map(V1NodeCondition::getStatus)
                    .map(s -> "True".equals(s) ? "Ready" : "NotReady")
                    .orElse("Unknown");
        }

        Map<String, String> capacity = status != null && status.getCapacity() != null
                ? status.getCapacity().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toSuffixedString()))
                : Collections.emptyMap();

        Map<String, String> allocatable = status != null && status.getAllocatable() != null
                ? status.getAllocatable().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toSuffixedString()))
                : Collections.emptyMap();

        List<String> conditions = new ArrayList<>();
        if (status != null && status.getConditions() != null) {
            for (V1NodeCondition c : status.getConditions()) {
                if (PRESSURE_CONDITIONS.contains(c.getType()) && "True".equals(c.getStatus())) {
                    conditions.add(c.getType());
                }
            }
        }

        return new NodeInfo(
                metadata != null ? metadata.getName() : "unknown",
                nodeStatus,
                nodeInfo != null ? nodeInfo.getKubeletVersion() : "",
                nodeInfo != null ? nodeInfo.getOsImage() : "",
                nodeInfo != null ? nodeInfo.getArchitecture() : "",
                nodeInfo != null ? nodeInfo.getContainerRuntimeVersion() : "",
                capacity,
                allocatable,
                metadata != null && metadata.getLabels() != null ? metadata.getLabels() : Collections.emptyMap(),
                metadata != null && metadata.getCreationTimestamp() != null
                        ? metadata.getCreationTimestamp().toString() : "",
                conditions
        );
    }
}
