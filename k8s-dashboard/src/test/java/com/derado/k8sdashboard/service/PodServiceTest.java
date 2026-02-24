package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.PodInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PodServiceTest {

    @Test
    void podInfo_recordFields() {
        PodInfo.ContainerInfo container = new PodInfo.ContainerInfo(
                "nginx", "nginx:latest", true, "Running"
        );
        PodInfo pod = new PodInfo(
                "nginx-abc123", "default", "Running", "node-1",
                "10.244.1.5", 0, List.of(container), "2024-01-01T00:00:00Z",
                "100m", "500m", "128Mi", "512Mi"
        );

        assertEquals("nginx-abc123", pod.name());
        assertEquals("default", pod.namespace());
        assertEquals("Running", pod.status());
        assertEquals(1, pod.containers().size());
        assertEquals("nginx", pod.containers().get(0).name());
        assertTrue(pod.containers().get(0).ready());
    }

    @Test
    void podInfo_withMultipleContainers() {
        List<PodInfo.ContainerInfo> containers = List.of(
                new PodInfo.ContainerInfo("app", "app:v1", true, "Running"),
                new PodInfo.ContainerInfo("sidecar", "envoy:latest", true, "Running")
        );
        PodInfo pod = new PodInfo("multi-pod", "prod", "Running", "node-2",
                "10.244.2.10", 3, containers, "", "200m", "1", "256Mi", "1Gi");

        assertEquals(2, pod.containers().size());
        assertEquals(3, pod.restartCount());
    }

    @Test
    void podInfo_resourceFields() {
        PodInfo pod = new PodInfo(
                "resource-pod", "default", "Running", "node-1", "10.0.0.1",
                0, List.of(), "", "250m", "500m", "128Mi", "256Mi"
        );

        assertEquals("250m", pod.cpuRequest());
        assertEquals("500m", pod.cpuLimit());
        assertEquals("128Mi", pod.memoryRequest());
        assertEquals("256Mi", pod.memoryLimit());
    }

    @Test
    void podInfo_noResources() {
        PodInfo pod = new PodInfo(
                "no-resource-pod", "default", "Running", "node-1", "10.0.0.1",
                0, List.of(), "", "-", "-", "-", "-"
        );

        assertEquals("-", pod.cpuRequest());
        assertEquals("-", pod.cpuLimit());
    }
}
