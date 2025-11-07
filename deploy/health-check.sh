#!/bin/bash
set -e

NAMESPACE="bss"
TIMEOUT=300

log_info() {
    echo -e "\033[0;32m[INFO]\033[0m $1"
}

log_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

log_success() {
    echo -e "\033[0;32m[SUCCESS]\033[0m $1"
}

check_namespace() {
    log_info "Checking namespace: $NAMESPACE"
    if kubectl get namespace $NAMESPACE &> /dev/null; then
        log_success "Namespace $NAMESPACE exists"
    else
        log_error "Namespace $NAMESPACE does not exist"
        exit 1
    fi
}

check_pods() {
    log_info "Checking pod status..."

    # Check if all pods are running
    NOT_RUNNING=$(kubectl get pods -n $NAMESPACE --no-headers | grep -v "Running" | wc -l)

    if [ $NOT_RUNNING -eq 0 ]; then
        log_success "All pods are running"
    else
        log_error "Found $NOT_RUNNING pod(s) not in Running state"
        kubectl get pods -n $NAMESPACE
        exit 1
    fi
}

check_services() {
    log_info "Checking services..."

    BACKEND_SVC=$(kubectl get svc bss-backend-service -n $NAMESPACE -o jsonpath='{.spec.ports[0].port}')
    FRONTEND_SVC=$(kubectl get svc bss-frontend -n $NAMESPACE -o jsonpath='{.spec.ports[0].port}')
    POSTGRES_SVC=$(kubectl get svc postgres -n $NAMESPACE -o jsonpath='{.spec.ports[0].port}')
    REDIS_SVC=$(kubectl get svc redis -n $NAMESPACE -o jsonpath='{.spec.ports[0].port}')
    KEYCLOAK_SVC=$(kubectl get svc keycloak -n $NAMESPACE -o jsonpath='{.spec.ports[0].port}')

    log_success "Backend Service: $BACKEND_SVC"
    log_success "Frontend Service: $FRONTEND_SVC"
    log_success "PostgreSQL Service: $POSTGRES_SVC"
    log_success "Redis Service: $REDIS_SVC"
    log_success "Keycloak Service: $KEYCLOAK_SVC"
}

check_endpoints() {
    log_info "Checking health endpoints..."

    # Wait for backend health check
    log_info "Waiting for backend health check..."
    for i in $(seq 1 $TIMEOUT); do
        if kubectl exec -n $NAMESPACE deployment/bss-backend -- curl -sf http://localhost:8080/actuator/health &> /dev/null; then
            log_success "Backend health check passed"
            break
        fi
        if [ $i -eq $TIMEOUT ]; then
            log_error "Backend health check failed after ${TIMEOUT}s"
            exit 1
        fi
        sleep 1
    done

    # Check frontend
    log_info "Checking frontend..."
    for i in $(seq 1 30); do
        if kubectl exec -n $NAMESPACE deployment/bss-frontend -- curl -sf http://localhost:3000/ &> /dev/null; then
            log_success "Frontend is responding"
            break
        fi
        if [ $i -eq 30 ]; then
            log_error "Frontend is not responding"
            exit 1
        fi
        sleep 1
    done
}

check_database() {
    log_info "Checking database connectivity..."

    kubectl exec -n $NAMESPACE deployment/bss-backend -- \
      bash -c "curl -sf postgres:5432 > /dev/null && echo 'Database connection successful' || exit 1"

    log_success "Database is accessible"
}

check_redis() {
    log_info "Checking Redis connectivity..."

    kubectl exec -n $NAMESPACE deployment/bss-backend -- \
      bash -c "redis-cli -h redis ping > /dev/null && echo 'Redis connection successful' || exit 1"

    log_success "Redis is accessible"
}

check_hpa() {
    log_info "Checking HPA status..."

    HPA_STATUS=$(kubectl get hpa bss-backend-hpa -n $NAMESPACE -o jsonpath='{.status.currentMetrics[0].resource.current.averageUtilization}')

    if [ -n "$HPA_STATUS" ]; then
        log_success "HPA is active (CPU: ${HPA_STATUS}%)"
    else
        log_info "HPA is configured (not active yet)"
    fi
}

display_resource_usage() {
    log_info "Resource usage:"
    echo ""
    kubectl top pods -n $NAMESPACE 2>/dev/null || log_info "Metrics server not available"
    echo ""
}

display_access_info() {
    log_info "Access information:"
    echo ""
    echo "Frontend URL: https://bss.company.com"
    echo "API URL: https://api.bss.company.com/actuator/health"
    echo "Keycloak URL: https://auth.bss.company.com"
    echo ""
    echo "Port-forward commands:"
    echo "  kubectl port-forward svc/bss-backend-service 8080:8080 -n $NAMESPACE"
    echo "  kubectl port-forward svc/bss-frontend 3000:3000 -n $NAMESPACE"
    echo "  kubectl port-forward svc/postgres 5432:5432 -n $NAMESPACE"
    echo "  kubectl port-forward svc/redis 6379:6379 -n $NAMESPACE"
    echo ""
}

main() {
    log_info "Starting health check for BSS deployment"
    echo "======================================"
    echo ""

    check_namespace
    check_pods
    check_services
    check_endpoints
    check_database
    check_redis
    check_hpa
    display_resource_usage
    display_access_info

    echo "======================================"
    log_success "All health checks passed!"
}

main "$@"
