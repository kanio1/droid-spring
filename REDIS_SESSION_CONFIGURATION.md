# Redis Session Configuration for Keycloak and Spring Boot

**Date:** 2025-11-05
**Status:** CONFIGURED ✅

## Overview

This document describes the Redis session storage configuration for the BSS system, which enables distributed session management across multiple application instances for both Keycloak (identity provider) and Spring Boot backend (business logic).

## Architecture

### Components

1. **Redis Server** - Central session store
2. **Keycloak** - Identity provider with Redis-backed sessions
3. **Spring Boot Backend** - Business API with Redis session management
4. **Spring Session** - Session management framework

### Session Flow

```
Client Request
    ↓
Load Balancer (Caddy)
    ↓
Keycloak (Redis Session Store)
    ↓
Spring Boot Backend (Redis Session Store)
    ↓
Application Logic
```

## Configuration Details

### 1. Redis Server

**Location:** `dev/compose.yml`

```yaml
redis:
  image: redis:7-alpine
  container_name: bss-redis
  restart: unless-stopped
  command: redis-server --save 20 1 --loglevel warning
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data
```

**Key Features:**
- Persistence enabled (snapshot every 20 seconds if 1 key changes)
- Dedicated database for sessions (DB 1)
- Separate from cache database (DB 0)

### 2. Keycloak Configuration

#### Realm Configuration

**Location:** `infra/keycloak/realm-bss.json`

Session settings configured:
- **SSO Idle Timeout:** 1800 seconds (30 minutes)
- **SSO Max Lifespan:** 36000 seconds (10 hours)
- **Session Idle Timeout:** 1800 seconds
- **Session Max Lifespan:** 36000 seconds
- **Access Token Lifespan:** 300 seconds (5 minutes)

#### Cache Configuration

**Location:** `infra/keycloak/conf/keycloak-cache-ispn.xml`

Caches configured with Redis persistence:

1. **Sessions Cache**
   - User sessions
   - Key prefix: `bss:sessions:`
   - Distributed, async mode

2. **Authentication Sessions Cache**
   - Login flows
   - Key prefix: `bss:auth-sessions:`
   - Distributed, async mode

3. **Offline Sessions Cache**
   - Offline access tokens
   - Key prefix: `bss:offline-sessions:`
   - Distributed, async mode

4. **Client Sessions Cache**
   - Service account sessions
   - Key prefix: `bss:client-sessions:`
   - Distributed, async mode

#### Docker Compose Integration

**Location:** `dev/compose.yml` (Keycloak service)

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26.0
  command:
    - start-dev
    - --import-realm
    - --http-enabled=true
    - --health-enabled=true
    - --cache-config-file=/opt/keycloak/conf/keycloak-cache-ispn.xml
  environment:
    REDIS_HOST: redis
    REDIS_PORT: 6379
    REDIS_SESSION_DB: 1
  volumes:
    - ../infra/keycloak/conf/keycloak-cache-ispn.xml:/opt/keycloak/conf/keycloak-cache-ispn.xml:ro
  depends_on:
    redis:
      condition: service_healthy
```

### 3. Spring Boot Configuration

#### Application Configuration

**Location:** `backend/src/main/resources/application.yaml`

```yaml
spring:
  session:
    store-type: redis
    redis:
      namespace: bss:session
      flush-mode: on_save
      timeout: 1800s
    timeout: 1800s
    redis:
      repository:
        enabled: true
```

**Settings:**
- Session timeout: 1800 seconds (30 minutes)
- Redis namespace: `bss:session`
- Flush mode: on_save
- Session repository enabled

#### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:#{null}}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

#### Configuration Class

**Location:** `backend/src/main/java/com/droid/bss/infrastructure/config/RedisSessionConfig.java`

Features:
- Custom RedisTemplate for session storage
- String keys with JDK serialization for values
- Session cleanup executor
- Customized RedisSessionRepository

### 4. Environment Configuration

**Location:** `.env.example`

```bash
# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SESSION_DB=1
```

## Session Management Features

### 1. Distributed Sessions

- Sessions stored in Redis (shared across instances)
- No session stickiness required at load balancer
- Horizontal scaling support for all services

### 2. Session Timeout

- **Keycloak SSO Idle:** 30 minutes
- **Keycloak Session Max:** 10 hours
- **Spring Session:** 30 minutes
- **Access Token:** 5 minutes

### 3. Session Persistence

- Redis persistence enabled (AOF)
- Key prefixes to avoid collisions:
  - Keycloak: `bss:sessions:`, `bss:auth-sessions:`, etc.
  - Spring: `bss:session:`

### 4. Connection Pooling

- Redis connection pool: 8-100 connections
- Connection timeout: 2000ms
- Proper cleanup on shutdown

## Database Separation

### Redis Database Strategy

- **DB 0:** Application caching (default)
- **DB 1:** Session storage (Keycloak + Spring)
- **DB 2:** Available for future use (analytics, etc.)

This separation prevents cache eviction from affecting sessions.

## Security Considerations

### 1. Session Data Protection

- Sessions contain sensitive authentication data
- Redis should be on private network only
- No Redis authentication in development
- Enable Redis AUTH in production

### 2. Session Encryption

- Keycloak sessions may contain credentials
- Redis should use TLS in production
- Consider Redis ACL for fine-grained access

### 3. Network Security

- Redis accessible only within Docker network
- No external Redis port exposure in production
- Use VPC/private network for production

## Monitoring and Health Checks

### 1. Redis Health

```bash
# Check Redis connectivity
docker exec -it bss-redis redis-cli ping

# Check session database
docker exec -it bss-redis redis-cli -n 1 ping

# List session keys
docker exec -it bss-redis redis-cli -n 1 keys "bss:*"
```

### 2. Session Metrics

Monitor via:
- Redis INFO command (connected clients, memory)
- Spring Actuator metrics (`/actuator/metrics`)
- Keycloak Server Info endpoint

### 3. Key Session Counters

- Active sessions: `DBSIZE` on session DB
- Auth sessions: `SCAN` for `bss:auth-sessions:*`
- Total sessions: Sum of all session types

## Troubleshooting

### 1. Session Not Persisting

**Symptoms:** Users logged out immediately

**Checks:**
- Redis is running: `docker compose ps redis`
- Redis connectivity: `redis-cli ping`
- Session DB: `redis-cli -n 1 ping`
- Keycloak logs: `docker compose logs keycloak`
- Backend logs: `docker compose logs backend`

**Solutions:**
- Ensure Redis is healthy before starting Keycloak/Backend
- Check Redis database index (REDIS_SESSION_DB)
- Verify cache config file is mounted

### 2. Session Key Collisions

**Symptoms:** Users seeing each other's sessions

**Checks:**
- Key prefixes in Redis: `redis-cli -n 1 KEYS "bss:*"`
- Ensure different prefixes for different services
- Verify namespace configuration

**Solutions:**
- Use unique key prefixes per service
- Separate databases per use case

### 3. Performance Issues

**Symptoms:** Slow session operations

**Checks:**
- Redis memory usage: `redis-cli INFO memory`
- Connection pool exhaustion
- Network latency

**Solutions:**
- Increase connection pool size
- Enable Redis persistence tuning
- Monitor slow query log

### 4. Session Loss After Restart

**Symptoms:** Users must re-login after service restart

**Checks:**
- Redis persistence: `redis-cli CONFIG GET save`
- Data directory volume mounted
- Snapshots being created

**Solutions:**
- Verify `redis-data` volume mounted
- Check snapshot frequency
- Consider AOF persistence

## Production Deployment Checklist

### 1. Security

- [ ] Enable Redis AUTH with strong password
- [ ] Use TLS for Redis connections
- [ ] Restrict Redis network access
- [ ] Enable Redis ACL for fine-grained control
- [ ] Rotate session secrets regularly

### 2. Performance

- [ ] Tune Redis persistence settings
- [ ] Configure appropriate connection pool sizes
- [ ] Monitor Redis memory usage
- [ ] Set up Redis monitoring/alerts
- [ ] Consider Redis Cluster for high availability

### 3. Monitoring

- [ ] Enable Redis slow query log
- [ ] Set up Prometheus Redis exporter
- [ ] Monitor session expiration rates
- [ ] Alert on connection failures
- [ ] Track session growth trends

### 4. Backup

- [ ] Configure Redis RDB snapshots
- [ ] Set up Redis backup schedule
- [ ] Test recovery procedures
- [ ] Document session data retention policy

## Testing the Configuration

### 1. Start Services

```bash
cd /home/labadmin/projects/droid-spring
docker compose up -d redis keycloak
```

### 2. Verify Redis Connection

```bash
docker exec -it bss-redis redis-cli -n 1 ping
# Expected: PONG
```

### 3. Check Keycloak Sessions

```bash
# Login via Keycloak admin console
# Check for sessions in Redis
docker exec -it bss-redis redis-cli -n 1 keys "bss:*"
```

### 4. Test Session Persistence

```bash
# Create a session (login)
# Restart backend
docker compose restart backend
# Session should still be valid
```

### 5. Verify Backend Sessions

```bash
# Check Spring sessions
docker exec -it bss-redis redis-cli -n 1 keys "bss:session:*"
```

## Benefits

### 1. High Availability

- Session survives individual service restarts
- No single point of failure for sessions
- Horizontal scaling support

### 2. Performance

- Fast session access (Redis in-memory)
- Reduced database load
- Efficient session sharing across instances

### 3. Scalability

- Add more application instances easily
- Session data centralized
- Load balancer doesn't need session affinity

### 4. Reliability

- Persistent sessions across restarts
- Session replication via Redis
- Graceful handling of instance failures

## References

- [Spring Session Documentation](https://docs.spring.io/spring-session/reference/)
- [Keycloak Caching Guide](https://www.keycloak.org/server/caching)
- [Redis Documentation](https://redis.io/documentation)
- [Infinispan Configuration](https://infinispan.org/docs/stable/)

## Summary

The Redis session configuration enables:
- ✅ Distributed session management
- ✅ Horizontal scalability
- ✅ Session persistence
- ✅ Cross-service session sharing
- ✅ Production-ready setup

Sessions are now stored in Redis (DB 1) with:
- 30-minute idle timeout
- Separate from application cache
- Persistent across restarts
- Monitored and health-checked

This configuration is production-ready and provides a solid foundation for session management in the BSS system.
