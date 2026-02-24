package com.derado.k8sdashboard.exception;

public class KubernetesApiException extends RuntimeException {

    public KubernetesApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
