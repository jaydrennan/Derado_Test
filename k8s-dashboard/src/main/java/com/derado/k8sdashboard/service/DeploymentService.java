package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.DeploymentInfo;
import com.derado.k8sdashboard.exception.KubernetesApiException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeploymentService {

    private final AppsV1Api appsV1Api;

    public DeploymentService(ApiClient apiClient) {
        this.appsV1Api = new AppsV1Api(apiClient);
    }

    public List<DeploymentInfo> getDeployments(String namespace) {
        try {
            V1DeploymentList deploymentList;
            if (namespace != null && !namespace.isBlank()) {
                deploymentList = appsV1Api.listNamespacedDeployment(namespace).execute();
            } else {
                deploymentList = appsV1Api.listDeploymentForAllNamespaces().execute();
            }
            return deploymentList.getItems().stream()
                    .map(this::mapToDeploymentInfo)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new KubernetesApiException("Failed to list deployments", e);
        }
    }

    private DeploymentInfo mapToDeploymentInfo(V1Deployment deployment) {
        var metadata = deployment.getMetadata();
        var spec = deployment.getSpec();
        var status = deployment.getStatus();

        return new DeploymentInfo(
                metadata != null ? metadata.getName() : "unknown",
                metadata != null ? metadata.getNamespace() : "",
                spec != null && spec.getReplicas() != null ? spec.getReplicas() : 0,
                status != null && status.getReadyReplicas() != null ? status.getReadyReplicas() : 0,
                status != null && status.getAvailableReplicas() != null ? status.getAvailableReplicas() : 0,
                spec != null && spec.getStrategy() != null && spec.getStrategy().getType() != null
                        ? spec.getStrategy().getType() : "RollingUpdate",
                metadata != null && metadata.getCreationTimestamp() != null
                        ? metadata.getCreationTimestamp().toString() : ""
        );
    }
}
