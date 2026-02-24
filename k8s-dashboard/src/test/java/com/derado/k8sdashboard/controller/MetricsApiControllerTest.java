package com.derado.k8sdashboard.controller;

import com.derado.k8sdashboard.controller.api.MetricsApiController;
import com.derado.k8sdashboard.dto.PodResourceUsage;
import com.derado.k8sdashboard.dto.ResourceUsage;
import com.derado.k8sdashboard.service.DeploymentService;
import com.derado.k8sdashboard.service.MetricsService;
import com.derado.k8sdashboard.service.NodeService;
import com.derado.k8sdashboard.service.PodService;
import io.kubernetes.client.openapi.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricsApiController.class)
class MetricsApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    private NodeService nodeService;

    @MockBean
    private PodService podService;

    @MockBean
    private DeploymentService deploymentService;

    @MockBean
    private ApiClient apiClient;

    @Test
    void getNodeMetrics_returnsJsonArray() throws Exception {
        ResourceUsage usage = new ResourceUsage(
                "test-node", "250m", "512Mi", "4", "8Gi", 6.3, 6.1
        );
        when(metricsService.getNodeMetrics()).thenReturn(List.of(usage));

        mockMvc.perform(get("/api/v1/metrics/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("test-node"))
                .andExpect(jsonPath("$[0].cpuUsage").value("250m"))
                .andExpect(jsonPath("$[0].cpuPercent").value(6.3));
    }

    @Test
    void getPodMetrics_returnsJsonArray() throws Exception {
        PodResourceUsage pod = new PodResourceUsage(
                "nginx-pod", "default", "100m", "256Mi",
                List.of(new PodResourceUsage.ContainerResourceUsage("nginx", "100m", "256Mi"))
        );
        when(metricsService.getPodMetrics(null)).thenReturn(List.of(pod));

        mockMvc.perform(get("/api/v1/metrics/pods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("nginx-pod"))
                .andExpect(jsonPath("$[0].namespace").value("default"))
                .andExpect(jsonPath("$[0].cpuUsage").value("100m"))
                .andExpect(jsonPath("$[0].containers[0].name").value("nginx"));
    }

    @Test
    void getPodMetrics_withNamespaceFilter() throws Exception {
        PodResourceUsage pod = new PodResourceUsage(
                "kube-pod", "kube-system", "50m", "128Mi", List.of()
        );
        when(metricsService.getPodMetrics("kube-system")).thenReturn(List.of(pod));

        mockMvc.perform(get("/api/v1/metrics/pods").param("namespace", "kube-system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].namespace").value("kube-system"));
    }
}
