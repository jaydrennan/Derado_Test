package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.CalicoNodeMetrics;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalicoMetricsServiceTest {

    @Mock
    private CoreV1Api coreV1Api;

    @Mock
    private CoreV1Api.APIlistNamespacedPodRequest listNamespacedPodRequest;

    @Mock
    private RestTemplate restTemplate;

    private CalicoMetricsService calicoMetricsService;

    @BeforeEach
    void setUp() {
        calicoMetricsService = new CalicoMetricsService(coreV1Api, restTemplate);
    }

    @Test
    void getCalicoNodeMetrics_parsesFelixMetrics() throws Exception {
        V1Pod pod = buildCalicoPod("calico-node-abc", "kind-worker", "10.244.1.2");
        when(coreV1Api.listNamespacedPod(eq("kube-system"))).thenReturn(listNamespacedPodRequest);
        when(listNamespacedPodRequest.labelSelector(eq("k8s-app=calico-node"))).thenReturn(listNamespacedPodRequest);
        when(listNamespacedPodRequest.execute()).thenReturn(new V1PodList().items(List.of(pod)));
        when(restTemplate.getForObject(eq("http://10.244.1.2:9091/metrics"), eq(String.class))).thenReturn("""
                # HELP felix_active_local_endpoints Number of endpoints on this host
                felix_active_local_endpoints 42
                # HELP felix_iptables_save_errors_total Number of iptables save errors
                felix_iptables_save_errors_total 3
                """);

        List<CalicoNodeMetrics> metrics = calicoMetricsService.getCalicoNodeMetrics();

        assertEquals(1, metrics.size());
        CalicoNodeMetrics node = metrics.get(0);
        assertEquals("kind-worker", node.nodeName());
        assertEquals("calico-node-abc", node.calicoNodePodName());
        assertEquals("10.244.1.2", node.podIp());
        assertTrue(node.reachable());
        assertEquals(42L, node.activeLocalEndpoints());
        assertEquals(3L, node.iptablesSaveErrorsTotal());
        assertNull(node.errorMessage());
    }

    @Test
    void getCalicoNodeMetrics_parsesLabeledPrometheusSamples() throws Exception {
        V1Pod pod = buildCalicoPod("calico-node-def", "kind-worker2", "10.244.2.3");
        when(coreV1Api.listNamespacedPod(eq("kube-system"))).thenReturn(listNamespacedPodRequest);
        when(listNamespacedPodRequest.labelSelector(eq("k8s-app=calico-node"))).thenReturn(listNamespacedPodRequest);
        when(listNamespacedPodRequest.execute()).thenReturn(new V1PodList().items(List.of(pod)));
        when(restTemplate.getForObject(eq("http://10.244.2.3:9091/metrics"), eq(String.class))).thenReturn("""
                felix_active_local_endpoints{node="kind-worker2"} 7
                felix_iptables_save_errors_total{node="kind-worker2"} 0
                """);

        List<CalicoNodeMetrics> metrics = calicoMetricsService.getCalicoNodeMetrics();

        assertEquals(1, metrics.size());
        CalicoNodeMetrics node = metrics.get(0);
        assertEquals(7L, node.activeLocalEndpoints());
        assertEquals(0L, node.iptablesSaveErrorsTotal());
        assertTrue(node.reachable());
    }

    @Test
    void getCalicoNodeMetrics_marksUnreachableWhenEndpointFails() throws Exception {
        V1Pod pod = buildCalicoPod("calico-node-ghi", "kind-worker3", "10.244.3.4");
        when(coreV1Api.listNamespacedPod(eq("kube-system"))).thenReturn(listNamespacedPodRequest);
        when(listNamespacedPodRequest.labelSelector(eq("k8s-app=calico-node"))).thenReturn(listNamespacedPodRequest);
        when(listNamespacedPodRequest.execute()).thenReturn(new V1PodList().items(List.of(pod)));
        when(restTemplate.getForObject(eq("http://10.244.3.4:9091/metrics"), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        List<CalicoNodeMetrics> metrics = calicoMetricsService.getCalicoNodeMetrics();

        assertEquals(1, metrics.size());
        CalicoNodeMetrics node = metrics.get(0);
        assertFalse(node.reachable());
        assertNull(node.activeLocalEndpoints());
        assertNull(node.iptablesSaveErrorsTotal());
        assertTrue(node.errorMessage().contains("Connection refused"));
    }

    private static V1Pod buildCalicoPod(String podName, String nodeName, String podIp) {
        V1PodStatus status = new V1PodStatus();
        status.setPodIP(podIp);
        return new V1Pod()
                .metadata(new V1ObjectMeta().name(podName).namespace("kube-system"))
                .spec(new V1PodSpec().nodeName(nodeName))
                .status(status);
    }
}
