package com.derado.k8sdashboard.service;

import com.derado.k8sdashboard.dto.ClusterSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClusterSummaryTest {

    @Test
    void clusterSummary_allFields() {
        ClusterSummary summary = new ClusterSummary(3, 3, 25, 20, 8, 7, 5);

        assertEquals(3, summary.totalNodes());
        assertEquals(3, summary.readyNodes());
        assertEquals(25, summary.totalPods());
        assertEquals(20, summary.runningPods());
        assertEquals(8, summary.totalDeployments());
        assertEquals(7, summary.availableDeployments());
        assertEquals(5, summary.totalNamespaces());
    }
}
