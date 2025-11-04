# Testcontainers Guide - BSS Backend

## Overview

BSS Backend uses **Testcontainers** for integration testing with real services:
- PostgreSQL 18 (database)
- Kafka 7.4 (messaging)
- Redis 7 (caching & rate limiting)

## Quick Start

### Run All Tests

```bash
# Unit tests only (fast)
mvn test

# Integration tests (requires Docker)
mvn test -DfailIfNoTests=false

# Specific test
mvn test -Dtest=CustomerControllerWebTest
mvn test -Dtest=CustomerCrudIntegrationTest
```

### Run with Act (GitHub Actions locally)

```bash
# Install Act (one-time setup)
curl -s https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Run workflow locally
act push
act -j test  # Run specific job
```

## Test Categories

### ✅ Unit Tests (Fast)
- **HelloServiceTest** (4 tests) - Pure logic tests
- **CustomerTest** (7 tests) - Domain logic
- **ControllerWebTest** (37 tests) - Web layer with mocks

### 🔧 Integration Tests (Requires Docker)
- **CustomerCrudIntegrationTest** (10 tests) - Full CRUD flow
- **ProductCrudIntegrationTest** (10 tests)
- **OrderFlowIntegrationTest** (3 tests)
- **UpdateInvoiceIntegrationTest** (4 tests)
- **AuthIntegrationTest** (10 tests)

### 📦 Repository Tests
- **CustomerRepositoryDataJpaTest** (16 tests)
- **InvoiceRepositoryDataJpaTest** (25 tests)
- **ProductRepositoryDataJpaTest** (19 tests)

## Testcontainers Setup

### Docker Requirements
- Docker must be running
- PostgreSQL 18-alpine image (auto-pulled)
- Kafka 7.4 image (auto-pulled)
- Redis 7 image (auto-pulled)

### Configuration Files

1. **IntegrationTestConfiguration.java**
   ```java
   @TestConfiguration
   @Testcontainers
   public class IntegrationTestConfiguration {
       @Container
       static PostgreSQLContainer<?> postgres = ...
       @Container
       static KafkaContainer kafka = ...
       @Container
       static GenericContainer<?> redis = ...
   }
   ```

2. **application-test.yaml**
   - Test-specific configuration
   - Rate limiting disabled
   - Logging minimized

## Usage Examples

### Run Specific Test Categories

```bash
# Web layer tests only
mvn test -Dtest=*ControllerWebTest

# Unit tests only (no Docker needed)
mvn test -Dtest=HelloServiceTest,CustomerTest

# Integration tests (Docker required)
mvn test -Dtest=*IntegrationTest

# Repository tests
mvn test -Dtest=*Repository*Test
```

### Debug Tests

```bash
# Run tests with debugging
mvn test -Dtest=CustomerControllerWebTest -Dsurefire.debug=true

# Enable debug logging
mvn test -Dtest=CustomerCrudIntegrationTest -Dlogging.level.com.droid.bss=DEBUG
```

## Common Issues

### 1. Bean Definition Override

**Error:** `BeanDefinitionOverrideException: redisTemplate`

**Solution:** Fixed by using @Primary on RedisCacheConfiguration.redisTemplate

```java
@Bean
@Primary
public RedisTemplate<String, Object> redisTemplate(...) { ... }
```

### 2. Docker Not Running

**Error:** Docker connection errors

**Solution:**
```bash
# Check Docker status
docker ps

# Start Docker if needed
sudo systemctl start docker

# Verify Docker works
docker run hello-world
```

### 3. Database Connection Failed

**Error:** `password authentication failed for user "bss_app"`

**Solution:** Testcontainers PostgreSQL should auto-start. Check:
- Docker daemon is running
- No firewall blocking
- Enough disk space for containers

### 4. Test Timeouts

**Error:** Tests timeout during container startup

**Solution:**
```bash
# Increase timeout for specific test
mvn test -Dtest=CustomerCrudIntegrationTest -Dtest.integration.timeout=300
```

## CI/CD Integration

### GitHub Actions

Workflow file: `.github/workflows/test.yml`

```yaml
name: BSS Backend Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:18-alpine
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Run Tests
        run: mvn test -B
```

### Local CI/CD with Act

```bash
# Run full pipeline locally
act push

# Run specific job
act -j test

# Run with specific event
act pull_request
act workflow_dispatch
```

## Performance Tips

### Speed Up Tests

1. **Parallel Execution**
   ```bash
   mvn test -T 4  # Run 4 threads
   ```

2. **Skip Tests Temporarily**
   ```bash
   mvn test -Dtest=!CustomerCrudIntegrationTest  # Skip integration tests
   ```

3. **Clean Build Cache**
   ```bash
   mvn clean test  # Start fresh
   ```

### Resource Management

- **Memory:** Each test run ~512MB-1GB
- **Disk:** PostgreSQL + Kafka images ~500MB each
- **Time:** Unit tests ~3s, Integration tests ~30s

## Best Practices

### ✅ Do
- Run unit tests locally before pushing
- Use Act for local CI/CD testing
- Check Docker status if tests fail
- Clean up containers after tests

### ❌ Don't
- Commit failing integration tests
- Run full test suite on every commit
- Ignore Testcontainers warnings
- Use production database for tests

## Troubleshooting

### Container Won't Start

```bash
# Check Docker daemon
docker info

# Pull images manually
docker pull postgres:18-alpine
docker pull confluentinc/cp-kafka:7.4.0
docker pull redis:7-alpine

# Check disk space
df -h
```

### Port Conflicts

```bash
# Check if ports are in use
lsof -i :5432  # PostgreSQL
lsof -i :9092  # Kafka
lsof -i :6379  # Redis

# Kill process if needed
kill -9 <PID>
```

### Memory Issues

```bash
# Increase Docker memory limit
# Docker Desktop > Settings > Resources

# Or use smaller images
docker pull postgres:18-alpine  # vs postgres:18
docker pull redis:7-alpine     # vs redis:7
```

## Next Steps

1. **TASK 1.2:** Fix Repository tests with Testcontainers
2. **TASK 1.3:** Fix Integration tests (database connection)
3. **Sprint 1 Complete:** 149/184 errors fixed (81%)

## Support

If tests fail:
1. Check Docker daemon is running
2. Verify enough disk space (>2GB)
3. Check logs in `target/surefire-reports/`
4. Contact DevOps team

---

**Status:** ✅ Infrastructure ready, integration tests need database connection fix
**Last Updated:** 2025-10-30
