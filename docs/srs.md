# Software Requirements Specification
## For Event-Driven Order Platform

Version 1
Prepared by KM
Personal Portfolio Project
September 10, 2026

## Table of Contents
<!-- TOC -->
* [1. Introduction](#1-introduction)
  * [1.1 Document Purpose](#11-document-purpose)
  * [1.2 Product Scope](#12-product-scope)
  * [1.3 Definitions, Acronyms, and Abbreviations](#13-definitions-acronyms-and-abbreviations)
  * [1.4 References](#14-references)
  * [1.5 Document Overview](#15-document-overview)
* [2. Product Overview](#2-product-overview)
  * [2.1 Product Perspective](#21-product-perspective)
  * [2.2 Product Functions](#22-product-functions)
  * [2.3 Product Constraints](#23-product-constraints)
  * [2.4 User Characteristics](#24-user-characteristics)
  * [2.5 Assumptions and Dependencies](#25-assumptions-and-dependencies)
  * [2.6 Apportioning of Requirements](#26-apportioning-of-requirements)
* [3. Requirements](#3-requirements)
  * [3.1 External Interfaces](#31-external-interfaces)
  * [3.2 Functional](#32-functional)
  * [3.3 Quality of Service](#33-quality-of-service)
  * [3.4 Compliance](#34-compliance)
  * [3.5 Design and Implementation](#35-design-and-implementation)
  * [3.6 AI/ML](#36-aiml)
* [4. Verification](#4-verification)
* [5. Appendixes](#5-appendixes)
<!-- TOC -->

## Revision History

| Name | Date | Reason For Changes | Version |
|------|------|--------------------|---------|
| KM | 2026-09-10 | Initial baseline, derived from the Master Context and the requirements discovery process | 0.1 |
| KM | 2026-10-04 | final | 1 |

## 1. Introduction

This document specifies the requirements of the Event-Driven Order Platform: what the system must do and which properties it must satisfy. Section 2 provides context and scope; Section 3 contains the verifiable requirements; Section 4 defines how each one will be verified; Section 5 contains non-normative supporting material.

### 1.1 Document Purpose

This SRS specifies the functional and quality requirements of the Event-Driven Order Platform, a reference backend that demonstrates asynchronous communication between services using Apache Kafka, eventual consistency, the Transactional Outbox pattern, and idempotent consumers. Its primary audience is the author — acting as Software Engineer during design and implementation — and any technical reviewer (interviewer, portfolio reader) evaluating the engineering rigor of the project. The document defines **what** the system must do, not **how** to implement it.

### 1.2 Product Scope

**Event-Driven Order Platform**, version v1, single release. Its purpose is to process order creation and to asynchronously coordinate the associated inventory reservation, maintaining eventual consistency between `order-service` and `inventory-service` through events published to Kafka using the Transactional Outbox pattern.

It includes:
- Order creation and retrieval.
- API client authentication using self-issued JWTs.
- Emission and consumption of domain events (`OrderCreated`, `InventoryReserved`, `InventoryRejected`).
- Idempotent event processing.

It explicitly excludes: a user interface, catalog/cart/checkout/payment management, end-user management, integration with an external identity provider (IdP/OAuth2), and any production SLA objective. The project does not aim to be a complete e-commerce platform; it aims to demonstrate event-driven architecture patterns within a bounded, zero-cost scope.

### 1.3 Definitions, Acronyms, and Abbreviations

| Term | Definition |
|------|------------|
| API | Application Programming Interface. |
| SRS | Software Requirements Specification. |
| UI | User Interface. |
| Outbox Pattern | A pattern that persists the domain event in the same database transaction as the state change, so that it can later be published asynchronously and reliably. |
| Eventual Consistency | A guarantee that, in the absence of new updates, all components will eventually converge to the same state, with no guarantee of immediacy. |
| Idempotent Consumer | An event consumer that produces the same business effect regardless of how many times it receives the same event. |
| DLT | Dead Letter Topic — the Kafka topic that receives messages whose processing has failed repeatedly. |
| JWT | JSON Web Token — a signed token used to authenticate API calls. |
| Client Credential | A seeded (not self-registrable) `client_id`/`client_secret` pair used to obtain a JWT. |
| Aggregate | A domain object identified by an ID (e.g., Order) whose state changes are communicated through events. |

### 1.4 References

| Title | Owner | Version | Location | Type |
|---|---|---|---|---|
| RFC 7807 — Problem Details for HTTP APIs | IETF | RFC 7807 | https://www.rfc-editor.org/rfc/rfc7807 | Normative (error format, see 3.1.3) |

### 1.5 Document Overview

Section 2 (Product Overview) provides context: product perspective, high-level functions, constraints, user characteristics, assumptions/dependencies, and the allocation of requirements to this release. Section 3 (Requirements) contains the verifiable requirements organized into External Interfaces, Functional, Quality of Service, Compliance, Design/Implementation, and AI/ML (not applicable). Section 4 (Verification) maps each requirement to its verification method and status. Section 5 (Appendixes) contains the reference architecture diagram, the data model, and the business rules. Requirement identifiers are unique and immutable; any change is versioned (`-[VER]`) and recorded in the Revision History.

## 2. Product Overview

### 2.1 Product Perspective

A **greenfield**, standalone project, with no prior system to replace and not part of a larger corporate ecosystem. It consists of two proprietary components (`order-service`, `inventory-service`) and two third-party, open-source infrastructure dependencies, self-hosted via Docker Compose (PostgreSQL, Apache Kafka). There are no contractual SLAs or formal support model, as this is an individual portfolio project. `notification-service` is identified as an optional future extension, explicitly outside the core of this version (see 2.6).

### 2.2 Product Functions

- Create an order (validating items and quantities).
- Retrieve an order by ID.
- Issue an authentication token (JWT) from client credentials.
- Reliably publish domain events via the Transactional Outbox.
- Reserve or reject inventory based on stock availability.
- Confirm or reject the order based on the outcome of the inventory reservation.
- Process events idempotently (tolerate duplicate deliveries).
- Recover from temporary Kafka failures without losing already-persisted orders.

The detailed behaviors, inputs/outputs, and error cases of each function are specified in 3.2.

### 2.3 Product Constraints

- The system shall be implemented in Java 21 with Spring Boot 4.
- The build shall be managed with Maven.
- Order and inventory persistence shall use PostgreSQL (single instance, no separate database per component).
- Communication between `order-service` and `inventory-service` shall be asynchronous, through events published to Apache Kafka.
- The system shall implement the Transactional Outbox pattern to publish events derived from state changes.
- Event consumers shall be idempotent.
- The entire environment shall be startable with Docker Compose using a single command.
- Integration tests shall run against real PostgreSQL and Kafka instances via Testcontainers.
- The system shall not use distributed transactions (2PC) between PostgreSQL and Kafka.
- The system shall not implement Event Sourcing.
- The system shall not depend on any paid service, tool, or tier (see 3.5.7).
- Authentication shall be implemented through JWTs self-issued by the backend itself, with no external identity provider.
- There is no production SLA objective (uptime, guaranteed throughput).

### 2.4 User Characteristics

Single actor: **API Client** — a system, script, or tool (e.g., a Postman/Swagger collection, a test suite, or another service) that interacts with the platform exclusively via REST, authenticated with a JWT Bearer token. There are no human end users with a graphical interface, nor different role or permission levels (there is no user management). Familiarity with REST APIs and Bearer token handling is assumed.

### 2.5 Assumptions and Dependencies

**Assumptions**

| ID | Assumption | Impact if false |
|---|---|---|
| ASM-001 | Single-developer project, with no formal operations team. | Low — affects only the process level (3.5.10), not the technical requirements. |
| ASM-002 | This specification corresponds to a single release (v1); no later phases are planned yet. | Medium — if phases are added, 2.6 must be updated and the SRS versioned. |
| ASM-004 | Kafka and PostgreSQL will be available and accessible in the execution environment (Docker Compose) at demo/execution time. | High — without them the full flow cannot be demonstrated; mitigated by the Outbox design, which tolerates *temporary* unavailability, not total unavailability. |

**Dependencies**

- Apache Kafka (single broker, no cluster).
- PostgreSQL (single instance).
- Open-source JWT library (e.g., `jjwt` or `nimbus-jose-jwt`).
- Testcontainers (PostgreSQL, Kafka) for integration tests.
- Docker and Docker Compose in the execution environment.
- GitHub Actions (free tier) for CI.

All dependencies are free/open-source, in line with the zero-cost constraint (2.3, 3.5.7).

### 2.6 Apportioning of Requirements

All requirements defined in this document correspond to the **single release v1**.

| Item | Status |
|---|---|
| All REQ-* defined in Section 3 | Included in v1 |
| `notification-service` | Deferred — outside the core of v1 |
| Inventory management API (CRUD) | Deferred — v1 uses only a seed/script (DEC-005) |
| Formal delivery deadline | TBD (see 3.5.8) |

## 3. Requirements

### 3.1 External Interfaces

#### 3.1.1 User Interfaces

**N/A.** There is no graphical interface or frontend within the scope of this project (see 1.2 and the Out-of-Scope section of the context document).

#### 3.1.2 Hardware Interfaces

**N/A.** The system is purely software; it does not interact with physical devices.

#### 3.1.3 Software Interfaces

**REST API — `order-service`**
- `POST /auth/token` — exchanges `client_id`/`client_secret` for a JWT. Does not require prior authentication.
- `POST /api/v1/orders` — creates an order. Requires `Authorization: Bearer <JWT>`.
- `GET /api/v1/orders/{id}` — retrieves an order. Requires `Authorization: Bearer <JWT>`.
- `Content-Type: application/json` on all endpoints.
- Error format: **RFC 7807 Problem Details** (`ProblemDetail`, native to Spring 6), extended with an `errors[]` array for field-level violations.

**Kafka**
- Topics: `order.created`, `inventory.reserved`, `inventory.rejected` (`domain.event` convention, lowercase).
- No Schema Registry (out of scope); plain JSON payload with a common envelope:
  ```json
  { "eventId": "uuid", "eventType": "OrderCreated", "occurredAt": "ISO-8601", "aggregateId": "orderId", "payload": { } }
  ```

**PostgreSQL**
- A single instance shared by `order-service` and `inventory-service`, with separate schemas/tables per domain (there is no database per component — see the Out-of-Scope section of the context document).

### 3.2 Functional

The following requirements are verifiable and implement, among others, business rules BR-001 through BR-018 (see Appendix C for the complete list).

#### 3.2.1 Order Creation (UC-01)

```markdown
- ID: REQ-FUNC-001
- Title: Reject malformed order creation requests
- Statement: The system shall reject an order creation request that does not include at least one item, responding with HTTP 400 and a structured error response.
- Rationale: Enforces BR-001 at the API boundary before domain processing.
- Acceptance Criteria: A POST with an empty or missing `items` array returns HTTP 400; no order or OutboxEvent is persisted.
- Verification Method: Test
- More Information: BR-001. Error format: RFC 7807 ProblemDetail with `errors[]` extension (see 3.1.3).
```

```markdown
- ID: REQ-FUNC-002
- Title: Reject items with non-positive quantity
- Statement: The system shall reject an order creation request containing any item with quantity <= 0, responding with HTTP 400.
- Rationale: Enforces BR-002.
- Acceptance Criteria: A request with at least one item having quantity 0 or negative returns HTTP 400; the order is not persisted.
- Verification Method: Test
- More Information: BR-002. Error format: RFC 7807 ProblemDetail with `errors[]` extension.
```

```markdown
- ID: REQ-FUNC-003
- Title: Persist new order in PENDING state
- Statement: Given a valid request, the system shall persist a new order with status PENDING, including its items (productId, quantity, unitPriceSnapshot) and a computed total.
- Rationale: BR-003; establishes the canonical initial state.
- Acceptance Criteria: After a valid POST, querying the order (REQ-FUNC-006) returns status PENDING, the submitted items, and total = Σ(quantity × unitPriceSnapshot).
- Verification Method: Test
```

```markdown
- ID: REQ-FUNC-004
- Title: Atomically persist order and OutboxEvent
- Statement: The system shall create an OutboxEvent of type OrderCreated and persist it together with the new order in the same PostgreSQL transaction.
- Rationale: BR-008; guarantees the event is never lost relative to the order.
- Acceptance Criteria: If the transaction fails, neither the order nor the OutboxEvent is persisted; if it succeeds, both exist in the same commit.
- Verification Method: Test (integration, Testcontainers)
- More Information: Related constraint: Transactional Outbox mandatory (2.3).
```

```markdown
- ID: REQ-FUNC-005
- Title: Respond with created order representation
- Statement: Upon successful order creation, the system shall respond 201 Created including the order identifier and its current state.
- Rationale: Enables client tracking (UC-02).
- Acceptance Criteria: Response status is 201; body contains orderId and status = PENDING.
- Verification Method: Test
```

#### 3.2.2 Order Query (UC-02)

```markdown
- ID: REQ-FUNC-006
- Title: Retrieve order by ID
- Statement: The system shall provide GET /api/v1/orders/{id} returning the order's current persisted state (PENDING, CONFIRMED, or REJECTED) and its items.
- Rationale: UC-02; allows observing eventual state.
- Acceptance Criteria: Existing ID returns 200 with the current status per PostgreSQL as the source of truth (BR-009). Non-existent ID returns 404.
- Verification Method: Test
```

#### 3.2.3 Authentication (UC-08)

```markdown
- ID: REQ-FUNC-007
- Title: Issue JWT for valid client credentials
- Statement: The system shall provide POST /auth/token that, given a valid client_id/client_secret, returns a signed JWT.
- Rationale: Enables authenticated access without an external IdP.
- Acceptance Criteria: Valid credentials return 200 with a JWT in the response body; invalid/unknown credentials return 401.
- Verification Method: Test
- More Information: Client credentials are seeded, not self-registrable (no user management, per Out-of-Scope).
```

```markdown
- ID: REQ-FUNC-008
- Title: Reject unauthenticated access to protected endpoints
- Statement: The system shall reject any request to /api/v1/orders/** lacking a valid, non-expired JWT, responding with 401.
- Rationale: BR-012.
- Acceptance Criteria: A request with a missing Authorization header, or with an invalid/expired token, receives 401 and is not processed further.
- Verification Method: Test
```

```markdown
- ID: REQ-FUNC-009
- Title: Enforce JWT expiration
- Statement: The system shall issue JWTs with a 30-minute expiration and reject expired tokens per REQ-FUNC-008. No refresh token is issued; the client re-authenticates via REQ-FUNC-007 when the token expires.
- Rationale: BR-013.
- Acceptance Criteria: A JWT issued with exp = issuedAt + 30 min, used after that timestamp, is rejected with 401.
- Verification Method: Test
```

#### 3.2.4 Outbox Publishing (UC-03)

```markdown
- ID: REQ-FUNC-010
- Title: Publish pending Outbox events to Kafka
- Statement: The system shall poll for unpublished OutboxEvent records every 5 seconds (configurable via `outbox.publisher.polling-interval-ms`) and publish each to its corresponding Kafka topic.
- Rationale: UC-03; decouples the HTTP transaction from Kafka availability.
- Acceptance Criteria: An event created by REQ-FUNC-004 is published within the polling interval and marked published only after a successful publish acknowledgment.
- Verification Method: Test (integration, Testcontainers Kafka)
```

```markdown
- ID: REQ-FUNC-011
- Title: Retry publishing on Kafka unavailability
- Statement: If Kafka is unavailable during a publish attempt, the system shall leave the event pending and retry on a subsequent polling cycle, without a maximum attempt limit, until publish succeeds.
- Rationale: A temporary Kafka failure must not lose a persisted order (NFR Reliability).
- Acceptance Criteria: Simulated Kafka unavailability leaves the event unpublished; once Kafka recovers, the event publishes on a later cycle with no manual intervention.
- Verification Method: Test (failure test)
```

#### 3.2.5 Inventory Reservation (UC-04)

```markdown
- ID: REQ-FUNC-012
- Title: Reserve inventory on sufficient stock
- Statement: Upon consuming OrderCreated, inventory-service shall, if stock is sufficient for all items, reserve the stock and publish InventoryReserved.
- Rationale: UC-04.
- Acceptance Criteria: Given sufficient seeded stock, consuming OrderCreated results in InventoryReserved referencing the same orderId.
- Verification Method: Test (integration)
```

```markdown
- ID: REQ-FUNC-013
- Title: Reject reservation on insufficient stock
- Statement: Upon consuming OrderCreated, inventory-service shall, if stock is insufficient for any item, publish InventoryRejected without reserving stock.
- Rationale: UC-04.
- Acceptance Criteria: Given insufficient seeded stock for at least one item, InventoryRejected is published; no stock is decremented.
- Verification Method: Test (integration)
```

#### 3.2.6 Order State Transitions (UC-05, UC-06)

```markdown
- ID: REQ-FUNC-014
- Title: Confirm order on InventoryReserved
- Statement: Upon consuming InventoryReserved, order-service shall transition the referenced order from PENDING to CONFIRMED.
- Rationale: BR-004.
- Acceptance Criteria: After consuming InventoryReserved for a PENDING order, GET returns CONFIRMED.
- Verification Method: Test (integration)
```

```markdown
- ID: REQ-FUNC-015
- Title: Reject order on InventoryRejected
- Statement: Upon consuming InventoryRejected, order-service shall transition the referenced order from PENDING to REJECTED.
- Rationale: BR-005.
- Acceptance Criteria: After consuming InventoryRejected for a PENDING order, GET returns REJECTED.
- Verification Method: Test (integration)
```

```markdown
- ID: REQ-FUNC-016
- Title: Deduplicate event processing per consumer
- Statement: Each event consumer shall record the eventId of every processed event in a processed_events table (unique per event_id, consumer) and shall skip business-effect processing for any event whose (event_id, consumer) pair already exists.
- Rationale: BR-006, BR-007.
- Acceptance Criteria: Delivering the same event twice to a consumer produces the business effect exactly once; the second delivery is a recorded no-op.
- Verification Method: Test (idempotency test)
```

```markdown
- ID: REQ-FUNC-017
- Title: Deterministic seeding
- Statement: Flyway shall seed the product catalog, client credentials and stock so that, after docker compose up, the system is operable without manual steps.
- Rationale: REQ-INST-001.
- Acceptance Criteria: On a clean clone, docker compose up --build leaves the catalog, credentials and stock available.
- Verification Method: Test (integration)
```

```markdown
- ID: REQ-FUNC-018
- Title: Declarative topics
- Statement: Each service shall declare the Kafka topics it produces or consumes, and their dead-letter topics, as code at startup, with auto topic creation disabled.
- Rationale: REQ-INST-001.
- Acceptance Criteria: The 6 topics exist with 3 partitions after startup, with no auto-create and no scripts.
- Verification Method: Test (integration)
```

```markdown
- ID: REQ-FUNC-019
- Title: Invalid transitions and unknown aggregates are no-ops
- Statement: When an event does not apply to the current state of the order, or refers to a non-existent aggregate, the consumer shall record it as processed and log it, without raising an exception.
- Rationale: BR-014.
- Acceptance Criteria: The event is recorded as processed, a WARN is logged, and there is no exception, retry or DLT entry.
- Verification Method: Test (integration)
```

```markdown
- ID: REQ-FUNC-020
- Title: Inventory events published through the outbox
- Statement: inventory-service shall write the reservation or rejection and its outgoing event in a single local transaction, and shall not publish to Kafka from the use case.
- Rationale: BR-008.
- Acceptance Criteria: Reservation or rejection and its outgoing event are committed together; no send() is made from the use case.
- Verification Method: Test (integration)
```

### 3.3 Quality of Service

#### 3.3.1 Performance

```markdown
- ID: REQ-PERF-001
- Title: Order creation latency (reference, non-SLA)
- Statement: Under normal local conditions (Docker Compose, no external load), order creation (REQ-FUNC-001–005) should complete with p95 latency under 300ms.
- Rationale: Demonstration reference only; no production SLA exists (2.3).
- Acceptance Criteria: Measured via a local benchmark script; documented as a reference figure, not a contractual target.
- Verification Method: Test (benchmark/demonstration)
```

#### 3.3.2 Security

```markdown
- ID: REQ-SEC-001
- Title: JWT signing and secret management
- Statement: The system shall sign JWTs using HS256 with a signing secret provided via environment variable/config, never hardcoded in source control.
- Rationale: Baseline secret hygiene, zero additional cost.
- Acceptance Criteria: The signing secret is externalized; the repository contains no literal secret value.
- Verification Method: Inspection
```

```markdown
- ID: REQ-SEC-002
- Title: No sensitive data in logs
- Statement: The system shall not log JWTs, client secrets, or Authorization header values at any log level.
- Rationale: Baseline confidentiality control.
- Acceptance Criteria: Code/log review confirms no logging statement includes token or secret values.
- Verification Method: Inspection
```

**Note:** HTTPS in transit is advisable for any real deployment, but it is not defined as a requirement in this version — there is no cloud infrastructure within scope (2.3). **N/A** for v1.

#### 3.3.3 Reliability

```markdown
- ID: REQ-REL-001
- Title: Consumer retry and dead-letter handling
- Statement: On a Kafka consumer processing failure, the system shall retry up to 3 times with exponential backoff (1s, 2s, 4s), then route the message to a dead-letter topic (<topic>.DLT).
- Rationale: Tolerates transient failures without blocking the consumer group; poison messages don't stall processing.
- Acceptance Criteria: A consumer that fails 3 times for a given message publishes it to the DLT and continues processing subsequent messages.
- Verification Method: Test (failure test)
```

#### 3.3.4 Availability

**N/A for this version.** Single-instance deployment, with no uptime objective or redundancy — consistent with the absence of a production SLA (2.3).

#### 3.3.5 Observability

```markdown
- ID: REQ-OBS-001
- Title: Correlate logs by orderId and eventId
- Statement: All log entries related to processing an order or an event shall include orderId and eventId (when applicable) as structured fields.
- Rationale: Enables tracing the flow Order → Outbox → Kafka → Consumer across services.
- Acceptance Criteria: Log entries emitted during UC-01–UC-07 contain orderId/eventId fields, verifiable by inspection of log output.
- Verification Method: Inspection
```

### 3.4 Compliance

```markdown
- ID: REQ-COMP-001
- Title: Permissive open-source licensing for all dependencies
- Statement: The system shall use only third-party libraries and tools distributed under permissive open-source licenses (e.g., MIT, Apache 2.0, BSD) compatible with public portfolio distribution.
- Rationale: Zero-cost constraint (2.3) and public portfolio distribution of the code — avoids license conflicts.
- Acceptance Criteria: A license audit of Maven dependencies shows no copyleft-incompatible or commercial licenses.
- Verification Method: Inspection
```

Remainder of the section: **N/A** — there is no real-user personal data and no payment processing, and therefore no regulatory obligations (GDPR/PCI/etc.) apply, since both scenarios are out of scope by design (1.2).

### 3.5 Design and Implementation

#### 3.5.1 Installation

```markdown
- ID: REQ-INST-001
- Title: Full environment startup via Docker Compose
- Statement: The system shall start order-service, inventory-service, PostgreSQL, and Kafka with a single `docker-compose up` command, with all environment-specific values (DB credentials, Kafka broker address, JWT signing secret, seeded client credentials) externalized via environment variables.
- Rationale: Docker Compose is mandatory (2.3); ensures reproducibility for portfolio reviewers.
- Acceptance Criteria: A clean clone of the repository, with Docker installed, becomes fully operational (all UC-01–UC-08 exercisable) after `docker-compose up` with no manual steps beyond setting env vars.
- Verification Method: Demonstration
```

Rollback/uninstall: `docker-compose down -v` (removes containers and volumes) — sufficient for a demo environment, with no need for a production rollback plan.

#### 3.5.2 Build and Delivery

- Build tool: **Maven**. Reproducibility via `mvn clean verify`, which runs unit and integration tests (Testcontainers).
- No external image registry: a local `docker-compose build` is sufficient; publishing images to a registry is not required (keeps the scope bounded and the cost at zero).

```markdown
- ID: REQ-BUILD-001
- Title: Continuous integration on push
- Statement: The system shall run an automated build and test pipeline (GitHub Actions) on every push and pull request, executing `mvn clean verify` (unit + integration tests).
- Rationale: Quality evidence for portfolio reviewers, at no cost (free tier for public repositories).
- Acceptance Criteria: A push to the repository triggers a workflow run; a failing test blocks a green status.
- Verification Method: Demonstration
```

#### 3.5.3 Distribution

**Single-instance** deployment (a single `docker-compose.yml`, no replicas, no multi-region, no orchestrator). **N/A** for any scale-out requirement, consistent with the Out-of-Scope section (Kubernetes, cloud infrastructure, multi-region).

#### 3.5.4 Maintainability

```markdown
- ID: REQ-MAINT-001
- Title: Domain layer independent of messaging technology
- Statement: The domain layer (order/inventory business logic) shall not import or reference Kafka-specific types; all Kafka interaction shall be isolated in adapter/infrastructure packages.
- Rationale: Allows Kafka to be replaced without touching the business rules.
- Acceptance Criteria: Static inspection of the domain package(s) shows zero imports from Kafka client libraries.
- Verification Method: Inspection
```

```markdown
- ID: REQ-MAINT-002
- Title: Documented event contracts
- Statement: Each event type (OrderCreated, InventoryReserved, InventoryRejected) shall have its schema (fields and types) documented in the repository (e.g., README or /docs).
- Rationale: Traceability and clarity for event consumers.
- Acceptance Criteria: A documentation file lists, for each event type, its full field set and an example payload.
- Verification Method: Inspection
```

#### 3.5.5 Reusability

**N/A.** No component is designed for reuse outside this project; it is neither a library nor a starter.

#### 3.5.6 Portability

```markdown
- ID: REQ-PORT-001
- Title: Cross-platform execution via containers
- Statement: The system shall run on any host with Docker Engine and Docker Compose installed (Linux, macOS, Windows), independent of the host OS.
- Rationale: The only real platform requirement is Docker; everything else lives in containers.
- Acceptance Criteria: Successful `docker-compose up` and UC execution on at least one Linux and one non-Linux host.
- Verification Method: Demonstration
```

#### 3.5.7 Cost

```markdown
- ID: REQ-COST-001
- Title: Zero operational cost
- Statement: The system shall not require any paid service, paid tier, or licensed software to build, run, or demo (development, CI, and runtime).
- Rationale: Explicit project constraint (2.3).
- Acceptance Criteria: No credit card, subscription, or paid API key is required to reproduce the full demo end-to-end.
- Verification Method: Inspection
```

#### 3.5.8 Deadline

**TBD.** No target delivery date has been defined; since this is a project with no external client, it is left to the author's discretion. It is recommended to record an approximate milestone here if progress tracking is desired.

#### 3.5.9 Proof of Concept

**N/A** as a separate phase. The complete project constitutes the validation of the patterns (Outbox, idempotency, eventual consistency, Kafka fault tolerance); no exploratory POC distinct from the development itself is planned beforehand.

#### 3.5.10 Change Management

```markdown
- ID: REQ-CM-001
- Title: Traceable change history
- Statement: The system shall track changes via Git with semantic/tagged versions (e.g., v0.1.0), without a formal external approval workflow (single-contributor project).
- Rationale: Individual project; a lightweight process keeps the focus on technical evidence.
- Acceptance Criteria: The repository has tagged releases matching SRS versions when applicable.
- Verification Method: Inspection
```

### 3.6 AI/ML

**N/A.** The system does not incorporate machine learning components or model-based behavior; 3.6.1–3.6.6 do not apply.

## 4. Verification

Actual status as of the date of this document: the project is not yet implemented — all requirements are **Not Started**. The test suites are proposed groupings intended to guide implementation, not final file paths.

| Requirement ID | Verification Method | Test Suite (proposed) | Status | Evidence |
|---|---|---|---|---|
| REQ-FUNC-001 / 002 | Test | OrderCreationValidationTests | Not Started | — |
| REQ-FUNC-003 / 004 / 005 | Test (integration) | OrderCreationTests | Not Started | — |
| REQ-FUNC-006 | Test | OrderQueryTests | Not Started | — |
| REQ-FUNC-007 / 008 / 009 | Test | AuthTests | Not Started | — |
| REQ-FUNC-010 / 011 | Test (integration/failure) | OutboxPublisherTests | Not Started | — |
| REQ-FUNC-012 / 013 | Test (integration) | InventoryReservationTests | Not Started | — |
| REQ-FUNC-014 / 015 | Test (integration) | OrderStateTransitionTests | Not Started | — |
| REQ-FUNC-016 | Test (idempotency) | IdempotencyTests | Not Started | — |
| REQ-PERF-001 | Test (benchmark) | PerformanceBenchmark | Not Started | — |
| REQ-SEC-001 / 002 | Inspection | Code/config review | Not Started | — |
| REQ-REL-001 | Test (failure) | ConsumerRetryDltTests | Not Started | — |
| REQ-OBS-001 | Inspection | Log output review | Not Started | — |
| REQ-COMP-001 | Inspection | Dependency license audit | Not Started | — |
| REQ-INST-001 | Demonstration | Manual: docker-compose up | Not Started | — |
| REQ-BUILD-001 | Demonstration | CI run history | Not Started | — |
| REQ-MAINT-001 / 002 | Inspection | Static/package review | Not Started | — |
| REQ-PORT-001 | Demonstration | Manual: multi-OS run | Not Started | — |
| REQ-COST-001 | Inspection | Manual audit | Not Started | — |
| REQ-CM-001 | Inspection | Git tags review | Not Started | — |

## 5. Appendixes

### Appendix A — Architecture Diagram (reference)

```text
                  Client
                     │
                     ▼
              ┌──────────────┐
              │ Order Service│
              │ Spring Boot  │
              │ PostgreSQL   │
              └──────┬───────┘
                     │
                Outbox Event
                     │
                     ▼
                  Kafka
                     │
                     ▼
           ┌───────────────────┐
           │ Inventory Service │
           └─────────┬─────────┘
                     │
              InventoryReserved
                     │
                     ▼
                Order Service
```

Full source: `02-event-driven-order-platform-context-maestro.md`, Section 8.

### Appendix B — Data Model Summary

| Table | Key Fields |
|---|---|
| `orders` | `id`, `status` (PENDING/CONFIRMED/REJECTED), `total`, `createdAt` |
| `order_items` | `orderId`, `productId`, `quantity`, `unitPriceSnapshot` |
| `outbox_events` | `eventId`, `eventType`, `aggregateId`, `payload`, `publishedAt` |
| `processed_events` | `eventId`, `consumer`, `processedAt` — `UNIQUE(event_id, consumer)` |

The detailed column design (types, indexes) is an implementation decision outside the normative scope of this SRS.

### Appendix C — Business Rules

| ID | Rule |
|---|---|
| BR-001 | An order contains at least one item. |
| BR-002 | The quantity of each item is greater than zero. |
| BR-003 | Every order starts in `PENDING`. |
| BR-004 | `PENDING → CONFIRMED` only via `InventoryReserved`. |
| BR-005 | `PENDING → REJECTED` only via `InventoryRejected`. |
| BR-006 | Every event has a unique identifier. |
| BR-007 | A consumer does not apply the same event twice. |
| BR-008 | Order and Outbox Event are persisted atomically. |
| BR-009 | PostgreSQL is the source of truth for order state. |
| BR-010 | Kafka is not the source of truth for business state. |
| BR-011 | An order's state may temporarily remain `PENDING`. |
| BR-012 | Any request to a protected endpoint without a valid JWT must be rejected with `401`. |
| BR-013 | The JWT has a configurable expiration (30 min); once expired, a new one must be requested. |
| BR-014 | Una transición de estado inválida es un no-op registrado, nunca una excepción. |
| BR-015 | La reserva de stock es todo o nada para la orden completa. |
| BR-016 | El stock decrementado no se libera en v1. |
| BR-017 | `CONFIRMED` y `REJECTED` son estados terminales. |
| BR-018 | `unit_price_snapshot` se fija al crear la orden y no se recalcula. |
