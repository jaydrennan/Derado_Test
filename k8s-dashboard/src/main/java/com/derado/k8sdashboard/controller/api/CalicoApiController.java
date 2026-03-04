package com.derado.k8sdashboard.controller.api;

import com.derado.k8sdashboard.dto.CalicoNodeMetrics;
import com.derado.k8sdashboard.service.CalicoMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/network")
public class CalicoApiController {

    private final CalicoMetricsService calicoMetricsService;

    public CalicoApiController(CalicoMetricsService calicoMetricsService) {
        this.calicoMetricsService = calicoMetricsService;
    }

    @GetMapping("/calico")
    public List<CalicoNodeMetrics> getCalicoMetrics() {
        return calicoMetricsService.getCalicoNodeMetrics();
    }
}
