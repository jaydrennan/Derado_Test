package com.derado.k8sdashboard.dto;

public record ClusterSummary(
        int totalNodes,
        int readyNodes,
        int totalPods,
        int runningPods,
        int totalDeployments,
        int availableDeployments,
        int totalNamespaces
) {}
