CLUSTER_NAME := derado-test
CONFIG := kind-config.yaml
APP_NAME := k8s-dashboard
DOCKER_IMAGE := $(APP_NAME):latest
NAMESPACE := k8s-dashboard

.PHONY: cluster-up cluster-down build test docker-build kind-load deploy undeploy status logs install-metrics-server install-calico all clean

# --- Cluster Management ---
cluster-up:
	kind create cluster --name $(CLUSTER_NAME) --config $(CONFIG)

cluster-down:
	kind delete cluster --name $(CLUSTER_NAME)

# --- Application Build ---
build:
	cd k8s-dashboard && ./mvnw clean package -DskipTests

test:
	cd k8s-dashboard && ./mvnw test

# --- Docker ---
docker-build:
	cd k8s-dashboard && docker build -t $(DOCKER_IMAGE) .

kind-load:
	kind load docker-image $(DOCKER_IMAGE) --name $(CLUSTER_NAME)

# --- Kubernetes Deployment ---
deploy:
	kubectl apply -f k8s-manifests/namespace.yaml
	kubectl apply -f k8s-manifests/serviceaccount.yaml
	kubectl apply -f k8s-manifests/clusterrole.yaml
	kubectl apply -f k8s-manifests/clusterrolebinding.yaml
	kubectl apply -f k8s-manifests/deployment.yaml
	kubectl apply -f k8s-manifests/service.yaml
	@echo "Waiting for deployment to be ready..."
	kubectl rollout status deployment/$(APP_NAME) -n $(NAMESPACE) --timeout=120s

undeploy:
	kubectl delete -f k8s-manifests/ --ignore-not-found

status:
	@echo "=== Nodes ==="
	kubectl get nodes
	@echo ""
	@echo "=== Dashboard Pod ==="
	kubectl get pods -n $(NAMESPACE)
	@echo ""
	@echo "=== Dashboard Service ==="
	kubectl get svc -n $(NAMESPACE)

logs:
	kubectl logs -f deployment/$(APP_NAME) -n $(NAMESPACE)

# --- Metrics Server (optional, for resource usage) ---
install-metrics-server:
	kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
	kubectl patch deployment metrics-server -n kube-system --type='json' \
		-p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}, {"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-preferred-address-types=InternalIP"}]'

# --- Calico CNI (optional, for network/Felix metrics) ---
install-calico:
	kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.28.2/manifests/calico.yaml
	@echo "Waiting for calico-node pods to be ready..."
	kubectl rollout status daemonset/calico-node -n kube-system --timeout=120s
	kubectl patch felixconfiguration default --type merge -p '{"spec":{"prometheusMetricsEnabled": true}}'
	@echo "Calico installed with Felix Prometheus metrics enabled on :9091"

# --- Full Pipeline ---
all: cluster-up install-metrics-server docker-build kind-load deploy status
	@echo ""
	@echo "Dashboard available at: http://localhost:30080"

clean: undeploy cluster-down
