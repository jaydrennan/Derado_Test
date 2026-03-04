package com.derado.k8sdashboard.dto;

public record CalicoNodeMetrics(
        String nodeName,
        String calicoNodePodName,
        String podIp,
        Long activeLocalEndpoints,
        Long iptablesSaveErrorsTotal,
        boolean reachable,
        String errorMessage
) {
}
