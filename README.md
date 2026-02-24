# Kubernetes Monitoring Dashboard

A full-stack Java Spring Boot dashboard that monitors a Kubernetes cluster - nodes, pods, deployments, events, and resource usage - with a REST API and a dark-themed web UI.

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.x, Kubernetes Java Client
- **Frontend:** Thymeleaf, Bootstrap 5 (dark theme), JavaScript (auto-refresh every 10s)
- **Infrastructure:** Docker (multi-stage build), KIND (Kubernetes in Docker)

## Prerequisites

- Docker
- [KIND](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) (Kubernetes in Docker)
- kubectl
- Java 17+ (for local development only)

## Quick Start

Deploy everything with a single command:

```bash
make all
```

This will:
1. Create a KIND cluster (1 control-plane + 2 workers)
2. Install metrics-server
3. Build the Docker image
4. Load it into the KIND cluster
5. Deploy the dashboard with RBAC
6. Wait for the pod to become ready

Then open: **http://localhost:30080**

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/cluster/summary` | Cluster summary (node/pod/deployment counts) |
| GET | `/api/v1/nodes` | All nodes with status and capacity |
| GET | `/api/v1/pods?namespace=` | All pods, optional namespace filter |
| GET | `/api/v1/deployments?namespace=` | All deployments |
| GET | `/api/v1/events?namespace=&limit=` | Recent events (default 50) |
| GET | `/api/v1/metrics/nodes` | Node resource usage |
| GET | `/` | Web dashboard |

## Makefile Targets

| Target | Description |
|--------|-------------|
| `make cluster-up` | Create KIND cluster |
| `make cluster-down` | Delete KIND cluster |
| `make build` | Build the Maven project |
| `make test` | Run unit tests |
| `make docker-build` | Build Docker image |
| `make kind-load` | Load image into KIND |
| `make deploy` | Apply K8s manifests |
| `make undeploy` | Remove K8s manifests |
| `make status` | Show cluster and pod status |
| `make logs` | Stream dashboard logs |
| `make install-metrics-server` | Install metrics-server |
| `make all` | Full pipeline (cluster + build + deploy) |
| `make clean` | Undeploy and delete cluster |

## Architecture

```
┌─────────────────────────────────────────────┐
│                 KIND Cluster                 │
│  ┌─────────────────────────────────────┐    │
│  │        k8s-dashboard namespace      │    │
│  │  ┌───────────────────────────────┐  │    │
│  │  │   Spring Boot Dashboard App   │  │    │
│  │  │  ┌─────────┐  ┌───────────┐  │  │    │
│  │  │  │ REST API │  │ Thymeleaf │  │  │    │
│  │  │  └────┬─────┘  └─────┬─────┘  │  │    │
│  │  │       │              │        │  │    │
│  │  │  ┌────┴──────────────┴────┐   │  │    │
│  │  │  │    Service Layer       │   │  │    │
│  │  │  │  (K8s Java Client)     │   │  │    │
│  │  │  └────────────┬───────────┘   │  │    │
│  │  └───────────────┼───────────────┘  │    │
│  └──────────────────┼──────────────────┘    │
│                     │                        │
│            K8s API Server                    │
│         (via ServiceAccount RBAC)            │
└─────────────────────────────────────────────┘
```

## Security

- **RBAC with least privilege:** The ServiceAccount only has `get/list/watch` permissions on nodes, pods, deployments, events, namespaces, and metrics - no write access
- **Non-root container:** The Docker image runs as a non-root user
- **In-cluster authentication:** Uses the ServiceAccount token mounted by Kubernetes

## Testing

### Verify the deployment

```bash
# Check cluster
kubectl get nodes

# Check dashboard pod
kubectl get pods -n k8s-dashboard

# Test API
curl http://localhost:30080/api/v1/nodes
curl http://localhost:30080/api/v1/cluster/summary

# Deploy a test workload and see it appear
kubectl create deployment nginx --image=nginx --replicas=3
```

### Run unit tests

```bash
make test
```

## Local Development

To run the dashboard locally against your kubeconfig:

```bash
cd k8s-dashboard
./mvnw spring-boot:run
```

The app will use your `~/.kube/config` to connect to whatever cluster is configured.
