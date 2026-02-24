package com.derado.k8sdashboard.controller.api;

import com.derado.k8sdashboard.dto.EventInfo;
import com.derado.k8sdashboard.service.EventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class EventApiController {

    private final EventService eventService;

    public EventApiController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events")
    public List<EventInfo> getEvents(
            @RequestParam(required = false) String namespace,
            @RequestParam(defaultValue = "50") int limit) {
        return eventService.getEvents(namespace, limit);
    }
}
