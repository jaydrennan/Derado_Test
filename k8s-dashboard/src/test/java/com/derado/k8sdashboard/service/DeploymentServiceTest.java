package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.DeploymentInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentServiceTest {

    @Test
    void deploymentInfo_recordFields() {
        DeploymentInfo deploy = new DeploymentInfo(
                "nginx-deploy", "default", 3, 3, 3,
                "RollingUpdate", "2024-01-01T00:00:00Z"
        );

        assertEquals("nginx-deploy", deploy.name());
        assertEquals(3, deploy.desiredReplicas());
        assertEquals(3, deploy.readyReplicas());
        assertEquals(3, deploy.availableReplicas());
        assertEquals("RollingUpdate", deploy.strategy());
    }

    @Test
    void deploymentInfo_partiallyAvailable() {
        DeploymentInfo deploy = new DeploymentInfo(
                "failing", "prod", 5, 2, 2, "RollingUpdate", ""
        );

        assertTrue(deploy.readyReplicas() < deploy.desiredReplicas());
    }
}
