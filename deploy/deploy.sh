#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="bss"
KUBECTL_CONTEXT="${KUBECTL_CONTEXT:-}"

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        log_error "docker is not installed"
        exit 1
    fi

    if ! command -v helm &> /dev/null; then
        log_warn "helm is not installed (optional)"
    fi

    if [ -z "$KUBECTL_CONTEXT" ]; then
        log_warn "KUBECTL_CONTEXT not set, using current context"
    fi

    log_info "Prerequisites check passed"
}

build_and_push_images() {
    log_info "Building and pushing Docker images..."

    # Build backend
    log_info "Building backend image..."
    docker build -t bss/backend:latest ./backend

    # Build frontend
    log_info "Building frontend image..."
    docker build -t bss/frontend:latest ./frontend

    # Push to registry
    if [ -n "$REGISTRY" ]; then
        log_info "Pushing images to registry: $REGISTRY"
        docker tag bss/backend:latest "${REGISTRY}/bss/backend:latest"
        docker tag bss/frontend:latest "${REGISTRY}/bss/frontend:latest"
        docker push "${REGISTRY}/bss/backend:latest"
        docker push "${REGISTRY}/bss/frontend:latest"
    fi

    log_info "Images built successfully"
}

create_namespace() {
    log_info "Creating namespace: $NAMESPACE"
    kubectl apply -f k8s/namespace.yaml
}

apply_manifests() {
    log_info "Applying Kubernetes manifests..."

    # Apply ConfigMaps
    log_info "Applying ConfigMaps..."
    kubectl apply -f k8s/configmaps/bss-config.yaml

    # Apply Secrets (only if template file doesn't exist)
    if [ ! -f "k8s/secrets/bss-secrets.yaml" ]; then
        log_warn "Secrets file not found. Copying template..."
        cp k8s/secrets/secrets-template.yaml k8s/secrets/bss-secrets.yaml
        log_warn "Please edit k8s/secrets/bss-secrets.yaml with your values before continuing"
        log_warn "Run this script again after updating secrets"
        exit 1
    fi

    log_info "Applying secrets..."
    kubectl apply -f k8s/secrets/bss-secrets.yaml

    # Apply infrastructure
    log_info "Applying PostgreSQL..."
    kubectl apply -f k8s/postgres/statefulset.yaml

    log_info "Applying Redis..."
    kubectl apply -f k8s/redis/deployment.yaml

    log_info "Applying Keycloak..."
    kubectl apply -f k8s/keycloak/deployment.yaml

    # Wait for infrastructure
    log_info "Waiting for infrastructure services..."
    kubectl wait --for=condition=ready pod -l app=postgres -n $NAMESPACE --timeout=300s
    kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=300s
    kubectl wait --for=condition=ready pod -l app=keycloak -n $NAMESPACE --timeout=300s

    # Apply applications
    log_info "Applying backend..."
    kubectl apply -f k8s/backend/

    log_info "Applying frontend..."
    kubectl apply -f k8s/frontend/

    # Apply ingress
    log_info "Applying ingress..."
    kubectl apply -f k8s/ingress.yaml

    log_info "All manifests applied successfully"
}

run_migrations() {
    log_info "Running database migrations..."

    # Create a job to run migrations
    cat <<EOF | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: bss-migrations
  namespace: $NAMESPACE
spec:
  template:
    spec:
      serviceAccountName: bss-backend
      containers:
      - name: migrations
        image: bss/backend:latest
        command: ["./mvnw", "flyway:migrate"]
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: bss-secrets
              key: database-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: bss-secrets
              key: database-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: bss-secrets
              key: database-password
      restartPolicy: OnFailure
EOF

    # Wait for migration job to complete
    kubectl wait --for=condition=complete job/bss-migrations -n $NAMESPACE --timeout=600s

    log_info "Database migrations completed"
}

check_deployment_health() {
    log_info "Checking deployment health..."

    # Wait for backend deployment
    log_info "Waiting for backend deployment..."
    kubectl rollout status deployment/bss-backend -n $NAMESPACE --timeout=600s

    # Wait for frontend deployment
    log_info "Waiting for frontend deployment..."
    kubectl rollout status deployment/bss-frontend -n $NAMESPACE --timeout=600s

    # Get pod status
    log_info "Pod status:"
    kubectl get pods -n $NAMESPACE

    # Get services
    log_info "Services:"
    kubectl get services -n $NAMESPACE

    log_info "Deployment completed successfully!"
}

show_access_info() {
    log_info "Deployment access information:"
    echo ""
    echo "Frontend URL: https://bss.company.com"
    echo "API URL: https://api.bss.company.com"
    echo "Keycloak URL: https://auth.bss.company.com"
    echo "Grafana URL: https://grafana.bss.company.com"
    echo ""
    echo "To port-forward for local access:"
    echo "  kubectl port-forward svc/bss-frontend 3000:3000 -n $NAMESPACE"
    echo "  kubectl port-forward svc/bss-backend-service 8080:8080 -n $NAMESPACE"
    echo ""
    echo "To view logs:"
    echo "  kubectl logs -f deployment/bss-backend -n $NAMESPACE"
    echo "  kubectl logs -f deployment/bss-frontend -n $NAMESPACE"
    echo ""
}

cleanup() {
    log_info "Cleaning up migration job..."
    kubectl delete job bss-migrations -n $NAMESPACE --ignore-not-found=true
}

main() {
    log_info "Starting BSS Production Deployment"
    log_info "Namespace: $NAMESPACE"

    check_prerequisites

    if [ "$1" == "full" ]; then
        build_and_push_images
    fi

    create_namespace
    apply_manifests
    run_migrations
    check_deployment_health
    cleanup
    show_access_info

    log_info "Deployment finished successfully!"
}

main "$@"
