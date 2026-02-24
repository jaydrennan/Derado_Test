package com.derado.k8sdashboard.dto;

public record DeploymentInfo(
        String name,
        String namespace,
        int desiredReplicas,
        int readyReplicas,
        int availableReplicas,
        String strategy,
        String creationTimestamp
) {}
