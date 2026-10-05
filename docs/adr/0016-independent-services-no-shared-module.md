---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0016: Keep services independent with no shared code module

## Context and Problem Statement

Both services need an outbox publisher and an event-envelope mapper with nearly identical logic. The usual instinct is to extract a shared library. A shared module, however, couples the services through a library: a change forces a coordinated rebuild and redeploy, and the services stop being independently evolvable. The repository is a Maven multi-module build, which makes sharing tempting. Do the services share code?

## Decision Drivers

* Independent deployment and evolution of each service.
* Avoid coupling through a common library.
* The architecture presents two real services, each with its own bounded context ([ADR-0002](0002-schema-and-role-per-service.md)).
* The duplication is small and can be controlled with tests.

## Considered Options

* No shared module: duplicate the publisher and mapper in each service
* A shared library module with publisher and mapper
* A shared contracts-only module (event types and envelope)
* A single deployable for both responsibilities

## Decision Outcome

Chosen option: **"No shared module; duplicate consciously"**, because independence of the services is worth more here than avoiding a few duplicated classes.

* `OutboxPublisher` and `EventEnvelopeMapper` exist in both `order-service` and `inventory-service`.
* The topic declarations are repeated with identical definitions ([ADR-0012](0012-declarative-topics-three-partitions.md)).
* The parent POM only manages versions and plugins; it contains no shared code, and neither service depends on the other.
* Event contracts are documented in `docs/events.md`, with one example per event type, and kept backward compatible.

### Consequences

* Good, because each service can be built, deployed and evolved on its own.
* Good, because there is no library version to coordinate.
* Bad, because the duplicated publisher and mapper can drift apart. This is controlled by equivalent tests in both services and by reviewing the copy with a `diff`.
* Bad, because a bug fix may have to be applied twice.

### Confirmation

Inspection of the Maven reactor: no service depends on another module or on a shared code module. Equivalent publisher and mapper tests run in both services (tests 2 and 14 exercise each side). Verified from Phase 5 (`order-service`) and Phase 7 (copy in `inventory-service`).

## Pros and Cons of the Options

### No shared module

* Good, because services stay fully independent.
* Bad, because of the duplicated code and the drift risk.

### Shared library module

* Good, because there is a single implementation.
* Bad, because it couples both services to one artifact and one release cadence.

### Shared contracts-only module

* Good, because event types are defined once.
* Bad, because it still couples services by library and conflicts with the documented, backward-compatible contract approach.

### Single deployable

* Good, because there is no duplication.
* Bad, because it removes the asynchronous, two-service interaction the project exists to demonstrate.

## More Information

* Related: [ADR-0001](0001-transactional-outbox-polling-publisher.md), [ADR-0006](0006-lightweight-hexagonal-architecture.md), [ADR-0013](0013-outbox-in-inventory-service.md).
* Requirements: REQ-MAINT-002.
