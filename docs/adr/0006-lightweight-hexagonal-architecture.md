---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0006: Structure both services with a lightweight hexagonal architecture

## Context and Problem Statement

REQ-MAINT-001 requires that the `domain` package does not depend on infrastructure, and that this is verifiable. The domain must not import Spring, `jakarta.persistence`, `org.apache.kafka`, `org.springframework.kafka` or Jackson. Which layering style is applied to `order-service` and `inventory-service`?

## Decision Drivers

* REQ-MAINT-001: independence of the domain, verified automatically.
* Portfolio scope: demonstrable without excessive ceremony.
* Domain testable in isolation, with plain unit tests and no containers.
* Business rules (BR-014) must not depend on exceptions thrown by infrastructure.

## Considered Options

* Lightweight hexagonal (`domain` / `application` / `adapter`)
* Simple layers (Controller → Service → Repository) with Kafka wrapped inside the service layer
* Full Clean Architecture with use-case interactors and per-layer mappers

## Decision Outcome

Chosen option: **"Lightweight hexagonal"**, because it gives the verifiable guarantee required by REQ-MAINT-001 without the ceremony of full Clean Architecture.

Dependency direction: `adapter → application → domain` and `adapter → domain`, never the reverse.

* `domain`: model and outbound **ports** (`OrderRepository`, `OutboxEventRepository`, `ProcessedEventRepository`, `StockRepository`, ...). It returns results instead of throwing for invalid state transitions.
* `application`: use cases (`CreateOrderService`, `ReserveInventoryService`, `ApplyInventoryOutcomeService`), which own the `@Transactional` boundary.
* `adapter`: REST, Kafka listeners, persistence, outbox publisher, envelope mapper, security.

Two deliberate rules follow:

* **No `EventPublisher` port exists.** The application layer only writes to its `OutboxEventRepository`; publishing to Kafka lives entirely in the adapter ([ADR-0001](0001-transactional-outbox-polling-publisher.md), [ADR-0013](0013-outbox-in-inventory-service.md)).
* DTOs are Java `record`s and there is no Lombok.

### Consequences

* Good, because the domain can be tested with plain unit tests and mocked ports.
* Good, because the independence of the domain is a checked rule, not a promise.
* Neutral, because it introduces a few interfaces that a trivial CRUD would not need; they are justified by the explicit requirement.
* Bad, because explicit mapping between layers adds some files and indirection.

### Confirmation

`ArchitectureTest` (ArchUnit) in each service verifies that `domain` imports none of the forbidden packages and that the dependency direction holds. It runs from Phase 1 onwards.

## Pros and Cons of the Options

### Lightweight hexagonal

* Good, because REQ-MAINT-001 is met in a verifiable way.
* Good, because the domain is testable in isolation.
* Neutral, because it has somewhat more files than simple layers.

### Simple layers

* Good, because it is the least code.
* Bad, because nothing structurally prevents Kafka or JPA types from leaking into the service layer.

### Full Clean Architecture

* Good, because boundaries are very strict.
* Bad, because interactors and per-layer mappers are disproportionate for two small services.

## More Information

* Related: [ADR-0018](0018-jpa-for-order-aggregate-jdbcclient-elsewhere.md) (JPA entities live in the adapter), [ADR-0016](0016-independent-services-no-shared-module.md).
* Requirements: REQ-MAINT-001, BR-014.
