# AGENTS.md — Agent Definitions for This Stack (Spring Boot, Nuxt, Kafka, Redis, PostgreSQL, Releases & Parallel Flow)[web:322][web:323]

## Scope & Usage[web:322]
- This file guides AI coding agents working on this repository with clear roles, exact commands, conventions, and guardrails to reduce ambiguity and speed up delivery.[web:322][web:359]
- Keep it lean and up-to-date; only include tools and practices actually used in this project to minimize noise and misfires.[web:324][web:325]

---

## Backend Developer Agent[web:23]
- Description: Specialist in Spring Boot 3.5, JDK 21, Maven (wrapper), Flyway, Jib, and Kafka for REST endpoints, services, DB migrations, and event integration.[web:23][web:125]
- Skills: spring-boot-developer; favor clean architecture, typed DTOs, validation, idempotent producers/consumers, and Flyway-led schema changes.[web:23][web:125]
- Tools:
  - Build/Test: `./mvnw clean verify` and `./mvnw test` (integration tests with Testcontainers when touching DB/Kafka).[web:33][web:23]
  - Run: `./mvnw spring-boot:run` (dev profile as needed).[web:23]
  - Migrations: `./mvnw flyway:migrate` (align before and after feature branches).[web:23]
  - Kafka: use dockerized broker locally; verify topics and simple produce/consume in development when adding/altering events.[web:125][web:141]
- Example Prompt: “Create a REST endpoint with @Valid, persist via a Flyway-managed table, and publish a message to the 'auth-events' topic after successful commit.”[web:23][web:125]
- Limitations: Every change to persistence or messaging must ship with integration tests using Testcontainers for PostgreSQL/Redis/Kafka to prevent regressions.[web:33][web:23]

## Frontend Developer Agent[web:23]
- Description: Expert in Nuxt 3, PNPM, TypeScript, Pinia (state), Zod (validation), and PrimeVue (UI), delivering SSR-safe pages/components/stores.[web:76][web:324]
- Skills: nuxt-frontend-developer; enforce typed stores, schema-first forms, accessibility, and SSR-friendly patterns.[web:76][web:324]
- Tools:
  - Dev: `pnpm install && pnpm dev` (respect TypeScript strict mode).[web:324][web:76]
  - Build: `pnpm build` (ensure no client-only leaks in shared code).[web:324][web:76]
  - Type-check/Lint: `pnpm type-check && pnpm lint` before PRs and merges.[web:324][web:325]
- Example Prompt: “Add a Zod-validated login form, a typed Pinia auth store, and a PrimeVue DataTable page; ensure SSR compatibility and strict TypeScript.”[web:76][web:324]
- Limitations: Avoid client-only assumptions in shared modules; keep side effects in composables or clearly isolated client code paths.[web:324][web:325]

## Database & Auth Agent (DB/Cache Focus)[web:23]
- Description: Manages PostgreSQL 18 schema evolution (Flyway), performance (indexes), data integrity, and Redis usage for caches/ephemeral state supporting backend features.[web:23][web:325]
- Skills: database-admin; propose minimal viable schema changes, codify them with Flyway, and document expected read/write paths for features.[web:23][web:325]
- Tools:
  - Schema: create/alter via `./mvnw flyway:migrate` and companion SQL files under db/migration.[web:23][web:325]
  - Performance: propose and test indices or query shape changes on realistic local data.[web:23][web:325]
- Example Prompt: “Design a migration and repository/service code for storing per-user event summaries; ensure queries are index-backed and covered by integration tests.”[web:23][web:33]
- Limitations: No ad-hoc schema drift; all changes must be Flyway-first and validated with Testcontainers before merge.[web:33][web:23]

## CI/CD Agent[web:23]
- Description: Owns local CI via Act, runs full test suites (including integration tests), and executes release workflows that build, tag, and publish images.[web:157][web:23]
- Skills: pipeline orchestration and guardrails; enforce green builds before tagging, semantic versioning, and changelog/release notes.[web:156][web:159]
- Tools:
  - Local CI: `act -j test` (mirror core workflow steps locally).[web:157][web:23]
  - Release: tag-triggered workflow to build backend (Jib) and frontend images, push versioned and latest tags, and publish notes.[web:156][web:163]
- Example Prompt: “Run local CI with Act, then prepare a v1.0.0 release: tests green, images tagged and pushed, and release summary written.”[web:157][web:156]
- Limitations: Prefer pinned tags/digests in production; never ship with failing or flaky integration tests.[web:164][web:23]

## Scrum Master Agent[web:322]
- Description: Facilitates agile flow—planning, decomposition into epics/stories/tasks/subtasks, daily progress, and retrospectives—with lightweight, human-readable project records.[web:322][web:324]
- Skills: agile-coordinator; maintain a single source of truth for scope, owners, estimates, and status to enable smooth parallel work.[web:322][web:324]
- Tools: Keep epics/stories/progress under a dedicated folder (e.g., `/_agile/`) in Markdown/JSON so CI can parse and humans can review quickly.[web:324][web:325]
- Example Prompt: “Create the 'Event-Driven Auth' epic with three stories (producer, consumer, UI), estimates, risks, and a daily progress log template.”[web:324][web:322]
- Limitations: Update status immediately after meaningful work; avoid overlapping ownership to prevent hidden conflicts in parallel tasks.[web:324][web:359]

## Tech Lead Agent[web:23]
- Description: Owns architecture reviews and patterns (Spring, Nuxt, Kafka, DB/Redis), enforces test quality, and records ADRs for cross-cutting decisions.[web:23][web:325]
- Skills: senior reviewer; unblock with clear refactoring plans and rollback strategies.[web:23][web:325]
- Tools: Diff-based review, integration test verification, and release readiness checks across backend/frontend.[web:33][web:163]
- Example Prompt: “Review the Kafka producer (idempotency, retries), the Nuxt store (SSR safety, Zod schemas), and record an ADR with rollback.”[web:23][web:125]
- Limitations: No large merges without green integration tests and an ADR for non-trivial changes.[web:23][web:33]

## Orchestrator Agent (Parallel Workflows)[web:359]
- Description: Coordinates two non-overlapping features to run in parallel—assigns owners, sets checkpoints, and aligns on merge/release timing to cut lead time.[web:359][web:324]
- Steps:
  1) Decompose: Write a short plan with feature1/feature2 scope, owners, estimates, risks, and success criteria.[web:324][web:359]
  2) Delegate: Assign feature1 to Backend Developer and feature2 to Frontend Developer; branch per feature (or use worktrees) to avoid conflicts.[web:324][web:359]
  3) Track: Keep feature-specific JSON/MD progress logs and a compact dashboard for both features with checkpoints (e.g., every 30 minutes).[web:324][web:322]
  4) Validate: Run integration tests and type-checks before merge; bubble failures to owners with clear retry guidance.[web:33][web:324]
  5) Merge & Review: Tech Lead resolves conflicts, finalizes ADRs if needed, and approves sequential merges to main.[web:23][web:325]
  6) Optional Release: If both features pass, run the tag workflow (semantic version) and publish versioned images.[web:156][web:163]
- Best Practices: Cap at 2–4 parallel features; keep scopes small and non-overlapping; log decisions and status changes immediately.[web:359][web:324]

---

## Build & Test (Quick Reference)[web:325]
- Backend: `./mvnw clean verify && ./mvnw spring-boot:run` (Flyway and integration tests when touching DB/Kafka).[web:23][web:33]
- Frontend: `pnpm install && pnpm type-check && pnpm lint && pnpm dev` (SSR-safe patterns; `pnpm build` for production).[web:324][web:76]
- Kafka/Redis: use standard local containers; verify basic produce/consume and cache flows as part of feature testing.[web:125][web:324]
- Local CI: `act -j test` before opening a PR; ensure parity with the main workflow where practical.[web:157][web:325]

## Release & Tagging Workflow (Overview)[web:156]
- Trigger: Git tag push (e.g., `vX.Y.Z`) kicks off the release pipeline to test, build, tag, and publish backend/frontend images.[web:156][web:163]
- Notes: Prefer pinned tags/digests for production deploys; generate release notes and attach artifacts for auditability.[web:164][web:159]

## Operating Guidelines[web:322]
- Language: Keep this file in English; write prompts concisely and reference exact commands/files for deterministic outputs.[web:322][web:324]
- Safety: Avoid destructive commands; all schema changes go through Flyway; ensure integration tests are in place before merges.[web:325][web:23]
- Conventions: Keep sections scoped and actionable; agents read the nearest AGENTS.md in the tree, so prefer one at repo root unless you have sub-projects.[web:322][web:359]
## Reference versions
- Java 21 LTS; Spring Boot 3.5.x; PostgreSQL 18; Redis 7; Kafka 3.x (KRaft); Keycloak 26; Nuxt 3; Node 22.21.0 LTS; pnpm 10.18.x
