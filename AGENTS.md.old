# AGENTS.md

## Purpose and scope
A single source of truth for agents/droids and humans. Defines build/test/run, architecture rules, security, observability, eventing, CI/CD, and PR acceptance. Stack:
- Backend: Spring Boot 3.4 on Java 21 (LTS, Virtual Threads)
- Frontend: Nuxt 3 + TypeScript + pnpm
- Services: PostgreSQL 18, Kafka (CloudEvents v1.0), Redis, Keycloak 26 (OIDC)
- Testing: Maven + JUnit, Testcontainers with @ServiceConnection, Vitest, Playwright
- Errors: RFC 7807 (ProblemDetail)
- Observability: Spring Actuator + Micrometer Tracing (OTLP)

## Build, test, run

- Backend (Java 21 + Maven)
  - Build: `mvn -q -DskipTests clean package`
  - Tests: `mvn -q -DskipTests=false verify`
  - Dev: `mvn spring-boot:run`
  - Virtual threads: `spring.threads.virtual.enabled=true` (dev/prod)

- Frontend (Nuxt + TypeScript + pnpm)
  - Install: `pnpm install --frozen-lockfile`
  - Type-check: `pnpm run typecheck`
  - Unit: `pnpm run test:unit -- --run --no-color`
  - E2E: `pnpm run test:e2e`
  - Dev: `pnpm run dev`

- Local services (Docker Compose)
  - `docker compose -f dev/compose.yml up -d postgres redis kafka keycloak`
  - Health: containers healthy; backend starts; frontend serves UI

- Testcontainers (integration)
  - Dependencies via Testcontainers + `@ServiceConnection`; no manual URLs in tests

Global DoD
- Backend verify green (when tests are planned); frontend typecheck + unit + e2e green; compose healthy; Actuator `health/metrics/traces` live.

## Monorepo layout
- `backend/`, `frontend/`, `infra/` (compose, keycloak), `docs/`, `.factory/droids/`

## Droid roles and responsibilities

- scrum-master: planning/backlog/DoR/DoD/tracking/retro; artifacts under `docs/sprint-*`
- architect-techlead: SPEC mode only; `docs/architecture.md`, ADRs, contracts, NFRs; no feature coding
- backend-dev: backend implementation (DDD, API, migrations, integrations); tests only when approved by mentor-reviewer
- frontend-dev: frontend implementation (Nuxt + OIDC); tests only when approved by mentor-reviewer
- mentor-reviewer (tutor): teaches testing strategy; decides what/where/why to test and when to implement; reviews code
- devops-engineer: CI/CD, infra-as-code, deployments, observability
- educator-scribe: architecture education, docs/diagrams, onboarding; supports mentor with architectural testing context

Testing coordination (critical)
- Implementation droids (backend-dev, frontend-dev) produce application code and, if required, only empty test scaffolding (`@Disabled`, `test.todo()`).
- Full test implementations happen only:
  1) after mentor-reviewer approves a test plan, or
  2) upon explicit developer request for this task.

## Java 21 standards
- JEP 444 Virtual Threads: prefer `VirtualThreadPerTaskExecutor` for blocking I/O; avoid long `synchronized` in hot paths.
- JEP 441 switch pattern matching: prefer over cascaded if-else; be exhaustive and null-safe.
- JEP 440 record patterns: use `record` DTOs; pattern deconstruction where helpful.
- JEP 431 Sequenced Collections: use when order matters.
- Preview features disabled in prod unless explicitly approved.

## Spring Boot 3.4 principles
- Config: externalized; profiles `dev/test/prod`; `@ConfigurationProperties` (constructor) + `@Validated`; no secrets in repo.
- Errors: centralized `@ControllerAdvice` with `ProblemDetail` (RFC 7807) including `code/title/detail` and `traceId`.
- Persistence: public APIs return `Page/Slice` + explicit sorting; avoid unbounded collections; fix N+1 via projections, `@EntityGraph`, fetch joins; consider `hibernate.default_batch_fetch_size`.
- Messaging: Kafka with retry/backoff + DLT; metrics for success/failure latency.
- Cache: Spring Cache + Redis with TTL per cache, deterministic keys, and invalidation strategy (domain events/pub-sub).
- Observability: Actuator + OTLP tracing (gRPC preferred); structured logs (ECS/Logstash/GELF); application grouping.

## API and contracts
- OpenAPI via springdoc; update on every API change; publish spec as CI artifact.
- Pagination/sorting mapped; max page size enforced; explicit sorting required.
- All 4xx/5xx map to ProblemDetail consistently.

## CloudEvents (Kafka)
- v1.0 JSON; required: `id` (UUIDv4), `source` (URN per bounded context), `type` (namespaced+versioned, e.g., `com.example.orders.order.created.v1`), `specversion=1.0`, `time` (RFC3339), `datacontenttype=application/json`, `data`, optional `subject`.
- Kafka binary mode: `ce_*` headers + JSON payload; partition key = entity id.
- Publication after commit: `@TransactionalEventListener(AFTER_COMMIT)`; outbox for DB→Kafka consistency when needed.
- Consumers idempotent (dedupe by `ce_id`); retry/backoff + DLT; metrics; trace context propagation (`traceparent`/`b3`).
- Schemas: JSON Schemas under `docs/events/...`; samples under `docs/events/samples/`; validate in tests.

## CQRS (optional)
- Command side (write model):
  - Packages: `api.command`, `application.command`, `domain`, `infrastructure.write`
  - Controllers return 201/202; transactions in Application; events published AFTER_COMMIT.
- Query side (read model):
  - Packages: `api.query`, `application.query`, `infrastructure.read`, `infrastructure.projection`
  - Projections from Kafka events populate read tables or Redis.
- Read model consistency SLA: document (e.g., < 2s).
- Projection handlers must be idempotent and tested with Testcontainers.
- Metrics: expose lag between write and read model (gauge).

## Hexagonal (Ports & Adapters) guidance
- Use inbound/outbound ports in application layer; adapters in infrastructure.
- Controllers/consumers (inbound adapters) call use cases; repositories/producers (outbound adapters) implement ports.
- Enforce dependency inversion: domain/app depend on ports; infra depends on domain/app.

## Tests: strategy and layers
- Unit: pure domain logic; fast, no framework.
- Slice: `@WebMvcTest` for controllers (ProblemDetail, validation); `@DataJpaTest` for repositories (mapping, queries).
- Integration: Testcontainers Postgres/Kafka/Redis with `@ServiceConnection`; run Flyway migrations before assertions.
- Frontend: Vitest (unit), Playwright (E2E) with `data-testid` selectors.
- CloudEvents: assert `ce_*` headers, payload schema, partitioning, idempotency, retry/DLT.
- Observability smoke: presence of `health/metrics/traces` in envs used by tests.
- Implementation timing: per mentor-reviewer plan.

## CI/CD (quality gates)
- Backend: build + tests (when planned), OpenAPI artifact, container image, scans.
- Frontend: typecheck + unit + e2e, artifact/image.
- Contracts: OpenAPI published; event schemas validated; samples stored.
- Infra: Compose in CI for deps; no reliance on external shared services.
- Deploy: rolling updates; DB migrations via init container/job; post-deploy smoke.

## Security and OIDC (Keycloak 26)
- Realm/clients/scopes/roles under `infra/keycloak` for import/export.
- Backend resources protected with roles/scopes; integration tests mock JWKS/token.
- Secrets via env/secret stores; never commit secrets.

## PR checklist
- OpenAPI current; ProblemDetail consistent; pagination/sorting enforced.
- CloudEvents headers correct; JSON Schemas validated; samples updated.
- Code compiles; app starts; Actuator/OTLP live; no regressions.
- Virtual Threads enabled; no long synchronized sections in hot paths.
- Externalized config; DB migrations included; no secrets.
- Tests: plan approved by mentor-reviewer; implemented per plan (if in scope); artifacts available.
- Docs: educator-scribe updated architecture docs/diagrams if structure changed.

## Commands (reference)
- Deps up: `docker compose -f dev/compose.yml up -d postgres redis kafka keycloak`
- Backend: `mvn -DskipTests=false verify` (when tests are planned), `mvn spring-boot:run`
- Frontend: `pnpm install --frozen-lockfile && pnpm run typecheck && pnpm run dev`
- E2E: `pnpm run test:e2e`

## Droid CLI notes
- Keep AGENTS.md at repo root (auto-detected).
- Custom roles in `.factory/droids/*.md` (names from YAML frontmatter).
- Use SPEC mode (planning) before edits; builders follow this file for commands/DoD.

## Operational notes
- Group logs/metrics by `spring.application.group`.
- TLS/SSL: validate bundle reload and ciphers in lower envs before prod.
- Monitor Virtual Threads and carrier threads; tune `-Djdk.virtualThreadScheduler.parallelism` only if needed.

## Reference versions
- Java 21 LTS; Spring Boot 3.4.x; PostgreSQL 18; Redis 7; Kafka 3.x (KRaft); Keycloak 26; Nuxt 3; Node 22.21.0 LTS; pnpm 10.15.x
