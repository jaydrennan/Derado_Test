package com.derado.k8sdashboard.dto;

import java.util.List;
import java.util.Map;

public record NodeInfo(
        String name,
        String status,
        String kubeletVersion,
        String osImage,
        String architecture,
        String containerRuntime,
        Map<String, String> capacity,
        Map<String, String> allocatable,
        Map<String, String> labels,
        String creationTimestamp,
        List<String> conditions
) {}
