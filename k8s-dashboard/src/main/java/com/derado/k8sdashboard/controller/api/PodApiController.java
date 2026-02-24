package com.derado.k8sdashboard.controller.api;

import com.derado.k8sdashboard.dto.PodInfo;
import com.derado.k8sdashboard.service.PodService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PodApiController {

    private final PodService podService;

    public PodApiController(PodService podService) {
        this.podService = podService;
    }

    @GetMapping("/pods")
    public List<PodInfo> getPods(@RequestParam(required = false) String namespace) {
        return podService.getPods(namespace);
    }
}
