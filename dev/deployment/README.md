# BSS CI/CD Documentation

This directory contains comprehensive CI/CD configuration for the BSS (Business Support System) including GitHub Actions workflows, Jenkins pipelines, and deployment scripts.

## Table of Contents

- [Overview](#overview)
- [GitHub Actions Workflows](#github-actions-workflows)
- [Jenkins Pipeline](#jenkins-pipeline)
- [Deployment Scripts](#deployment-scripts)
- [Environment Configuration](#environment-configuration)
- [Secrets Management](#secrets-management)
- [Troubleshooting](#troubleshooting)

## Overview

The BSS project uses a multi-stage CI/CD pipeline supporting:
- **Continuous Integration**: Automated testing, linting, type checking
- **Security Scanning**: SAST, dependency checks, container scanning
- **Multi-environment Deployment**: dev, staging, production
- **Load Testing Integration**: Automated K6 tests post-deployment
- **Database Migration**: Automated Flyway migrations
- **Notifications**: Slack, email integration

### Supported Workflows

1. **Backend CI** (`ci-backend.yml`)
   - Unit & integration tests with Testcontainers
   - Code coverage with JaCoCo
   - Docker image building
   - Security scanning with Trivy

2. **Frontend CI** (`ci-frontend.yml`)
   - Linting & type checking
   - Unit tests with Vitest
   - E2E tests with Playwright
   - Docker image building

3. **Load Testing** (`load-testing.yml`)
   - Spike tests
   - Volume tests (1M+ events)
   - Marathon tests (12h endurance)
   - Soak tests (24h endurance)

4. **Deployment** (`deploy.yml`)
   - Multi-environment deployment
   - Database backup
   - Migration execution
   - Post-deployment verification

## GitHub Actions Workflows

### 1. Backend CI Pipeline

**File**: `.github/workflows/ci-backend.yml`

**Triggers**:
- Push to `main` or `develop` branches (changes in `backend/`)
- Pull requests to `main` or `develop`

**Jobs**:
- **test**: Runs unit and integration tests
  - Uses Testcontainers for PostgreSQL, Redis, Kafka
  - Generates JaCoCo coverage report
  - Uploads to Codecov

- **build**: Builds application
  - Maven package build
  - Docker image creation
  - Archives artifacts

- **security-scan**: Security scanning
  - Trivy vulnerability scanner
  - SARIF upload to GitHub Security tab

**Usage**:
```yaml
# Trigger manually
workflow_dispatch:

# Or push code
git push origin develop
```

### 2. Frontend CI Pipeline

**File**: `.github/workflows/ci-frontend.yml`

**Jobs**:
- **lint-and-typecheck**: ESLint + TypeScript type checking
- **test**: Unit tests with Vitest
- **build**: Production build
- **e2e-test**: E2E tests with Playwright

### 3. Load Testing Workflow

**File**: `.github/workflows/load-testing.yml`

**Triggers**:
- Manual dispatch with parameters
- Push to `main` branch (changes in `dev/k6/`)

**Parameters**:
- `test_type`: spike, volume, marathon, soak
- `vus`: Virtual users (for spike/volume)
- `duration`: Test duration (for marathon/soak)

**Usage**:
```bash
# Via GitHub UI
# 1. Go to Actions tab
# 2. Select "Load Testing" workflow
# 3. Click "Run workflow"
# 4. Choose test type and parameters
```

### 4. Deployment Workflow

**File**: `.github/workflows/deploy.yml`

**Triggers**:
- Push to `main` or `develop` branches
- Manual dispatch with environment selection

**Features**:
- Automatic environment detection
  - `main` → production
  - `develop` → staging
  - manual → choose environment
- Database backup (staging & production)
- Image building & pushing to registry
- Database migration execution
- Health checks
- Slack notifications

## Jenkins Pipeline

**File**: `Jenkinsfile`

A complete Jenkins Pipeline supporting:
- Multi-branch builds
- Parallel test execution
- Parameterized deployment
- Security scanning
- Load testing integration

### Parameters

- **ENVIRONMENT**: dev, staging, production
- **SERVICE**: all, backend, frontend
- **RUN_LOAD_TESTS**: boolean
- **SKIP_DB_BACKUP**: boolean

### Stages

1. **Prepare**: Environment preparation
2. **Test**: Unit, integration, and E2E tests
3. **Build**: Docker image building
4. **Security Scan**: SAST, container scan, dependency check
5. **Deploy**: Environment deployment
6. **Post-Deployment Tests**: Health checks, load tests
7. **Load Testing**: K6 integration
8. **Generate Report**: Deployment report

### Setup Jenkins

```bash
# 1. Install Jenkins
# 2. Install required plugins:
#    - Pipeline
#    - Docker Pipeline
#    - JUnit
#    - HTML Publisher
#    - Email Extension

# 3. Create pipeline job
# 4. Set Pipeline script from SCM
# 5. Configure repository and Jenkinsfile path

# 6. Add credentials:
#    - GitHub Token
#    - Docker Registry
#    - Database URLs
#    - Slack Webhook
```

## Deployment Scripts

### deploy.sh

**File**: `dev/deployment/deploy.sh`

A comprehensive deployment script supporting all environments.

**Usage**:
```bash
# Make executable
chmod +x dev/deployment/deploy.sh

# Deploy all services to staging
./dev/deployment/deploy.sh --env staging

# Deploy only backend to production
./dev/deployment/deploy.sh --env production --service backend --backup-db

# Dry run to see what would happen
./dev/deployment/deploy.sh --env dev --dry-run

# Deploy without building (use existing images)
./dev/deployment/deploy.sh --env staging --no-build
```

**Options**:
- `--env`: Target environment (dev, staging, production)
- `--service`: Service to deploy (all, backend, frontend)
- `--backup-db`: Create database backup (default: true)
- `--no-backup`: Skip backup
- `--build`: Build images (default: true)
- `--no-build`: Skip building
- `--skip-tests`: Skip pre-deployment tests
- `--dry-run`: Show commands without executing

### Features

- ✅ Dependency checking
- ✅ Environment configuration loading
- ✅ Database backup
- ✅ Image building & pushing
- ✅ Pre-deployment tests
- ✅ Service deployment (Docker Compose, Kubernetes, Helm)
- ✅ Database migration
- ✅ Health checks
- ✅ Smoke tests
- ✅ Notifications (Slack, email)

## Environment Configuration

### Development Environment

```bash
# .env file
POSTGRES_DB=bss
POSTGRES_USER=bss_app
POSTGRES_PASSWORD=secure_password
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

REDIS_HOST=localhost
REDIS_PORT=6379

KAFKA_BOOTSTRAP_SERVERS=localhost:9092

API_URL=http://localhost:8080
FRONTEND_URL=http://localhost:3000

DOCKER_REGISTRY=ghcr.io
```

### Staging Environment

```bash
STAGING_DB_HOST=staging-db.internal
STAGING_API_URL=https://api-staging.bss.example.com
STAGING_FRONTEND_URL=https://staging.bss.example.com
```

### Production Environment

```bash
PROD_DB_HOST=prod-db.internal
PROD_API_URL=https://api.bss.example.com
PROD_FRONTEND_URL=https://bss.example.com
```

## Secrets Management

### GitHub Actions Secrets

Configure in GitHub repository settings → Secrets and variables → Actions

**Required secrets**:

```bash
# Database
STAGING_DB_URL=postgresql://user:pass@staging-db:5432/bss
PROD_DB_URL=postgresql://user:pass@prod-db:5432/bss

# API URLs
STAGING_API_URL=https://api-staging.bss.example.com
PROD_API_URL=https://api.bss.example.com

# Docker Registry
REGISTRY_USERNAME=github_username
REGISTRY_PASSWORD=ghp_token

# AWS (for backups)
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=eu-west-1
BACKUP_BUCKET=bss-backups

# Notifications
SLACK_WEBHOOK=https://hooks.slack.com/services/...
DEPLOYMENT_EMAIL=devops@example.com

# Security
CODECOV_TOKEN=...
SONAR_TOKEN=...
```

### Jenkins Credentials

Configure in Jenkins → Manage Jenkins → Manage Credentials

**Credential IDs**:
- `github-token`: GitHub personal access token
- `docker-registry`: Docker registry credentials
- `staging-db-url`: Staging database connection string
- `prod-db-url`: Production database connection string
- `aws-access-key-id`: AWS access key
- `aws-secret-access-key`: AWS secret key
- `slack-webhook`: Slack webhook URL
- `sonarqube-token`: SonarQube token
- `codecov-token`: Codecov token

### Encryption

For sensitive data, use GitHub's secret scanning and encryption:

```bash
# GPG encryption for sensitive files
gpg --symmetric --cipher-algo AES256 secrets.env
gpg --decrypt secrets.env.gpg > secrets.env
```

## Troubleshooting

### Common Issues

#### 1. Build Failures

**Problem**: Maven build fails with dependency errors
```bash
# Solution: Clear Maven cache
mvn dependency:purge-local-repository
mvn clean install
```

**Problem**: pnpm install fails
```bash
# Solution: Clear pnpm cache
pnpm store prune
rm -rf node_modules
pnpm install
```

#### 2. Test Failures

**Problem**: Testcontainers fails to start
```bash
# Solution: Increase Docker resources
# - Memory: 4GB+
# - CPU: 2 cores+
# - Disk: 20GB+
```

**Problem**: Database connection timeout
```bash
# Solution: Increase connection timeout
mvn test -Dtest.timeout=300
```

#### 3. Deployment Failures

**Problem**: Database migration fails
```bash
# Solution: Check migration log
mvn flyway:info -Dflyway.url=... -Dflyway.user=... -Dflyway.password=...

# Repair corrupted migrations
mvn flyway:repair -Dflyway.url=... -Dflyway.user=... -Dflyway.password=...
```

**Problem**: Health check fails
```bash
# Solution: Check application logs
kubectl logs deployment/backend -n bss
# or
docker-compose logs backend
```

#### 4. Load Test Failures

**Problem**: K6 tests timeout
```bash
# Solution: Increase timeout
k6 run --duration 30m --vus 1000 test.js

# Or reduce load
k6 run --vus 500 --duration 15m test.js
```

**Problem**: Out of memory during load test
```bash
# Solution: Add memory limits
k6 run --memory-limit 1GB test.js
```

### Debug Mode

Enable debug mode for more verbose output:

```bash
# GitHub Actions
echo "ACTIONS_STEP_DEBUG=true" >> $GITHUB_ENV
echo "ACTIONS_RUNNER_DEBUG=true" >> $GITHUB_ENV

# Jenkins
# Add to Jenkinsfile:
set +x  # Enable debug
```

### Log Analysis

```bash
# Extract test results
find . -name "*.xml" -path "*/target/surefire-reports/*" -exec cat {} \;

# Check coverage
cat backend/target/site/jacoco/index.html

# View deployment logs
kubectl logs -l app=backend -n bss --tail=100
```

### Health Check Script

```bash
#!/bin/bash
# health-check.sh

API_URL="${API_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:3000}"

# Check API
if curl -sf "$API_URL/actuator/health" > /dev/null; then
    echo "✓ API is healthy"
else
    echo "✗ API is unhealthy"
    exit 1
fi

# Check frontend
if curl -sf "$FRONTEND_URL" > /dev/null; then
    echo "✓ Frontend is accessible"
else
    echo "✗ Frontend is not accessible"
    exit 1
fi

# Check database
if psql "$DB_URL" -c "SELECT 1" > /dev/null 2>&1; then
    echo "✓ Database is accessible"
else
    echo "✗ Database is not accessible"
    exit 1
fi

echo "All health checks passed"
```

## Best Practices

### 1. Branching Strategy

- **main**: Production branch, protected, requires PR review
- **develop**: Staging branch, integration testing
- **feature/***: Feature branches, short-lived
- **hotfix/***: Hotfix branches for critical issues

### 2. Pull Request Process

1. Create feature branch from `develop`
2. Make changes and add tests
3. Run full CI pipeline
4. Create PR to `develop`
5. Code review + automated checks
6. Merge to `develop`
7. Deploy to staging
8. Test in staging
9. Create PR from `develop` to `main`
10. Deploy to production

### 3. Commit Messages

Follow conventional commits:

```bash
feat: add customer query endpoint
fix: resolve memory leak in payment processor
docs: update API documentation
test: add integration tests for order service
refactor: simplify address validation logic
chore: update dependencies
```

### 4. Versioning

Use semantic versioning (SemVer):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

```bash
# Example versions
1.0.0  - Initial release
1.1.0  - Add new API endpoints
1.1.1  - Fix bug in validation
2.0.0  - Breaking changes (new API structure)
```

### 5. Rollback Strategy

```bash
# Quick rollback (Kubernetes)
kubectl rollout undo deployment/backend -n bss
kubectl rollout undo deployment/frontend -n bss

# Or use previous image tag
kubectl set image deployment/backend backend=ghcr.io/bss/backend:previous-tag -n bss

# Database rollback
mvn flyway:undo -Dflyway.url=... -Dflyway.user=... -Dflyway.password=...
```

### 6. Monitoring & Alerting

- Prometheus metrics
- Grafana dashboards
- ELK stack for logs
- PagerDuty for critical alerts
- Slack for notifications

### 7. Security

- Scan all dependencies (Snyk, OWASP)
- Scan containers (Trivy, Clair)
- SAST scanning (SonarQube, CodeQL)
- DAST testing (OWASP ZAP)
- Secrets scanning (GitLeaks)
- Regular dependency updates

## Support

- **Email**: devops@bss.example.com
- **Slack**: #devops
- **Jira**: BSS project

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Docker Deployment Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Kubernetes Deployment Guide](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [K6 Load Testing Guide](https://k6.io/docs/)
