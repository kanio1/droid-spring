# Kubernetes & Helm Charts for BSS

This directory contains Kubernetes manifests and Helm charts to deploy the BSS system on Kubernetes.

## Directory Structure

```
k8s/
├── README.md                  # This file
└── helm/
    ├── bss-backend/          # Spring Boot backend Helm chart
    │   ├── Chart.yaml
    │   ├── values.yaml
    │   └── templates/
    │       ├── deployment.yaml
    │       ├── service.yaml
    │       ├── _helpers.tpl
    │       └── ...
    └── bss-frontend/         # Nuxt 3 frontend Helm chart
        ├── Chart.yaml
        ├── values.yaml
        └── templates/
            ├── deployment.yaml
            ├── service.yaml
            ├── _helpers.tpl
            └── ...
```

## Helm Charts

### bss-backend
Spring Boot application with:
- Health checks (liveness/readiness probes)
- Resource limits and requests
- Configurable environment variables
- Service account support
- Horizontal Pod Autoscaler ready

### bss-frontend
Nuxt 3 application with:
- Health checks
- Resource limits and requests
- Configurable environment variables
- Service account support

## Prerequisites

- Kubernetes cluster (1.20+)
- Helm 3.x
- kubectl configured

## Usage

### Install Backend

```bash
# Add dependencies (if any)
helm dependency update k8s/helm/bss-backend

# Install or upgrade
helm upgrade --install bss-backend k8s/helm/bss-backend \
  --namespace bss \
  --create-namespace \
  --set image.tag=latest
```

### Install Frontend

```bash
helm upgrade --install bss-frontend k8s/helm/bss-frontend \
  --namespace bss \
  --create-namespace \
  --set image.tag=latest
```

### Uninstall

```bash
# Uninstall charts
helm uninstall bss-backend -n bss
helm uninstall bss-frontend -n bss

# Delete namespace (optional)
kubectl delete namespace bss
```

## ArgoCD Integration

These Helm charts are designed to work with ArgoCD deployed in the Docker Compose environment.

### Add Applications in ArgoCD

1. Access ArgoCD: http://localhost:8080
2. Create new application:
   - Application Name: `bss-backend`
   - Project: `default`
   - Source Repository: `https://github.com/your-org/droid-spring`
   - Source Path: `k8s/helm/bss-backend`
   - Destination: `https://kubernetes.default.svc`
   - Namespace: `bss`

### Sync Policy

Set sync policy to `Automated` with:
- Prune Resources: true
- Self Heal: true
- Auto Create Namespace: true

## Migration from Docker Compose to Kubernetes

### Services Mapping

| Docker Compose | Kubernetes |
|----------------|------------|
| bss-backend | bss-backend Deployment |
| bss-frontend | bss-frontend Deployment |
| postgres | PostgreSQL Operator or StatefulSet |
| redis | Redis Operator or StatefulSet |
| kafka | Strimzi Kafka Operator |
| prometheus | Prometheus Operator |
| grafana | Grafana Operator |
| jaeger | Jaeger Operator |
| kong | Kong Ingress Controller |

### Next Steps

1. **Operators**: Install operators for Kafka, PostgreSQL, Redis
2. **ConfigMaps**: Extract configuration to ConfigMaps
3. **Secrets**: Migrate secrets to HashiCorp Vault or SealedSecrets
4. **Ingress**: Configure Kong or NGINX Ingress
5. **Storage**: Configure PV/PVC for persistent data
6. **Monitoring**: Deploy Prometheus Operator
7. **Service Mesh**: Optional - Install Istio or Linkerd

## Helm Values

### Backend

See `helm/bss-backend/values.yaml` for all configuration options.

Common customizations:
- `replicaCount`: Number of replicas
- `image.tag`: Docker image tag
- `resources.limits`: CPU/Memory limits
- `env`: Environment variables

### Frontend

See `helm/bss-frontend/values.yaml` for all configuration options.

## Best Practices

1. **Version Control**: Keep Helm charts in version control
2. **Values Files**: Use different values files per environment
3. **Hooks**: Leverage Helm hooks for pre/post-install tasks
4. **Dependencies**: Use Chart dependencies for shared resources
5. **Testing**: Use `helm template` to validate manifests before install

## References

- [Helm Documentation](https://helm.sh/docs/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [Kubernetes Operators](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/)
