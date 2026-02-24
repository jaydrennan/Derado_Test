package com.derado.k8sdashboard.controller.api;

import com.derado.k8sdashboard.dto.NodeInfo;
import com.derado.k8sdashboard.service.NodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class NodeApiController {

    private final NodeService nodeService;

    public NodeApiController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping("/nodes")
    public List<NodeInfo> getNodes() {
        return nodeService.getAllNodes();
    }
}
