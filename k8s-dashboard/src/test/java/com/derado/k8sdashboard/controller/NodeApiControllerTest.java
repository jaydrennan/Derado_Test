package com.derado.k8sdashboard.controller;

import com.derado.k8sdashboard.controller.api.NodeApiController;
import com.derado.k8sdashboard.dto.NodeInfo;
import com.derado.k8sdashboard.service.NodeService;
import io.kubernetes.client.openapi.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NodeApiController.class)
class NodeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NodeService nodeService;

    @MockBean
    private ApiClient apiClient;

    @Test
    void getNodes_returnsJsonArray() throws Exception {
        NodeInfo node = new NodeInfo(
                "test-node", "Ready", "v1.29.0", "Ubuntu", "amd64",
                "containerd://1.7", Map.of("cpu", "4"), Map.of("cpu", "3800m"),
                Map.of(), "", List.of()
        );
        when(nodeService.getAllNodes()).thenReturn(List.of(node));

        mockMvc.perform(get("/api/v1/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("test-node"))
                .andExpect(jsonPath("$[0].status").value("Ready"));
    }
}
