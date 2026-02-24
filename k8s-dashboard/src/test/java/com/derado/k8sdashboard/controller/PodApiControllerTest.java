package com.derado.k8sdashboard.controller;

import com.derado.k8sdashboard.controller.api.PodApiController;
import com.derado.k8sdashboard.dto.PodInfo;
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

@WebMvcTest(PodApiController.class)
class PodApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PodService podService;

    @MockBean
    private ApiClient apiClient;

    @Test
    void getPods_returnsJsonArray() throws Exception {
        PodInfo pod = new PodInfo(
                "test-pod", "default", "Running", "node-1", "10.0.0.1",
                0, List.of(), "", "100m", "500m", "128Mi", "512Mi"
        );
        when(podService.getPods(null)).thenReturn(List.of(pod));

        mockMvc.perform(get("/api/v1/pods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("test-pod"))
                .andExpect(jsonPath("$[0].status").value("Running"));
    }

    @Test
    void getPods_withNamespaceFilter() throws Exception {
        PodInfo pod = new PodInfo(
                "kube-pod", "kube-system", "Running", "node-1", "10.0.0.2",
                0, List.of(), "", "50m", "200m", "64Mi", "256Mi"
        );
        when(podService.getPods("kube-system")).thenReturn(List.of(pod));

        mockMvc.perform(get("/api/v1/pods").param("namespace", "kube-system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].namespace").value("kube-system"));
    }
}
