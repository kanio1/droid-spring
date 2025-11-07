#!/bin/bash
set -e

NAMESPACE="bss"
NEW_BACKEND_IMAGE="${1}"
NEW_FRONTEND_IMAGE="${2}"

log_info() {
    echo -e "\033[0;32m[INFO]\033[0m $1"
}

log_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

if [ -z "$NEW_BACKEND_IMAGE" ] || [ -z "$NEW_FRONTEND_IMAGE" ]; then
    log_error "Usage: $0 <backend-image> <frontend-image>"
    log_error "Example: $0 bss/backend:v1.1.0 bss/frontend:v1.1.0"
    exit 1
fi

log_info "Starting upgrade to:"
log_info "  Backend: $NEW_BACKEND_IMAGE"
log_info "  Frontend: $NEW_FRONTEND_IMAGE"

# Update backend image
log_info "Updating backend image..."
kubectl set image deployment/bss-backend backend=$NEW_BACKEND_IMAGE -n $NAMESPACE

# Update frontend image
log_info "Updating frontend image..."
kubectl set image deployment/bss-frontend frontend=$NEW_FRONTEND_IMAGE -n $NAMESPACE

# Wait for deployments to be available
log_info "Waiting for backend deployment..."
kubectl rollout status deployment/bss-backend -n $NAMESPACE --timeout=600s

log_info "Waiting for frontend deployment..."
kubectl rollout status deployment/bss-frontend -n $NAMESPACE --timeout=600s

# Check status
log_info "Current deployment status:"
kubectl get deployments -n $NAMESPACE

log_info "Upgrade completed successfully!"
