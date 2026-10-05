---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0002: Isolate data with one schema and one role per service in a single PostgreSQL instance

## Context and Problem Statement

The project uses a single PostgreSQL instance shared by `order-service` and `inventory-service`; one database per service is explicitly out of scope. The requirements fix *what* (single instance, separation by domain) but not *how* that separation is enforced. BR-009 and BR-010 make PostgreSQL the only source of truth for business state, and the architecture forbids any interaction between services through SQL: every interaction must cross Kafka.

How is data isolated between the two services?

## Decision Drivers

* One PostgreSQL instance; one database per service is out of scope.
* BR-009 / BR-010: PostgreSQL is the source of truth, Kafka is only transport.
* Real bounded contexts: each service must be able to evolve independently.
* No distributed transactions: cross-service coordination must not be possible through joins or foreign keys.
* The rule must be enforceable and testable, not only a convention.

## Considered Options

* One schema and one database role per service, each role limited to its own schema
* One shared schema with common tables accessed by both services
* One schema per service, but a single shared database role
* One PostgreSQL instance or database per service

## Decision Outcome

Chosen option: **"One schema and one role per service"**, because it satisfies the single-instance constraint while keeping real bounded contexts, and it makes cross-service SQL impossible rather than merely discouraged.

| Schema | Role | Tables |
|---|---|---|
| `order_service` | `order_svc` | `orders`, `order_items`, `products`, `client_credentials`, `outbox_events`, `processed_events` |
| `inventory_service` | `inventory_svc` | `stock`, `outbox_events`, `processed_events` |

Each role owns its schema and has no privilege on the other one ([ADR-0015](0015-schema-owner-roles-and-flyway.md)). There are no joins, foreign keys or queries across schemas. Privileges are never relaxed to make a test easier.

### Consequences

* Good, because a future move to separate databases would not require redesigning the data boundary.
* Good, because the rule "services interact only through Kafka" is enforced by database grants.
* Good, because it is verifiable: a role that tries to read the other schema gets *permission denied*.
* Bad, because the shared instance remains a single point of failure and resource contention; acceptable since availability is not a requirement.

### Confirmation

Test 15 (*schema isolation*): `order_svc` receives *permission denied* when accessing `inventory_service`. Migrations are also reviewed for the absence of cross-schema foreign keys. Verified in Phase 2.

## Pros and Cons of the Options

### One schema and one role per service

* Good, because it preserves bounded-context autonomy.
* Good, because isolation is enforced by the engine.
* Neutral, because it requires configuring two roles and two schemas, a small overhead compared with two instances.
* Bad, because infrastructure failures still affect both services.

### Shared schema with common tables

* Good, because it is the fastest to set up.
* Bad, because it couples both services at the data level, contradicting BR-009 / BR-010.
* Bad, because it invites cross-service transactions, which are excluded.

### One schema per service with a shared role

* Good, because it is slightly simpler to configure.
* Bad, because nothing prevents either service from reading the other's tables; the separation would be a convention only.

### One instance or database per service

* Good, because it gives the strongest isolation.
* Bad, because it violates an explicit out-of-scope item of the project.

## More Information

* Related: [ADR-0009](0009-flyway-migrations-and-seed-data.md) (one migration path per service), [ADR-0015](0015-schema-owner-roles-and-flyway.md) (role ownership), [ADR-0016](0016-independent-services-no-shared-module.md).
* The role and schema bootstrap lives in `infra/postgres/init/01-schemas-and-roles.sh`.
