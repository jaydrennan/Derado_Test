package com.derado.k8sdashboard.controller.api;

import com.derado.k8sdashboard.dto.ClusterSummary;
import com.derado.k8sdashboard.dto.PodResourceUsage;
import com.derado.k8sdashboard.dto.ResourceUsage;
import com.derado.k8sdashboard.service.DeploymentService;
import com.derado.k8sdashboard.service.MetricsService;
import com.derado.k8sdashboard.service.NodeService;
import com.derado.k8sdashboard.service.PodService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MetricsApiController {

    private final MetricsService metricsService;
    private final NodeService nodeService;
    private final PodService podService;
    private final DeploymentService deploymentService;

    public MetricsApiController(MetricsService metricsService, NodeService nodeService,
                                 PodService podService, DeploymentService deploymentService) {
        this.metricsService = metricsService;
        this.nodeService = nodeService;
        this.podService = podService;
        this.deploymentService = deploymentService;
    }

    @GetMapping("/metrics/nodes")
    public List<ResourceUsage> getNodeMetrics() {
        return metricsService.getNodeMetrics();
    }

    @GetMapping("/metrics/pods")
    public List<PodResourceUsage> getPodMetrics(@RequestParam(required = false) String namespace) {
        return metricsService.getPodMetrics(namespace);
    }

    @GetMapping("/cluster/summary")
    public ClusterSummary getClusterSummary() {
        var nodes = nodeService.getAllNodes();
        var pods = podService.getPods(null);
        var deployments = deploymentService.getDeployments(null);

        int readyNodes = (int) nodes.stream().filter(n -> "Ready".equals(n.status())).count();
        int runningPods = (int) pods.stream().filter(p -> "Running".equals(p.status())).count();
        int availableDeploys = (int) deployments.stream()
                .filter(d -> d.availableReplicas() == d.desiredReplicas() && d.desiredReplicas() > 0)
                .count();

        long namespaceCount = pods.stream()
                .map(p -> p.namespace())
                .distinct()
                .count();

        return new ClusterSummary(
                nodes.size(), readyNodes,
                pods.size(), runningPods,
                deployments.size(), availableDeploys,
                (int) namespaceCount
        );
    }
}
