package com.derado.k8sdashboard.dto;

import java.util.List;

public record PodResourceUsage(
        String name,
        String namespace,
        String cpuUsage,
        String memoryUsage,
        List<ContainerResourceUsage> containers
) {
    public record ContainerResourceUsage(
            String name,
            String cpuUsage,
            String memoryUsage
    ) {}
}
