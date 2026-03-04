package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.CalicoNodeMetrics;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import org.springframework.beans.factory.annotation.Autowired;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CalicoMetricsService {

    private static final Logger log = LoggerFactory.getLogger(CalicoMetricsService.class);

    private static final String CALICO_NAMESPACE = "kube-system";
    private static final String CALICO_LABEL_SELECTOR = "k8s-app=calico-node";
    private static final String FELIX_ACTIVE_LOCAL_ENDPOINTS = "felix_active_local_endpoints";
    private static final String FELIX_IPTABLES_SAVE_ERRORS_TOTAL = "felix_iptables_save_errors_total";

    private static final Pattern FELIX_ACTIVE_LOCAL_ENDPOINTS_PATTERN =
            buildMetricPattern(FELIX_ACTIVE_LOCAL_ENDPOINTS);
    private static final Pattern FELIX_IPTABLES_SAVE_ERRORS_TOTAL_PATTERN =
            buildMetricPattern(FELIX_IPTABLES_SAVE_ERRORS_TOTAL);

    private final CoreV1Api coreV1Api;
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, Long> previousErrorCountsByNode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> previousReachabilityByNode = new ConcurrentHashMap<>();

    @Autowired
    public CalicoMetricsService(ApiClient apiClient, RestTemplate restTemplate) {
        this(new CoreV1Api(apiClient), restTemplate);
    }

    CalicoMetricsService(CoreV1Api coreV1Api, RestTemplate restTemplate) {
        this.coreV1Api = coreV1Api;
        this.restTemplate = restTemplate;
    }

    public List<CalicoNodeMetrics> getCalicoNodeMetrics() {
        List<V1Pod> calicoPods = listCalicoNodePods();
        if (calicoPods.isEmpty()) {
            return List.of();
        }

        List<CalicoNodeMetrics> metrics = new ArrayList<>();
        for (V1Pod pod : calicoPods) {
            metrics.add(fetchNodeMetrics(pod));
        }
        return metrics;
    }

    @Scheduled(fixedDelayString = "${calico.metrics.poll-interval-ms:30000}")
    public void monitorCalicoHealth() {
        List<CalicoNodeMetrics> metrics = getCalicoNodeMetrics();
        Set<String> observedNodes = new HashSet<>();

        for (CalicoNodeMetrics nodeMetrics : metrics) {
            String nodeKey = buildNodeKey(nodeMetrics);
            observedNodes.add(nodeKey);

            if (!nodeMetrics.reachable()) {
                Boolean previousState = previousReachabilityByNode.put(nodeKey, false);
                if (previousState == null || previousState) {
                    log.warn(
                            "Calico metrics endpoint became unreachable for node {} (pod {} at {}): {}",
                            nodeMetrics.nodeName(),
                            nodeMetrics.calicoNodePodName(),
                            nodeMetrics.podIp(),
                            nodeMetrics.errorMessage()
                    );
                }
                continue;
            }

            previousReachabilityByNode.put(nodeKey, true);

            Long currentErrors = nodeMetrics.iptablesSaveErrorsTotal();
            if (currentErrors != null) {
                Long previousErrors = previousErrorCountsByNode.put(nodeKey, currentErrors);
                if (previousErrors != null && currentErrors > previousErrors) {
                    log.warn(
                            "{} increased on node {} from {} to {}",
                            FELIX_IPTABLES_SAVE_ERRORS_TOTAL,
                            nodeMetrics.nodeName(),
                            previousErrors,
                            currentErrors
                    );
                }
            }
        }

        previousErrorCountsByNode.keySet().removeIf(node -> !observedNodes.contains(node));
        previousReachabilityByNode.keySet().removeIf(node -> !observedNodes.contains(node));
    }

    private List<V1Pod> listCalicoNodePods() {
        try {
            V1PodList podList = coreV1Api.listNamespacedPod(CALICO_NAMESPACE)
                    .labelSelector(CALICO_LABEL_SELECTOR)
                    .execute();
            if (podList.getItems() == null) {
                return List.of();
            }
            return podList.getItems();
        } catch (ApiException e) {
            log.warn("Failed to list Calico pods: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.warn("Unexpected error while listing Calico pods: {}", e.getMessage());
            return List.of();
        }
    }

    private CalicoNodeMetrics fetchNodeMetrics(V1Pod pod) {
        String podName = pod.getMetadata() != null ? pod.getMetadata().getName() : "unknown";
        String nodeName = pod.getSpec() != null ? pod.getSpec().getNodeName() : "unknown";
        String podIp = pod.getStatus() != null ? pod.getStatus().getPodIP() : null;

        if (podIp == null || podIp.isBlank()) {
            return new CalicoNodeMetrics(
                    nodeName,
                    podName,
                    podIp,
                    null,
                    null,
                    false,
                    "Calico pod has no Pod IP"
            );
        }

        String endpoint = "http://" + podIp + ":9091/metrics";
        try {
            String rawText = restTemplate.getForObject(endpoint, String.class);
            ParsedCalicoMetrics parsed = parseCalicoMetrics(rawText != null ? rawText : "");
            return new CalicoNodeMetrics(
                    nodeName,
                    podName,
                    podIp,
                    parsed.activeLocalEndpoints(),
                    parsed.iptablesSaveErrorsTotal(),
                    true,
                    null
            );
        } catch (RestClientException e) {
            return new CalicoNodeMetrics(
                    nodeName,
                    podName,
                    podIp,
                    null,
                    null,
                    false,
                    e.getMessage()
            );
        }
    }

    private ParsedCalicoMetrics parseCalicoMetrics(String rawText) {
        Long activeLocalEndpoints = extractMetricValue(rawText, FELIX_ACTIVE_LOCAL_ENDPOINTS_PATTERN);
        Long iptablesSaveErrorsTotal = extractMetricValue(rawText, FELIX_IPTABLES_SAVE_ERRORS_TOTAL_PATTERN);
        return new ParsedCalicoMetrics(activeLocalEndpoints, iptablesSaveErrorsTotal);
    }

    private Long extractMetricValue(String rawText, Pattern metricPattern) {
        Matcher matcher = metricPattern.matcher(rawText);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        try {
            return new BigDecimal(value).longValue();
        } catch (NumberFormatException e) {
            log.debug("Failed to parse metric value '{}': {}", value, e.getMessage());
            return null;
        }
    }

    private static String buildNodeKey(CalicoNodeMetrics metrics) {
        if (metrics.nodeName() != null && !metrics.nodeName().isBlank()) {
            return metrics.nodeName();
        }
        return metrics.calicoNodePodName() != null ? metrics.calicoNodePodName() : "unknown";
    }

    private static Pattern buildMetricPattern(String metricName) {
        String valuePattern = "([+-]?(?:\\d+\\.?\\d*|\\d*\\.\\d+)(?:[eE][+-]?\\d+)?)";
        String pattern = "(?m)^" + metricName + "(?:\\{[^}]*})?\\s+" + valuePattern + "(?:\\s+.*)?$";
        return Pattern.compile(pattern);
    }

    private record ParsedCalicoMetrics(Long activeLocalEndpoints, Long iptablesSaveErrorsTotal) {
    }
}
