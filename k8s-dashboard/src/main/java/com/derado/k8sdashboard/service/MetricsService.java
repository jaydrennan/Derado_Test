package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.PodResourceUsage;
import com.derado.k8sdashboard.dto.ResourceUsage;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.custom.NodeMetricsList;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.PodMetricsList;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final Metrics metrics;
    private final CoreV1Api coreV1Api;

    public MetricsService(ApiClient apiClient) {
        this.metrics = new Metrics(apiClient);
        this.coreV1Api = new CoreV1Api(apiClient);
    }

    public List<ResourceUsage> getNodeMetrics() {
        try {
            NodeMetricsList metricsList = metrics.getNodeMetrics();
            V1NodeList nodeList = coreV1Api.listNode().execute();

            Map<String, V1Node> nodeMap = nodeList.getItems().stream()
                    .collect(Collectors.toMap(
                            n -> n.getMetadata() != null ? n.getMetadata().getName() : "",
                            n -> n
                    ));

            return metricsList.getItems().stream()
                    .map(nm -> mapToResourceUsage(nm, nodeMap))
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.warn("Metrics server not available: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to retrieve node metrics: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<PodResourceUsage> getPodMetrics(String namespace) {
        try {
            if (namespace != null && !namespace.isBlank()) {
                PodMetricsList metricsList = metrics.getPodMetrics(namespace);
                return metricsList.getItems().stream()
                        .map(this::mapToPodResourceUsage)
                        .collect(Collectors.toList());
            }
            // For all namespaces, list each namespace and aggregate
            var namespaces = coreV1Api.listNamespace().execute();
            List<PodResourceUsage> allPodMetrics = new ArrayList<>();
            for (var ns : namespaces.getItems()) {
                String nsName = ns.getMetadata() != null ? ns.getMetadata().getName() : null;
                if (nsName == null) continue;
                try {
                    PodMetricsList metricsList = metrics.getPodMetrics(nsName);
                    metricsList.getItems().stream()
                            .map(this::mapToPodResourceUsage)
                            .forEach(allPodMetrics::add);
                } catch (Exception e) {
                    log.debug("No pod metrics for namespace {}: {}", nsName, e.getMessage());
                }
            }
            return allPodMetrics;
        } catch (ApiException e) {
            log.warn("Metrics server not available for pods: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to retrieve pod metrics: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private PodResourceUsage mapToPodResourceUsage(PodMetrics pm) {
        String name = pm.getMetadata() != null ? pm.getMetadata().getName() : "unknown";
        String namespace = pm.getMetadata() != null ? pm.getMetadata().getNamespace() : "";

        List<PodResourceUsage.ContainerResourceUsage> containers = new ArrayList<>();
        BigDecimal totalCpu = BigDecimal.ZERO;
        BigDecimal totalMem = BigDecimal.ZERO;

        if (pm.getContainers() != null) {
            for (ContainerMetrics cm : pm.getContainers()) {
                var usage = cm.getUsage();
                String containerCpu = usage != null && usage.get("cpu") != null
                        ? usage.get("cpu").toSuffixedString() : "0";
                String containerMem = usage != null && usage.get("memory") != null
                        ? usage.get("memory").toSuffixedString() : "0";

                if (usage != null && usage.get("cpu") != null) {
                    try { totalCpu = totalCpu.add(usage.get("cpu").getNumber()); } catch (Exception ignored) {}
                }
                if (usage != null && usage.get("memory") != null) {
                    try { totalMem = totalMem.add(usage.get("memory").getNumber()); } catch (Exception ignored) {}
                }

                containers.add(new PodResourceUsage.ContainerResourceUsage(
                        cm.getName(), containerCpu, containerMem
                ));
            }
        }

        String cpuUsage = totalCpu.compareTo(BigDecimal.ZERO) > 0
                ? formatCpu(totalCpu) : "0";
        String memoryUsage = totalMem.compareTo(BigDecimal.ZERO) > 0
                ? formatMemory(totalMem) : "0";

        return new PodResourceUsage(name, namespace, cpuUsage, memoryUsage, containers);
    }

    private String formatCpu(BigDecimal cpuCores) {
        BigDecimal millis = cpuCores.multiply(BigDecimal.valueOf(1000));
        if (millis.compareTo(BigDecimal.ONE) < 0) {
            return cpuCores.toPlainString();
        }
        return millis.setScale(0, java.math.RoundingMode.HALF_UP) + "m";
    }

    private String formatMemory(BigDecimal memBytes) {
        BigDecimal ki = BigDecimal.valueOf(1024);
        BigDecimal mi = ki.multiply(ki);
        if (memBytes.compareTo(mi) >= 0) {
            return memBytes.divide(mi, 1, java.math.RoundingMode.HALF_UP) + "Mi";
        }
        return memBytes.divide(ki, 0, java.math.RoundingMode.HALF_UP) + "Ki";
    }

    private ResourceUsage mapToResourceUsage(NodeMetrics nm, Map<String, V1Node> nodeMap) {
        String name = nm.getMetadata() != null ? nm.getMetadata().getName() : "unknown";
        var usage = nm.getUsage();

        String cpuUsage = usage != null && usage.get("cpu") != null
                ? usage.get("cpu").toSuffixedString() : "0";
        String memUsage = usage != null && usage.get("memory") != null
                ? usage.get("memory").toSuffixedString() : "0";

        String cpuCapacity = "0";
        String memCapacity = "0";
        double cpuPercent = 0;
        double memPercent = 0;

        V1Node node = nodeMap.get(name);
        if (node != null && node.getStatus() != null && node.getStatus().getCapacity() != null) {
            var capacity = node.getStatus().getCapacity();
            if (capacity.get("cpu") != null) {
                cpuCapacity = capacity.get("cpu").toSuffixedString();
                try {
                    cpuPercent = capacity.get("cpu").getNumber().doubleValue() > 0
                            ? (usage != null && usage.get("cpu") != null
                                ? usage.get("cpu").getNumber().doubleValue() / capacity.get("cpu").getNumber().doubleValue() * 100
                                : 0)
                            : 0;
                } catch (Exception ignored) {}
            }
            if (capacity.get("memory") != null) {
                memCapacity = capacity.get("memory").toSuffixedString();
                try {
                    memPercent = capacity.get("memory").getNumber().doubleValue() > 0
                            ? (usage != null && usage.get("memory") != null
                                ? usage.get("memory").getNumber().doubleValue() / capacity.get("memory").getNumber().doubleValue() * 100
                                : 0)
                            : 0;
                } catch (Exception ignored) {}
            }
        }

        return new ResourceUsage(
                name, cpuUsage, memUsage, cpuCapacity, memCapacity,
                Math.round(cpuPercent * 10.0) / 10.0,
                Math.round(memPercent * 10.0) / 10.0
        );
    }
}
