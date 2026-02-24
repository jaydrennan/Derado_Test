package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.EventInfo;
import com.derado.k8sdashboard.exception.KubernetesApiException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final CoreV1Api coreV1Api;

    public EventService(ApiClient apiClient) {
        this.coreV1Api = new CoreV1Api(apiClient);
    }

    public List<EventInfo> getEvents(String namespace, int limit) {
        try {
            CoreV1EventList eventList;
            if (namespace != null && !namespace.isBlank()) {
                eventList = coreV1Api.listNamespacedEvent(namespace).execute();
            } else {
                eventList = coreV1Api.listEventForAllNamespaces().execute();
            }
            return eventList.getItems().stream()
                    .sorted(Comparator.comparing(
                            (CoreV1Event e) -> e.getMetadata() != null && e.getMetadata().getCreationTimestamp() != null
                                    ? e.getMetadata().getCreationTimestamp().toEpochSecond()
                                    : 0L
                    ).reversed())
                    .limit(limit)
                    .map(this::mapToEventInfo)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new KubernetesApiException("Failed to list events", e);
        }
    }

    private EventInfo mapToEventInfo(CoreV1Event event) {
        var involvedObj = event.getInvolvedObject();
        String objRef = involvedObj != null
                ? String.format("%s/%s", involvedObj.getKind(), involvedObj.getName())
                : "";

        return new EventInfo(
                event.getType() != null ? event.getType() : "Normal",
                event.getReason() != null ? event.getReason() : "",
                event.getMessage() != null ? event.getMessage() : "",
                objRef,
                event.getMetadata() != null ? event.getMetadata().getNamespace() : "",
                event.getFirstTimestamp() != null ? event.getFirstTimestamp().toString() : "",
                event.getLastTimestamp() != null ? event.getLastTimestamp().toString() : "",
                event.getCount() != null ? event.getCount() : 1
        );
    }
}
