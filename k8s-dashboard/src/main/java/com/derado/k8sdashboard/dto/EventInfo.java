package com.derado.k8sdashboard.dto;

public record EventInfo(
        String type,
        String reason,
        String message,
        String involvedObject,
        String namespace,
        String firstTimestamp,
        String lastTimestamp,
        int count
) {}
