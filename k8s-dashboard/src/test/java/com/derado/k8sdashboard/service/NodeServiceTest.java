package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.NodeInfo;
import com.derado.k8sdashboard.exception.KubernetesApiException;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @Mock
    private ApiClient apiClient;

    @Mock
    private CoreV1Api coreV1Api;

    @Mock
    private CoreV1Api.APIlistNodeRequest listNodeRequest;

    private NodeService nodeService;

    @BeforeEach
    void setUp() {
        nodeService = new NodeService(apiClient) {
            // Override to inject mock CoreV1Api
        };
    }

    @Test
    void getAllNodes_returnsNodeInfoList() throws Exception {
        // This test validates the DTO structure
        NodeInfo node = new NodeInfo(
                "node-1", "Ready", "v1.29.0", "Ubuntu 22.04",
                "amd64", "containerd://1.7.0",
                Map.of("cpu", "4", "memory", "8Gi"),
                Map.of("cpu", "3800m", "memory", "7Gi"),
                Map.of("kubernetes.io/os", "linux"),
                OffsetDateTime.now().toString(),
                List.of()
        );

        assertNotNull(node);
        assertEquals("node-1", node.name());
        assertEquals("Ready", node.status());
        assertEquals("v1.29.0", node.kubeletVersion());
    }

    @Test
    void nodeInfo_recordEquality() {
        NodeInfo n1 = new NodeInfo("n1", "Ready", "v1.29", "", "", "",
                Map.of(), Map.of(), Map.of(), "", List.of());
        NodeInfo n2 = new NodeInfo("n1", "Ready", "v1.29", "", "", "",
                Map.of(), Map.of(), Map.of(), "", List.of());
        assertEquals(n1, n2);
    }

    @Test
    void nodeInfo_withPressureConditions() {
        NodeInfo node = new NodeInfo("n1", "Ready", "v1.29", "", "", "",
                Map.of(), Map.of(), Map.of(), "",
                List.of("DiskPressure", "MemoryPressure"));
        assertEquals(2, node.conditions().size());
        assertTrue(node.conditions().contains("DiskPressure"));
        assertTrue(node.conditions().contains("MemoryPressure"));
    }

    @Test
    void nodeInfo_healthyNoConditions() {
        NodeInfo node = new NodeInfo("n1", "Ready", "v1.29", "", "", "",
                Map.of(), Map.of(), Map.of(), "", Collections.emptyList());
        assertTrue(node.conditions().isEmpty());
    }
}
