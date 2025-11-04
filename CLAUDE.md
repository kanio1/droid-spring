# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a **BSS (Business Support System)** monorepo with a Spring Boot 3.4 backend (Java 21) and Nuxt 3 frontend. The system implements customer management with Hexagonal architecture, CQRS patterns, and CloudEvents for Kafka messaging.

**Architecture highlights:**
- **Backend**: Spring Boot 3.4, Java 21 (Virtual Threads), PostgreSQL 18, Redis, Kafka, Keycloak 26 (OIDC)
- **Frontend**: Nuxt 3 + TypeScript + pnpm
- **Patterns**: Hexagonal (Ports & Adapters), CQRS (optional), CloudEvents v1.0
- **Observability**: Spring Actuator + Micrometer Tracing (OTLP)
- **Testing**: Maven + JUnit, Testcontainers, Vitest, Playwright

## Common Commands

### Backend (Java 21 + Maven)

```bash
# Build
mvn -q -DskipTests clean package

# Run tests
mvn -q -DskipTests=false verify

# Run in dev mode
mvn spring-boot:run

# Single test
mvn test -Dtest=CustomerControllerTest

# Build Docker image
docker build -t bss-backend:latest backend/
```

### Frontend (Nuxt + TypeScript + pnpm)

```bash
# Install dependencies
pnpm install --frozen-lockfile

# Type-check
pnpm run typecheck

# Lint
pnpm run lint

# Run unit tests
pnpm run test:unit -- --run --no-color

# Run E2E tests
pnpm run test:e2e

# Dev server
pnpm run dev

# Build for production
pnpm run build
```

### Local Development Environment

```bash
# Start all services (PostgreSQL, Redis, Kafka, Keycloak, Backend, Frontend, Caddy)
docker compose -f dev/compose.yml up -d

# Check service health
docker compose -f dev/compose.yml ps

# View logs
docker compose -f dev/compose.yml logs -f backend

# Stop all services
docker compose -f dev/compose.yml down
```

### Environment Setup

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your secrets (PostgreSQL, Keycloak, etc.)
```

## High-Level Architecture

### Backend Structure (`backend/src/main/java/com/droid/bss/`)

```
├── api/                    # REST controllers (inbound adapters)
│   ├── customer/
│   │   └── CustomerController.java
│   └── HelloController.java
├── application/            # Application services & use cases
│   ├── command/customer/  # CQRS command side
│   ├── query/customer/    # CQRS query side
│   └── dto/               # Data transfer objects
├── domain/                # Domain entities & repositories
│   └── customer/          # Customer aggregate root
│       ├── Customer.java
│       ├── CustomerEntity.java (JPA)
│       └── CustomerRepository.java (port)
└── infrastructure/        # External adapters
    ├── write/             # Write-side implementations
    ├── read/              # Read-side implementations
    ├── exception/         # GlobalExceptionHandler
    └── security/          # SecurityConfig
```

**Key architectural patterns:**

1. **Hexagonal Architecture**: Domain-driven design with clear separation between application, domain, and infrastructure layers. Controllers (inbound adapters) call use cases; repositories/producers (outbound adapters) implement ports.

2. **CQRS** (optional):
   - Command side: `api.command`, `application.command`, `domain`, `infrastructure.write`
   - Query side: `api.query`, `application.query`, `infrastructure.read`, `infrastructure.projection`
   - Events published via Kafka CloudEvents AFTER_COMMIT

3. **CloudEvents (Kafka)**:
   - Format: v1.0 JSON with `ce_*` headers
   - Schema: `id` (UUIDv4), `source` (URN), `type` (namespaced+versioned), `specversion=1.0`
   - Consumers must be idempotent (dedupe by `ce_id`)

4. **Error Handling**: Centralized `@ControllerAdvice` using RFC 7807 `ProblemDetail` with `code/title/detail/traceId`

5. **Virtual Threads**: Java 21 `VirtualThreadPerTaskExecutor` for blocking I/O; enabled via `spring.threads.virtual.enabled=true`

### Frontend Structure (`frontend/app/`)

```
├── app/
│   ├── components/common/  # Reusable UI components
│   ├── pages/             # Route pages
│   ├── layouts/           # Layout components
│   ├── middleware/        # Global auth middleware
│   ├── plugins/           # Keycloak client plugin
│   └── types/             # TypeScript type definitions
├── tests/                 # Vitest unit tests
└── Dockerfile
```

**Frontend architecture:**
- **Nuxt 3** with TypeScript
- **Keycloak JS** for OIDC authentication
- **Global auth middleware** (`middleware/auth.global.ts`)
- **Component-based** architecture with reusable UI components
- **API integration** via Nuxt `$fetch` with Keycloak token injection

### Infrastructure (`dev/`, `infra/`)

```
dev/
├── compose.yml            # Main Docker Compose with all services
├── caddy/                 # Reverse proxy (ports 8085/8443)
infra/
└── keycloak/
    └── realm-bss.json     # Keycloak realm configuration
```

**Services:**
- **PostgreSQL 18**: Primary database (port 5432)
- **Redis 7**: Caching and session store (port 6379)
- **Keycloak 26**: OIDC identity provider (port 8081)
- **Caddy**: TLS termination and reverse proxy (ports 8085/8443)

## Testing Strategy

### Backend Testing Layers

1. **Unit tests**: Pure domain logic, no framework
2. **Slice tests**: `@WebMvcTest` (controllers), `@DataJpaTest` (repositories)
3. **Integration tests**: Testcontainers with `@ServiceConnection` for Postgres/Kafka/Redis

### Frontend Testing

- **Unit**: Vitest with Vue Test Utils
- **E2E**: Playwright with `data-testid` selectors

**Critical note**: Implementation droids should only produce empty test scaffolding (`@Disabled`, `test.todo()`). Full test implementations require mentor-reviewer approval.

## Configuration & Profiles

- **Spring profiles**: `dev`, `test`, `prod`
- **Externalized config**: `@ConfigurationProperties` with `@Validated`
- **No secrets in repo**: All sensitive data via environment variables

### Key Properties

```yaml
# Virtual Threads (dev/prod)
spring.threads.virtual.enabled: true

# Actuator endpoints
management.endpoints.web.exposure.include: health,metrics,traces

# OpenTelemetry OTLP
management.otlp.tracing.endpoint: http://tempo:4317
```

## Documentation

- **AGENTS.md**: Complete development guidelines, architecture rules, and droid roles
- **OpenAPI spec**: Auto-generated from Spring controllers; published as CI artifact
- **Event schemas**: JSON Schemas under `docs/events/` with samples

## Common Development Tasks

### Add a new API endpoint

1. Create DTO in `application/dto/`
2. Add controller in `api/` (follow existing patterns)
3. Add use case in `application/command/` or `application/query/`
4. Update OpenAPI spec (auto-generated)
5. Add integration test with Testcontainers

### Add a new domain entity

1. Create entity in `domain/` following existing pattern
2. Create repository interface (port)
3. Create repository implementation in `infrastructure/write/`
4. Add JPA entity mapping
5. Create database migration (Flyway)

### Running specific tests

```bash
# Backend - specific test class
mvn test -Dtest=CustomerServiceTest

# Backend - specific test method
mvn test -Dtest=CustomerServiceTest#shouldCreateCustomer

# Frontend - specific test file
pnpm run test:unit -- customer.test.ts

# Run with coverage
mvn verify -Djacoco.skip=false
```

## Important Resources

- **AGENTS.md**: Comprehensive development guidelines and patterns
- **`.env.example`**: Environment variable template
- **`dev/compose.yml`**: Service orchestration and configuration
- **Test execution**: All integration tests use Testcontainers (no external dependencies)

## Security Notes

- **OIDC Authentication**: Keycloak realm configuration in `infra/keycloak/realm-bss.json`
- **Backend security**: OAuth2 resource server with JWT validation
- **Frontend**: Keycloak JS for token management
- **Never commit secrets**: Use `.env` with proper values in development
