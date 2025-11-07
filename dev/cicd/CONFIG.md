# CI/CD Configuration Reference

This document provides detailed configuration options for the BSS CI/CD pipeline.

## GitHub Actions Environment Protection

Configure environment protection rules in `.github/environments/`:

### staging-environment.yml

```yaml
# Environment protection for staging
deployment_environment: staging
deployment_branch_policy: protected_branches

protection_rules:
  # Required reviewers
  required_reviewers:
    - team: "tech-leads"
      count: 1
    - team: "devops-team"
      count: 1

  # Required status checks
  required_status_checks:
    strict: true
    checks:
      - "Backend CI Pipeline/test"
      - "Backend CI Pipeline/build"
      - "Frontend CI Pipeline/lint-and-typecheck"
      - "Frontend CI Pipeline/test"
      - "Security Scan"

  # Dismiss stale reviews
  dismiss_stale_reviews: true

  # Require branch to be up to date
  require_branch_to_be_up_to_date: true

  # Enforce for administrators
  enforce_for_administrators: true

  # Wait timer (minutes)
  wait_timer: 0

  # Created by
  created_by: "devops-team"

  # Updated by
  updated_by: "devops-team"
```

### production-environment.yml

```yaml
# Environment protection for production
deployment_environment: production
deployment_branch_policy: protected_branches

protection_rules:
  # Required reviewers (more strict for production)
  required_reviewers:
    - team: "tech-leads"
      count: 2
    - team: "devops-team"
      count: 1
    - team: "security-team"
      count: 1

  # Required status checks
  required_status_checks:
    strict: true
    checks:
      - "Backend CI Pipeline/test"
      - "Backend CI Pipeline/build"
      - "Backend CI Pipeline/security-scan"
      - "Frontend CI Pipeline/lint-and-typecheck"
      - "Frontend CI Pipeline/test"
      - "Frontend CI Pipeline/e2e-test"
      - "Security Scan"
      - "Load Testing/load-test-spike"
      - "Load Testing/load-test-volume"
      - "Deployment"

  # Dismiss stale reviews
  dismiss_stale_reviews: true

  # Require branch to be up to date
  require_branch_to_be_up_to_date: true

  # Enforce for administrators
  enforce_for_administrators: true

  # Wait timer (24 hours for production)
  wait_timer: 1440

  # Created by
  created_by: "devops-team"

  # Updated by
  updated_by: "devops-team"
```

## GitHub Branch Protection Rules

Configure in GitHub → Settings → Branches:

### main branch protection

```yaml
# Require pull request reviews
require_pull_request_reviews:
  required_approving_review_count: 2
  dismiss_stale_reviews: true
  require_code_owner_reviews: true
  restrictions:
    teams: ["tech-leads", "devops-team"]

# Require status checks
require_status_checks:
  strict: true
  checks:
    - "Backend CI Pipeline/test"
    - "Backend CI Pipeline/build"
    - "Frontend CI Pipeline/lint-and-typecheck"
    - "Frontend CI Pipeline/test"
    - "Security Scan"

# Require branches to be up to date
require_branches_to_be_up_to_date: true

# Enforce for administrators
enforce_admins: true

# Restrict pushes
restrictions:
  teams: ["tech-leads", "devops-team"]
```

### develop branch protection

```yaml
# Require pull request reviews
require_pull_request_reviews:
  required_approving_review_count: 1
  dismiss_stale_reviews: true

# Require status checks
require_status_checks:
  strict: false
  checks:
    - "Backend CI Pipeline/test"
    - "Backend CI Pipeline/build"
    - "Frontend CI Pipeline/lint-and-typecheck"
    - "Frontend CI Pipeline/test"

# Enforce for administrators
enforce_admins: false
```

## GitHub CodeQL Configuration

### .github/codeql/codeql-config.yml

```yaml
name: "BSS CodeQL Security Analysis"

query-filters:
  - include:
      kind: problem
      precision: high
  - include:
      kind: path-problem
      precision: high

queries:
  - uses: security-and-quality

paths:
  - "backend/src/main/java"
  - "frontend/app"

paths-ignore:
  - "**/*.test.*"
  - "**/test/**"
  - "**/tests/**"
  - "**/__tests__/**"

disable-default-queries: false

```

## GitHub Security Policy

### .github/SECURITY.md

```markdown
# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

Please do not report security vulnerabilities through public GitHub issues.

Instead, please report them to:

- Email: security@bss.example.com
- Key ID: 0x1234567890ABCDEF
- PGP Fingerprint: ABCD 1234 5678 90AB CDEF 1234 5678 90AB CDEF 1234

## Response Process

1. Acknowledgment within 48 hours
2. Initial assessment within 72 hours
3. Fix development
4. Security testing
5. Release with CVE (if applicable)
6. Public disclosure

## Security Updates

Subscribe to our security mailing list for updates.
```

## renovate.json

```json
{
  "enabled": true,
  "automerge": false,
  "commitMessage": "{{semanticPrefix}}{{#if isSingleCommit}}{{commitMessage}}{{else}}{{#each commits}}{{header}}{{#if ../lastCommit}}{{#if (ne @index 0)}} - {{/if}}{{/if}}{{/each}}{{/if}}",
  "commitMessageAction": "Update",
  "commitMessageTopic": "{{depName}}",
  "commitMessageExtra": "to {{newVersion}}",
  "schedule": ["before 5am on monday"],
  "timezone": "Europe/Warsaw",
  "prHourlyLimit": 2,
  "prConcurrentLimit": 10,
  "rebaseWhen": "conflicted",
  "lockFileMaintenance": {
    "enabled": true,
    "schedule": ["before 5am on sunday"]
  },
  "packageRules": [
    {
      "description": "Auto-merge minor and patch updates for dependencies",
      "matchUpdateTypes": ["minor", "patch"],
      "automerge": true
    },
    {
      "description": "Require review for major updates",
      "matchUpdateTypes": ["major"],
      "automerge": false,
      "reviewers": ["tech-leads"]
    },
    {
      "description": "Group JavaScript/TypeScript dependencies",
      "matchPackagePatterns": ["^@types/", "^eslint", "^prettier", "^typescript"],
      "groupName": "frontend-dev-dependencies"
    },
    {
      "description": "Group Spring Boot dependencies",
      "matchPackagePatterns": ["org.springframework.boot"],
      "groupName": "spring-boot-dependencies"
    }
  ]
}
```

## SonarCloud Configuration

### sonar-project.properties

```properties
# Project information
sonar.projectKey=bss
sonar.organization=your-org
sonar.projectName=BSS
sonar.projectVersion=1.0.0

# Source code
sonar.sources=backend/src/main/java,frontend/app
sonar.tests=backend/src/test/java,frontend/tests
sonar.exclusions=**/*.test.java,**/*.spec.ts,**/node_modules/**,**/dist/**,**/build/**

# Language-specific settings
sonar.java.binaries=backend/target/classes
sonar.java.test.binaries=backend/target/test-classes
sonar.java.libraries=backend/target/dependency/*

sonar.javascript.lcov.reportPaths=frontend/coverage/lcov.info

# Quality gate
sonar.qualitygate.wait=true

# Exclusions
sonar.exclusions=**/generated/**,**/AutoConfiguration*.java,**/*Application.java

# Test execution
sonar.test.inclusions=**/*Test.java,**/*Tests.java,**/*.test.ts,**/*.spec.ts

# Coverage exclusions
sonar.coverage.exclusions=**/*Test.java,**/*Tests.java,**/*.test.ts,**/*.spec.ts,**/main/java/com/droid/bss/Application.java

# Duplication detection
sonar.cpd.exclusions=**/*Entity.java,**/*Dto.java
```

## Snyk Configuration

### .snyk

```yaml
# Snyk configuration
version: v1.25.0

language-settings:
  java:
    target-file: backend/pom.xml

exclude:
  global:
    - '**/*.md'
    - '**/*.txt'
    - '**/node_modules/**'
    - '**/target/**'
    - '**/build/**'
    - '**/.git/**'
    - '**/.idea/**'
    - '**/.vscode/**'
    - '**/.DS_Store'

patch: {}
```

### .snykignore

```
# Ignore test dependencies
# Ignore development tools
# Path patterns
backend/src/test/**
frontend/tests/**
dev/**
```

## OWASP ZAP Configuration

### .zap/zap-baseline.py

```python
#!/usr/bin/env python3
"""
OWASP ZAP Baseline Script for BSS

Runs active security scans against the application.
"""

import os
import sys
import time
from zapv2 import ZAPv2

# Configuration
API_KEY = os.environ.get('ZAP_API_KEY', '')
TARGET = os.environ.get('TARGET_URL', 'http://localhost:8080')

# Initialize ZAP
zap = ZAPv2(apikey=API_KEY, proxies={'http': 'http://127.0.0.1:8080', 'https': 'http://127.0.0.1:8080'})

# Spider scan
print(f'Starting spider scan on {TARGET}...')
scan_id = zap.spider.scan(TARGET)
while int(zap.spider.status(scan_id)) < 100:
    print(f'Spider progress: {zap.spider.status(scan_id)}%')
    time.sleep(2)

# Active scan
print('Starting active scan...')
scan_id = zap.ascan.scan(TARGET)
while int(zap.ascan.status(scan_id)) < 100:
    print(f'Active scan progress: {zap.ascan.status(scan_id)}%')
    time.sleep(5)

# Report
alerts = zap.core.alerts()
print(f'Found {len(alerts)} alerts')

# Exit with error if high/critical vulnerabilities
for alert in alerts:
    if alert.get('risk') in ['High', 'Critical']:
        print(f'VULNERABILITY: {alert.get("name")} - {alert.get("risk")}')
        sys.exit(1)

print('No high/critical vulnerabilities found')
```

## Trivy Configuration

### .trivyignore

```
# Ignore specific CVEs
# Example: CVE-2021-12345 - fixed in next version
CVE-2021-12345

# Ignore specific files
usr/lib/x86_64-linux-gnu/libc.so.6
```

## GitGuardian Configuration

### .gitguardian.yaml

```yaml
version: v1.25.0

exclude:
  - filepath: "**/*.md"
  - filepath: "**/*.txt"
  - filepath: "**/test/**"
  - filepath: "**/tests/**"
  - filepath: "**/dev/**"

ignore_content_default_keys:
  - -d flag
  - -D flag
  - --password
  - --secret
  - PASSWORD
  - SECRET
```

## Prometheus Alerting Rules

### prometheus/alerting-rules.yml

```yaml
groups:
  - name: bss-alerts
    rules:
      - alert: DeploymentFailed
        expr: kube_job_status_failed{job="bss-deploy"} > 0
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "BSS deployment failed"
          description: "BSS deployment to {{ $labels.namespace }} has failed"

      - alert: HighErrorRate
        expr: |
          rate(http_requests_total{status=~"5.."}[5m]) /
          rate(http_requests_total[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: DatabaseConnectionFailure
        expr: up{job="postgres"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database is down"
          description: "PostgreSQL database is not responding"
```

## Notification Configuration

### Slack Notification Template

```json
{
  "channel": "#deployments",
  "username": "BSS CI/CD",
  "icon_emoji": ":rocket:",
  "attachments": [
    {
      "color": "good",
      "title": "Deployment Successful",
      "fields": [
        {
          "title": "Environment",
          "value": "${ENVIRONMENT}",
          "short": true
        },
        {
          "title": "Service",
          "value": "${SERVICE}",
          "short": true
        },
        {
          "title": "Commit",
          "value": "<${COMMIT_URL}|${SHORT_SHA}>",
          "short": true
        },
        {
          "title": "Author",
          "value": "${AUTHOR}",
          "short": true
        }
      ],
      "footer": "BSS CI/CD",
      "ts": ${TIMESTAMP}
    }
  ]
}
```

### Email Template

```html
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; }
        .header { background-color: #4CAF50; color: white; padding: 10px; }
        .content { padding: 20px; }
        .success { color: #4CAF50; }
        .failure { color: #f44336; }
        .info { color: #2196F3; }
        table { border-collapse: collapse; width: 100%; }
        th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
    </style>
</head>
<body>
    <div class="header">
        <h2>${EVENT_TYPE}</h2>
    </div>
    <div class="content">
        <p><strong>Environment:</strong> ${ENVIRONMENT}</p>
        <p><strong>Service:</strong> ${SERVICE}</p>
        <p><strong>Commit:</strong> ${COMMIT_SHA}</p>
        <p><strong>Author:</strong> ${AUTHOR}</p>
        <p><strong>Status:</strong> <span class="${STATUS_CLASS}">${STATUS}</span></p>

        <h3>Build Details</h3>
        <table>
            <tr><th>Build Number</th><td>${BUILD_NUMBER}</td></tr>
            <tr><th>Duration</th><td>${DURATION}</td></tr>
            <tr><th>Branch</th><td>${BRANCH}</td></tr>
        </table>

        <h3>Test Results</h3>
        <p>Unit Tests: ${UNIT_TEST_COUNT} (${UNIT_TEST_PASSED} passed, ${UNIT_TEST_FAILED} failed)</p>
        <p>Integration Tests: ${INTEGRATION_TEST_COUNT} (${INTEGRATION_TEST_PASSED} passed, ${INTEGRATION_TEST_FAILED} failed)</p>
        <p>E2E Tests: ${E2E_TEST_COUNT} (${E2E_TEST_PASSED} passed, ${E2E_TEST_FAILED} failed)</p>

        <p><a href="${BUILD_URL}">View build details</a></p>
    </div>
</body>
</html>
```

## Performance Monitoring

### Lighthouse CI Configuration

### .lighthouserc.json

```json
{
  "ci": {
    "collect": {
      "url": [
        "http://localhost:3000/",
        "http://localhost:3000/dashboard",
        "http://localhost:3000/customers"
      ],
      "numberOfRuns": 3,
      "settings": {
        "chromeFlags": "--no-sandbox --disable-dev-shm-usage"
      }
    },
    "assert": {
      "preset": "lighthouse:recommended",
      "assertions": {
        "categories:performance": ["warn", {"minScore": 0.8}],
        "categories:accessibility": ["error", {"minScore": 0.9}],
        "categories:best-practices": ["warn", {"minScore": 0.8}],
        "categories:seo": ["warn", {"minScore": 0.8}]
      }
    },
    "upload": {
      "target": "temporary-public-storage"
    }
  }
}
```

## Backup Verification Script

### verify-backup.sh

```bash
#!/bin/bash
# Verify database backup integrity

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file.sql>"
    exit 1
fi

# Verify backup file
if ! file "$BACKUP_FILE" | grep -q "gzip compressed"; then
    echo "ERROR: Backup file is not gzip compressed"
    exit 1
fi

# Test gzip integrity
if ! gzip -t "$BACKUP_FILE"; then
    echo "ERROR: Backup file is corrupted"
    exit 1
fi

# Test SQL syntax
if ! zcat "$BACKUP_FILE" | head -100 | grep -q "PostgreSQL database dump"; then
    echo "ERROR: Backup file does not appear to be a valid PostgreSQL dump"
    exit 1
fi

echo "✓ Backup file is valid"

# Optional: Restore to test database
read -p "Restore to test database? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    zcat "$BACKUP_FILE" | psql test_bss
    echo "✓ Backup restored successfully"
fi
```
