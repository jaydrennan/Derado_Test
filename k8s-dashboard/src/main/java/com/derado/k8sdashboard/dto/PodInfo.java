package com.derado.k8sdashboard.dto;

import java.util.List;

public record PodInfo(
        String name,
        String namespace,
        String status,
        String nodeName,
        String podIP,
        int restartCount,
        List<ContainerInfo> containers,
        String creationTimestamp,
        String cpuRequest,
        String cpuLimit,
        String memoryRequest,
        String memoryLimit
) {
    public record ContainerInfo(
            String name,
            String image,
            boolean ready,
            String state
    ) {}
}
