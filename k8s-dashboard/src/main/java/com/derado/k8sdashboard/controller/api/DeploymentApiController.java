package com.derado.k8sdashboard.controller.api;

import com.derado.k8sdashboard.dto.DeploymentInfo;
import com.derado.k8sdashboard.service.DeploymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DeploymentApiController {

    private final DeploymentService deploymentService;

    public DeploymentApiController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping("/deployments")
    public List<DeploymentInfo> getDeployments(@RequestParam(required = false) String namespace) {
        return deploymentService.getDeployments(namespace);
    }
}
