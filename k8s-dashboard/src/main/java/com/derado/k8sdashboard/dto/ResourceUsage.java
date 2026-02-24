package com.derado.k8sdashboard.dto;

public record ResourceUsage(
        String name,
        String cpuUsage,
        String memoryUsage,
        String cpuCapacity,
        String memoryCapacity,
        double cpuPercent,
        double memoryPercent
) {}
