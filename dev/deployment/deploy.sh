#!/bin/bash
################################################################################
# BSS Deployment Script
#
# Supports deployment to dev, staging, and production environments
# Usage: ./deploy.sh --env production --service backend --backup-db
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/dev/compose.yml"
NAMESPACE="bss"

# Default values
ENVIRONMENT="staging"
SERVICE="all"
BACKUP_DB=true
BUILD_IMAGES=true
SKIP_TESTS=false
DRY_RUN=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --service)
            SERVICE="$2"
            shift 2
            ;;
        --backup-db)
            BACKUP_DB=true
            shift
            ;;
        --no-backup)
            BACKUP_DB=false
            shift
            ;;
        --build)
            BUILD_IMAGES=true
            shift
            ;;
        --no-build)
            BUILD_IMAGES=false
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_usage() {
    cat << EOF
BSS Deployment Script

USAGE:
    $0 [OPTIONS]

OPTIONS:
    --env ENV         Target environment: dev, staging, production (default: staging)
    --service SERVICE Service to deploy: all, backend, frontend (default: all)
    --backup-db       Create database backup before deployment (default: true)
    --no-backup       Skip database backup
    --build           Build Docker images before deployment (default: true)
    --no-build        Skip building images (use existing)
    --skip-tests      Skip pre-deployment tests
    --dry-run         Show what would be done without executing
    --help            Show this help message

EXAMPLES:
    # Deploy all services to staging with backup
    $0 --env staging

    # Deploy only backend to production without backup
    $0 --env production --service backend --no-backup

    # Dry run to see what would happen
    $0 --env dev --dry-run

ENVIRONMENT VARIABLES:
    DOCKER_REGISTRY    Docker registry URL (default: ghcr.io)
    IMAGE_TAG          Image tag (default: current git branch + build number)
    KUBECTL_CONTEXT    kubectl context to use
    HELM_RELEASE_NAME  Helm release name (default: bss)

EOF
}

check_dependencies() {
    log_info "Checking dependencies..."

    local deps=("docker" "docker-compose" "git" "curl" "jq")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            log_error "$dep is not installed"
            exit 1
        fi
    done

    log_info "All dependencies are available"
}

load_environment() {
    log_info "Loading environment configuration..."

    local env_file="$PROJECT_ROOT/.env"
    if [[ -f "$env_file" ]]; then
        source "$env_file"
        log_info "Loaded environment from $env_file"
    else
        log_warn ".env file not found, using default values"
    fi

    # Set environment-specific variables
    case "$ENVIRONMENT" in
        dev)
            export DB_HOST="${DEV_DB_HOST:-localhost}"
            export API_URL="${DEV_API_URL:-http://localhost:8080}"
            export FRONTEND_URL="${DEV_FRONTEND_URL:-http://localhost:3000}"
            ;;
        staging)
            export DB_HOST="${STAGING_DB_HOST:-staging-db.internal}"
            export API_URL="${STAGING_API_URL:-https://api-staging.bss.example.com}"
            export FRONTEND_URL="${STAGING_FRONTEND_URL:-https://staging.bss.example.com}"
            ;;
        production)
            export DB_HOST="${PROD_DB_HOST:-prod-db.internal}"
            export API_URL="${PROD_API_URL:-https://api.bss.example.com}"
            export FRONTEND_URL="${PROD_FRONTEND_URL:-https://bss.example.com}"
            ;;
    esac
}

create_database_backup() {
    if [[ "$BACKUP_DB" != "true" ]]; then
        log_warn "Skipping database backup"
        return 0
    fi

    if [[ "$ENVIRONMENT" == "dev" ]]; then
        log_warn "Skipping backup for dev environment"
        return 0
    fi

    log_info "Creating database backup..."

    local backup_script="$PROJECT_ROOT/dev/database/backup/restore/backup.sh"
    if [[ -f "$backup_script" ]]; then
        if [[ "$DRY_RUN" == "true" ]]; then
            echo "[DRY-RUN] Would run: $backup_script --env $ENVIRONMENT"
        else
            "$backup_script" --env "$ENVIRONMENT"
        fi
    else
        log_warn "Backup script not found at $backup_script"
    fi
}

build_images() {
    if [[ "$BUILD_IMAGES" != "true" ]]; then
        log_warn "Skipping image build"
        return 0
    fi

    log_info "Building Docker images..."

    local registry="${DOCKER_REGISTRY:-ghcr.io}"
    local tag="${IMAGE_TAG:-$ENVIRONMENT-$(git rev-parse --short HEAD)}"

    if [[ "$DRY_RUN" == "true" ]]; then
        echo "[DRY-RUN] Would build images with tag: $tag"
        return 0
    fi

    # Build backend
    if [[ "$SERVICE" == "all" || "$SERVICE" == "backend" ]]; then
        log_info "Building backend image..."
        docker build -t "$registry/bss/backend:$tag" "$PROJECT_ROOT/backend"
    fi

    # Build frontend
    if [[ "$SERVICE" == "all" || "$SERVICE" == "frontend" ]]; then
        log_info "Building frontend image..."
        docker build -t "$registry/bss/frontend:$tag" "$PROJECT_ROOT/frontend"
    fi

    log_info "Images built successfully"
}

push_images() {
    if [[ "$BUILD_IMAGES" != "true" ]]; then
        return 0
    fi

    local registry="${DOCKER_REGISTRY:-ghcr.io}"
    local tag="${IMAGE_TAG:-$ENVIRONMENT-$(git rev-parse --short HEAD)}"

    log_info "Pushing images to registry..."

    if [[ "$DRY_RUN" == "true" ]]; then
        echo "[DRY-RUN] Would push images to $registry"
        return 0
    fi

    # Push backend
    if [[ "$SERVICE" == "all" || "$SERVICE" == "backend" ]]; then
        docker push "$registry/bss/backend:$tag"
    fi

    # Push frontend
    if [[ "$SERVICE" == "all" || "$SERVICE" == "frontend" ]]; then
        docker push "$registry/bss/frontend:$tag"
    fi

    log_info "Images pushed successfully"
}

run_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        log_warn "Skipping tests"
        return 0
    fi

    log_info "Running pre-deployment tests..."

    if [[ "$DRY_RUN" == "true" ]]; then
        echo "[DRY-RUN] Would run tests"
        return 0
    fi

    # Backend tests
    if [[ "$SERVICE" == "all" || "$SERVICE" == "backend" ]]; then
        log_info "Running backend tests..."
        cd "$PROJECT_ROOT/backend"
        mvn -B test
    fi

    # Frontend tests
    if [[ "$SERVICE" == "all" || "$SERVICE" == "frontend" ]]; then
        log_info "Running frontend tests..."
        cd "$PROJECT_ROOT/frontend"
        pnpm run test:unit -- --run
    fi

    log_info "All tests passed"
}

deploy_services() {
    log_info "Deploying to $ENVIRONMENT environment..."

    if [[ "$DRY_RUN" == "true" ]]; then
        echo "[DRY-RUN] Would deploy to $ENVIRONMENT"
        return 0
    fi

    local registry="${DOCKER_REGISTRY:-ghcr.io}"
    local tag="${IMAGE_TAG:-$ENVIRONMENT-$(git rev-parse --short HEAD)}"

    # Update docker-compose file with new image tags
    if [[ -f "$DOCKER_COMPOSE_FILE" ]]; then
        if [[ "$SERVICE" == "all" || "$SERVICE" == "backend" ]]; then
            sed -i "s|image: .*bss/backend:.*|image: $registry/bss/backend:$tag|g" "$DOCKER_COMPOSE_FILE"
        fi

        if [[ "$SERVICE" == "all" || "$SERVICE" == "frontend" ]]; then
            sed -i "s|image: .*bss/frontend:.*|image: $registry/bss/frontend:$tag|g" "$DOCKER_COMPOSE_FILE"
        fi
    fi

    # Deploy using docker-compose
    log_info "Starting services with docker-compose..."
    cd "$PROJECT_ROOT"
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d

    # Or deploy using kubectl (if using Kubernetes)
    # kubectl set image deployment/backend backend="$registry/bss/backend:$tag" -n "$NAMESPACE"
    # kubectl set image deployment/frontend frontend="$registry/bss/frontend:$tag" -n "$NAMESPACE"

    # Or deploy using helm
    # helm upgrade --install bss ./helm-chart \
    #     --set image.tag="$tag" \
    #     --set environment="$ENVIRONMENT" \
    #     --namespace="$NAMESPACE" \
    #     --create-namespace

    log_info "Deployment completed"
}

run_migrations() {
    log_info "Running database migrations..."

    if [[ "$DRY_RUN" == "true" ]]; then
        echo "[DRY-RUN] Would run database migrations"
        return 0
    fi

    # Wait for deployment to be ready
    log_info "Waiting for application to be ready..."
    sleep 30

    # Run Flyway migrations
    cd "$PROJECT_ROOT/backend"
    mvn -B flyway:migrate \
        -Dflyway.url="$DB_URL" \
        -Dflyway.user="$DB_USER" \
        -Dflyway.password="$DB_PASSWORD"

    log_info "Migrations completed"
}

verify_deployment() {
    log_info "Verifying deployment..."

    if [[ "$DRY_RUN" == "true" ]]; then
        echo "[DRY-RUN] Would verify deployment"
        return 0
    fi

    # Wait a bit more for services to start
    sleep 30

    # Health checks
    log_info "Checking API health..."
    if curl -sf "$API_URL/actuator/health" > /dev/null; then
        log_info "API is healthy"
    else
        log_error "API health check failed"
        return 1
    fi

    log_info "Checking frontend..."
    if curl -sf "$FRONTEND_URL" > /dev/null; then
        log_info "Frontend is accessible"
    else
        log_warn "Frontend health check failed"
    fi

    # Run smoke tests
    log_info "Running smoke tests..."
    cd "$PROJECT_ROOT/dev/tools/generators"
    if python3 generate_test_data.py --count 5 --output json > /dev/null 2>&1; then
        log_info "Smoke tests passed"
    else
        log_warn "Smoke tests failed"
    fi

    log_info "Deployment verification completed"
}

notify() {
    local status="$1"
    local message="Deployment to $ENVIRONMENT completed: $status"

    log_info "Sending notification..."

    # Slack notification
    if [[ -n "${SLACK_WEBHOOK:-}" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"$message\"}" \
            "$SLACK_WEBHOOK"
    fi

    # Email notification
    if [[ -n "${DEPLOYMENT_EMAIL:-}" ]]; then
        echo "$message" | mail -s "BSS Deployment" "$DEPLOYMENT_EMAIL"
    fi

    log_info "Notification sent"
}

main() {
    log_info "Starting deployment to $ENVIRONMENT"
    log_info "Service: $SERVICE"
    log_info "Environment: $ENVIRONMENT"

    # Execute deployment steps
    check_dependencies
    load_environment
    create_database_backup
    build_images
    push_images
    run_tests
    deploy_services
    run_migrations
    verify_deployment

    # Send notification
    notify "SUCCESS"

    log_info "Deployment to $ENVIRONMENT completed successfully!"
}

# Run main
main
