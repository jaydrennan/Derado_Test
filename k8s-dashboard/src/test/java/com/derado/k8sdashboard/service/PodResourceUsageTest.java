package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.PodResourceUsage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PodResourceUsageTest {

    @Test
    void podResourceUsage_recordFields() {
        PodResourceUsage.ContainerResourceUsage container = new PodResourceUsage.ContainerResourceUsage(
                "nginx", "50m", "128Mi"
        );
        PodResourceUsage pod = new PodResourceUsage(
                "nginx-abc123", "default", "50m", "128Mi", List.of(container)
        );

        assertEquals("nginx-abc123", pod.name());
        assertEquals("default", pod.namespace());
        assertEquals("50m", pod.cpuUsage());
        assertEquals("128Mi", pod.memoryUsage());
        assertEquals(1, pod.containers().size());
        assertEquals("nginx", pod.containers().get(0).name());
    }

    @Test
    void podResourceUsage_multipleContainers() {
        List<PodResourceUsage.ContainerResourceUsage> containers = List.of(
                new PodResourceUsage.ContainerResourceUsage("app", "100m", "256Mi"),
                new PodResourceUsage.ContainerResourceUsage("sidecar", "20m", "64Mi")
        );
        PodResourceUsage pod = new PodResourceUsage(
                "multi-pod", "prod", "120m", "320Mi", containers
        );

        assertEquals(2, pod.containers().size());
        assertEquals("120m", pod.cpuUsage());
        assertEquals("320Mi", pod.memoryUsage());
    }

    @Test
    void podResourceUsage_emptyContainers() {
        PodResourceUsage pod = new PodResourceUsage(
                "empty-pod", "default", "0", "0", List.of()
        );

        assertEquals(0, pod.containers().size());
        assertEquals("0", pod.cpuUsage());
    }

    @Test
    void podResourceUsage_recordEquality() {
        var c1 = new PodResourceUsage.ContainerResourceUsage("app", "10m", "32Mi");
        var c2 = new PodResourceUsage.ContainerResourceUsage("app", "10m", "32Mi");
        PodResourceUsage p1 = new PodResourceUsage("pod", "ns", "10m", "32Mi", List.of(c1));
        PodResourceUsage p2 = new PodResourceUsage("pod", "ns", "10m", "32Mi", List.of(c2));
        assertEquals(p1, p2);
    }
}
