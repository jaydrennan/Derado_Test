package com.derado.k8sdashboard.controller;

import com.derado.k8sdashboard.controller.api.CalicoApiController;
import com.derado.k8sdashboard.dto.CalicoNodeMetrics;
import com.derado.k8sdashboard.service.CalicoMetricsService;
import io.kubernetes.client.openapi.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalicoApiController.class)
class CalicoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalicoMetricsService calicoMetricsService;

    @MockBean
    private ApiClient apiClient;

    @Test
    void getCalicoMetrics_returnsJsonArray() throws Exception {
        CalicoNodeMetrics metrics = new CalicoNodeMetrics(
                "kind-worker",
                "calico-node-xyz",
                "10.244.1.2",
                31L,
                0L,
                true,
                null
        );
        when(calicoMetricsService.getCalicoNodeMetrics()).thenReturn(List.of(metrics));

        mockMvc.perform(get("/api/v1/network/calico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nodeName").value("kind-worker"))
                .andExpect(jsonPath("$[0].calicoNodePodName").value("calico-node-xyz"))
                .andExpect(jsonPath("$[0].activeLocalEndpoints").value(31))
                .andExpect(jsonPath("$[0].iptablesSaveErrorsTotal").value(0))
                .andExpect(jsonPath("$[0].reachable").value(true));
    }
}
