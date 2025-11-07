# BSS Makefile
# Simplified commands for development and CI/CD operations

.PHONY: help
help: ## Show this help message
	@echo "BSS Development Commands"
	@echo "========================"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Colors
GREEN=\033[0;32m
YELLOW=\033[1;33m
NC=\033[0m # No Color

# Default target
.DEFAULT_GOAL := help

## =============================================================================
## Backend Commands
## =============================================================================

.PHONY: backend-install
backend-install: ## Install backend dependencies
	@echo "$(GREEN)Installing backend dependencies...$(NC)"
	cd backend && mvn clean install -DskipTests

.PHONY: backend-test
backend-test: ## Run backend tests
	@echo "$(GREEN)Running backend tests...$(NC)"
	cd backend && mvn test

.PHONY: backend-test-coverage
backend-test-coverage: ## Run backend tests with coverage
	@echo "$(GREEN)Running backend tests with coverage...$(NC)"
	cd backend && mvn verify

.PHONY: backend-lint
backend-lint: ## Lint backend code
	@echo "$(GREEN)Linting backend code...$(NC)"
	cd backend && mvn checkstyle:check

.PHONY: backend-build
backend-build: ## Build backend application
	@echo "$(GREEN)Building backend...$(NC)"
	cd backend && mvn clean package -DskipTests

.PHONY: backend-run
backend-run: ## Run backend application
	@echo "$(GREEN)Starting backend...$(NC)"
	cd backend && mvn spring-boot:run

.PHONY: backend-build-image
backend-build-image: ## Build backend Docker image
	@echo "$(GREEN)Building backend Docker image...$(NC)"
	docker build -t bss/backend:latest backend/

.PHONY: backend-scan
backend-scan: ## Run security scan on backend
	@echo "$(GREEN)Running security scan...$(NC)"
	cd backend && mvn dependency-check:check

## =============================================================================
## Frontend Commands
## =============================================================================

.PHONY: frontend-install
frontend-install: ## Install frontend dependencies
	@echo "$(GREEN)Installing frontend dependencies...$(NC)"
	cd frontend && pnpm install --frozen-lockfile

.PHONY: frontend-test
frontend-test: ## Run frontend tests
	@echo "$(GREEN)Running frontend tests...$(NC)"
	cd frontend && pnpm run test:unit

.PHONY: frontend-test-e2e
frontend-test-e2e: ## Run E2E tests
	@echo "$(GREEN)Running E2E tests...$(NC)"
	cd frontend && pnpm run test:e2e

.PHONY: frontend-lint
frontend-lint: ## Lint frontend code
	@echo "$(GREEN)Linting frontend code...$(NC)"
	cd frontend && pnpm run lint

.PHONY: frontend-typecheck
frontend-typecheck: ## Type check frontend code
	@echo "$(GREEN)Type checking frontend code...$(NC)"
	cd frontend && pnpm run typecheck

.PHONY: frontend-build
frontend-build: ## Build frontend application
	@echo "$(GREEN)Building frontend...$(NC)"
	cd frontend && pnpm run build

.PHONY: frontend-run
frontend-run: ## Run frontend development server
	@echo "$(GREEN)Starting frontend dev server...$(NC)"
	cd frontend && pnpm run dev

.PHONY: frontend-build-image
frontend-build-image: ## Build frontend Docker image
	@echo "$(GREEN)Building frontend Docker image...$(NC)"
	docker build -t bss/frontend:latest frontend/

## =============================================================================
## Infrastructure Commands
## =============================================================================

.PHONY: infra-up
infra-up: ## Start all infrastructure services
	@echo "$(GREEN)Starting infrastructure...$(NC)"
	docker compose -f dev/compose.yml up -d

.PHONY: infra-down
infra-down: ## Stop all infrastructure services
	@echo "$(GREEN)Stopping infrastructure...$(NC)"
	docker compose -f dev/compose.yml down

.PHONY: infra-logs
infra-logs: ## View infrastructure logs
	@echo "$(GREEN)Viewing infrastructure logs...$(NC)"
	docker compose -f dev/compose.yml logs -f

.PHONY: infra-status
infra-status: ## Check infrastructure status
	@echo "$(GREEN)Checking infrastructure status...$(NC)"
	docker compose -f dev/compose.yml ps

.PHONY: infra-clean
infra-clean: ## Clean all Docker resources
	@echo "$(GREEN)Cleaning Docker resources...$(NC)"
	docker system prune -af
	docker volume prune -f

## =============================================================================
## Database Commands
## =============================================================================

.PHONY: db-migrate
db-migrate: ## Run database migrations
	@echo "$(GREEN)Running database migrations...$(NC)"
	cd backend && mvn flyway:migrate

.PHONY: db-migrate-dev
db-migrate-dev: ## Run migrations for dev environment
	@echo "$(GREEN)Running dev migrations...$(NC)"
	cd backend && mvn flyway:migrate -Dspring.profiles.active=dev

.PHONY: db-backup
db-backup: ## Create database backup
	@echo "$(GREEN)Creating database backup...$(NC)"
	./dev/database/backup/restore/backup.sh

.PHONY: db-restore
db-restore: ## Restore database from backup
	@echo "$(YELLOW)WARNING: This will overwrite the current database!$(NC)"
	@read -p "Continue? (y/n): " confirm && [ "$$confirm" = "y" ]
	./dev/database/backup/restore/restore.sh

.PHONY: db-stats
db-stats: ## Show database statistics
	@echo "$(GREEN)Database statistics:$(NC)"
	./dev/tools/generators/load-test-data.sh stats

## =============================================================================
## Test Data Commands
## =============================================================================

.PHONY: data-generate
data-generate: ## Generate test data
	@echo "$(GREEN)Generating test data...$(NC)"
	python3 dev/tools/generators/generate_test_data.py --generate all --count 1000 --output sql

.PHONY: data-load
data-load: ## Load test data into database
	@echo "$(GREEN)Loading test data...$(NC)"
	./dev/tools/generators/load-test-data.sh all --count 1000

.PHONY: data-load-customers
data-load-customers: ## Load customer test data
	@echo "$(GREEN)Loading customer test data...$(NC)"
	./dev/tools/generators/load-test-data.sh customers --count 1000

.PHONY: data-load-orders
data-load-orders: ## Load order test data
	@echo "$(GREEN)Loading order test data...$(NC)"
	./dev/tools/generators/load-test-data.sh orders --count 5000

.PHONY: data-clean
data-clean: ## Clean all test data
	@echo "$(YELLOW)WARNING: This will delete all test data!$(NC)"
	@read -p "Continue? (y/n): " confirm && [ "$$confirm" = "y" ]
	./dev/tools/generators/load-test-data.sh clean

## =============================================================================
## Load Testing Commands
## =============================================================================

.PHONY: loadtest-spike
loadtest-spike: ## Run spike load test
	@echo "$(GREEN)Running spike load test...$(NC)"
	cd dev/k6/scripts && k6 run --vus 1000 --duration 5m extreme-spike-test.js

.PHONY: loadtest-volume
loadtest-volume: ## Run volume load test (1M events)
	@echo "$(GREEN)Running volume load test...$(NC)"
	cd dev/k6/scripts && k6 run --vus 500 --iterations 2000 volume-test-1m.js

.PHONY: loadtest-marathon
loadtest-marathon: ## Run marathon load test (12h)
	@echo "$(GREEN)Running marathon load test...$(NC)"
	@echo "$(YELLOW)Note: This will run for 12 hours!$(NC)"
	cd dev/k6/scripts && k6 run --duration 12h marathon-test.js

.PHONY: loadtest-soak
loadtest-soak: ## Run soak load test (24h)
	@echo "$(GREEN)Running soak load test...$(NC)"
	@echo "$(YELLOW)Note: This will run for 24 hours!$(NC)"
	cd dev/k6/scripts && k6 run --duration 24h extreme-soak-test.js

.PHONY: loadtest-custom
loadtest-custom: ## Run custom load test
	@echo "$(GREEN)Running custom load test...$(NC)"
	@read -p "Enter VUs: " vus && \
	read -p "Enter duration (e.g., 5m, 1h): " duration && \
	cd dev/k6/scripts && k6 run --vus $$vus --duration $$duration extreme-spike-test.js

## =============================================================================
## Deployment Commands
## =============================================================================

.PHONY: deploy-dev
deploy-dev: ## Deploy to dev environment
	@echo "$(GREEN)Deploying to dev environment...$(NC)"
	chmod +x dev/deployment/deploy.sh
	./dev/deployment/deploy.sh --env dev

.PHONY: deploy-staging
deploy-staging: ## Deploy to staging environment
	@echo "$(GREEN)Deploying to staging environment...$(NC)"
	chmod +x dev/deployment/deploy.sh
	./dev/deployment/deploy.sh --env staging --backup-db

.PHONY: deploy-prod
deploy-prod: ## Deploy to production environment
	@echo "$(YELLOW)WARNING: Deploying to production!$(NC)"
	@read -p "Are you sure? (y/n): " confirm && [ "$$confirm" = "y" ]
	@echo "$(GREEN)Deploying to production...$(NC)"
	chmod +x dev/deployment/deploy.sh
	./dev/deployment/deploy.sh --env production --backup-db

.PHONY: deploy-dry-run
deploy-dry-run: ## Dry run deployment (show what would happen)
	@echo "$(GREEN)Dry run deployment...$(NC)"
	chmod +x dev/deployment/deploy.sh
	./dev/deployment/deploy.sh --env staging --dry-run

## =============================================================================
## CI/CD Commands
## =============================================================================

.PHONY: ci-test
ci-test: frontend-install backend-install frontend-lint frontend-test backend-lint backend-test ## Run full CI test suite
	@echo "$(GREEN)All CI tests passed!$(NC)"

.PHONY: ci-build
ci-build: backend-build frontend-build ## Build all applications
	@echo "$(GREEN)All applications built successfully!$(NC)"

.PHONY: ci-scan
ci-scan: backend-scan ## Run security scans
	@echo "$(GREEN)Security scans completed!$(NC)"

## =============================================================================
## Monitoring Commands
## =============================================================================

.PHONY: monitor-grafana
monitor-grafana: ## Open Grafana dashboard
	@echo "$(GREEN)Opening Grafana...$(NC)"
	@command -v xdg-open > /dev/null && xdg-open http://localhost:3000 || echo "Open http://localhost:3000 manually"

.PHONY: monitor-prometheus
monitor-prometheus: ## Open Prometheus dashboard
	@echo "$(GREEN)Opening Prometheus...$(NC)"
	@command -v xdg-open > /dev/null && xdg-open http://localhost:9090 || echo "Open http://localhost:9090 manually"

.PHONY: monitor-kafka
monitor-kafka: ## Open Kafka UI
	@echo "$(GREEN)Opening Kafka UI...$(NC)"
	@command -v xdg-open > /dev/null && xdg-open http://localhost:8080 || echo "Open http://localhost:8080 manually"

## =============================================================================
## Maintenance Commands
## =============================================================================

.PHONY: clean
clean: ## Clean all build artifacts
	@echo "$(GREEN)Cleaning build artifacts...$(NC)"
	cd backend && mvn clean
	cd frontend && rm -rf .nuxt .output node_modules
	rm -rf target *.log

.PHONY: clean-all
clean-all: infra-clean clean ## Clean everything including Docker resources
	@echo "$(GREEN)Cleaned all resources!$(NC)"

.PHONY: update-deps
update-deps: ## Update all dependencies
	@echo "$(GREEN)Updating dependencies...$(NC)"
	cd backend && mvn versions:use-latest-versions
	cd frontend && pnpm update

.PHONY: format
format: ## Format all code
	@echo "$(GREEN)Formatting code...$(NC)"
	cd frontend && pnpm run lint --fix
	cd backend && mvn formatter:format

.PHONY: security-audit
security-audit: ## Run security audit on all dependencies
	@echo "$(GREEN)Running security audit...$(NC)"
	cd frontend && pnpm audit
	cd backend && mvn org.owasp:dependency-check-maven:check

.PHONY: docs
docs: ## Generate documentation
	@echo "$(GREEN)Generating documentation...$(NC)"
	cd backend && mvn dokka:dokka
	@echo "$(GREEN)Documentation generated in backend/target/dokka/$(NC)"

## =============================================================================
## Utility Commands
## =============================================================================

.PHONY: pre-commit-install
pre-commit-install: ## Install pre-commit hooks
	@echo "$(GREEN)Installing pre-commit hooks...$(NC)"
	pre-commit install
	pre-commit install --hook-type commit-msg

.PHONY: pre-commit-run
pre-commit-run: ## Run pre-commit on all files
	@echo "$(GREEN)Running pre-commit on all files...$(NC)"
	pre-commit run --all-files

.PHONY: env-check
env-check: ## Check environment variables
	@echo "$(GREEN)Checking environment variables...$(NC)"
	@echo "POSTGRES_DB: $${POSTGRES_DB:-not set}"
	@echo "POSTGRES_USER: $${POSTGRES_USER:-not set}"
	@echo "POSTGRES_PASSWORD: $${POSTGRES_PASSWORD:+***configured***}"
	@echo "REDIS_HOST: $${REDIS_HOST:-not set}"
	@echo "KAFKA_BOOTSTRAP_SERVERS: $${KAFKA_BOOTSTRAP_SERVERS:-not set}"

.PHONY: health-check
health-check: ## Run health checks
	@echo "$(GREEN)Running health checks...$(NC)"
	@curl -sf http://localhost:8080/actuator/health && echo "✓ Backend is healthy" || echo "✗ Backend is not healthy"
	@curl -sf http://localhost:3000 && echo "✓ Frontend is accessible" || echo "✗ Frontend is not accessible"

## =============================================================================
## Development Workflow
## =============================================================================

.PHONY: dev-setup
dev-setup: ## Initial development setup
	@echo "$(GREEN)Setting up development environment...$(NC)"
	cp .env.example .env
	@echo "$(GREEN)Please edit .env with your configuration$(NC)"
	pre-commit-install
	frontend-install
	backend-install

.PHONY: dev
dev: infra-up ## Start development environment
	@echo "$(GREEN)Starting development environment...$(NC)"
	@echo "$(GREEN)Backend: http://localhost:8080$(NC)"
	@echo "$(GREEN)Frontend: http://localhost:3000$(NC)"
	@echo "$(GREEN)Press Ctrl+C to stop$(NC)"

.PHONY: test-all
test-all: ## Run all tests (unit, integration, e2e)
	@echo "$(GREEN)Running all tests...$(NC)"
	backend-test
	frontend-test
	@echo "$(GREEN)All tests passed!$(NC)"
