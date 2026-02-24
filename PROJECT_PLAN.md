# Kubernetes Monitoring Dashboard - Project Plan

## Overview

A full-stack Java Spring Boot dashboard for monitoring Kubernetes clusters, deployed locally on KIND.

## Implementation Status

### Phase 1: Infrastructure Setup
- [x] Update `kind-config.yaml` with port mappings for NodePort 30080
- [x] Update `Makefile` with build/docker/deploy/test/clean targets

### Phase 2: Maven Project Setup
- [x] Create `pom.xml` with Spring Boot 3.2.x, K8s client, Thymeleaf, Bootstrap WebJar, Actuator
- [x] Create multi-stage `Dockerfile` (Maven build -> JRE Alpine runtime, non-root user)
- [x] Add Maven wrapper for reproducible builds

### Phase 3: Application Code
- [x] `K8sDashboardApplication.java` - Spring Boot entry point
- [x] `KubernetesClientConfig.java` - Dual-profile ApiClient (incluster vs kubeconfig)
- [x] DTO records: `NodeInfo`, `PodInfo`, `DeploymentInfo`, `EventInfo`, `ResourceUsage`, `ClusterSummary`
- [x] Service layer: `NodeService`, `PodService`, `DeploymentService`, `EventService`, `MetricsService`
- [x] REST controllers: Nodes, Pods, Deployments, Events, Metrics/Summary
- [x] `DashboardController` - Thymeleaf view controller
- [x] `GlobalExceptionHandler` with JSON error responses
- [x] `KubernetesApiException` custom exception

### Phase 4: Frontend UI
- [x] Thymeleaf `index.html` with tabbed layout (Nodes, Pods, Deployments, Events, Metrics)
- [x] `dashboard.css` - Dark theme with professional ops dashboard styling
- [x] `dashboard.js` - Auto-refresh every 10s, namespace filtering, status badges

### Phase 5: Kubernetes Manifests
- [x] `namespace.yaml`
- [x] `serviceaccount.yaml`
- [x] `clusterrole.yaml` - Read-only RBAC (least privilege)
- [x] `clusterrolebinding.yaml`
- [x] `deployment.yaml` - with `imagePullPolicy: Never` for KIND
- [x] `service.yaml` - NodePort 30080

### Phase 6: Testing
- [x] Unit tests for DTO records
- [x] `@WebMvcTest` controller tests
- [x] Application context load test

### Phase 7: Documentation
- [x] `README.md` with setup, API docs, architecture diagram
- [x] `PROJECT_PLAN.md` (this file)
- [x] `PROJECT_WALKTHROUGH.md` - comprehensive interview prep walkthrough

### Phase 8: Metrics Enhancements
- [x] `PodResourceUsage` DTO — per-pod CPU/memory usage with container-level breakdown
- [x] `MetricsService.getPodMetrics()` — fetch pod-level metrics from metrics-server
- [x] `GET /api/v1/metrics/pods?namespace=` — new REST endpoint for pod resource usage
- [x] Pod resource requests/limits (`cpuRequest`, `cpuLimit`, `memoryRequest`, `memoryLimit`) added to `PodInfo`
- [x] `PodService.sumQuantity()` — aggregates container resource requests/limits
- [x] Node health conditions (`DiskPressure`, `MemoryPressure`, `PIDPressure`) added to `NodeInfo`
- [x] Metrics tab: "Node Resource Usage" and "Pod Resource Usage" tables with namespace filtering
- [x] Pods tab: CPU Req/Lim and Mem Req/Lim columns
- [x] Nodes tab: Conditions column with Healthy/pressure badges
- [x] New tests: `PodResourceUsageTest`, `MetricsApiControllerTest`
- [x] Updated tests: `PodServiceTest`, `PodApiControllerTest`, `NodeServiceTest`, `NodeApiControllerTest`

## REST API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/cluster/summary` | Cluster summary |
| GET | `/api/v1/nodes` | All nodes |
| GET | `/api/v1/pods?namespace=` | Pods with optional filter |
| GET | `/api/v1/deployments?namespace=` | Deployments with optional filter |
| GET | `/api/v1/events?namespace=&limit=` | Recent events |
| GET | `/api/v1/metrics/nodes` | Node resource usage |
| GET | `/api/v1/metrics/pods?namespace=` | Pod resource usage |
| GET | `/` | Web dashboard |

## Verification Steps

1. `make all` - creates cluster, builds, deploys
2. `kubectl get nodes` - 3 nodes Ready
3. `kubectl get pods -n k8s-dashboard` - dashboard pod Running
4. `curl http://localhost:30080/api/v1/nodes` - returns JSON
5. Open `http://localhost:30080` - dashboard renders
6. Deploy test workload, verify it appears in dashboard
