package com.derado.k8sdashboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import io.kubernetes.client.openapi.ApiClient;

@SpringBootTest
@ActiveProfiles("test")
class K8sDashboardApplicationTests {

    @MockBean
    private ApiClient apiClient;

    @Test
    void contextLoads() {
    }
}
