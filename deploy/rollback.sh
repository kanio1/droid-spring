#!/bin/bash
set -e

NAMESPACE="bss"
BACKEND_IMAGE="${1:-bss/backend:latest}"

log_info() {
    echo -e "\033[0;32m[INFO]\033[0m $1"
}

log_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

log_info "Starting rollback for BSS application"

# Rollback backend
log_info "Rolling back backend deployment..."
kubectl rollout undo deployment/bss-backend -n $NAMESPACE

# Wait for rollback to complete
log_info "Waiting for rollback to complete..."
kubectl rollout status deployment/bss-backend -n $NAMESPACE --timeout=300s

# Rollback frontend
log_info "Rolling back frontend deployment..."
kubectl rollout undo deployment/bss-frontend -n $NAMESPACE

# Wait for rollback to complete
log_info "Waiting for rollback to complete..."
kubectl rollout status deployment/bss-frontend -n $NAMESPACE --timeout=300s

# Check status
log_info "Current deployment status:"
kubectl get deployments -n $NAMESPACE

log_info "Rollback completed successfully!"
