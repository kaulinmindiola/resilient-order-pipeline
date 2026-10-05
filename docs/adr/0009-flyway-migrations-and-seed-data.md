---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0009: Version the schema and seed data with Flyway, one migration path per service

## Context and Problem Statement

REQ-INST-001 requires that a clean clone is fully operational after `docker compose up --build`. That includes the schemas, the product catalog, the stock and the client credentials (REQ-FUNC-017). Credentials must be stored as BCrypt hashes, never in clear text. Which tool versions the schema and performs the seeding?

## Decision Drivers

* REQ-INST-001 and REQ-FUNC-017: deterministic, reproducible start with no manual steps.
* [ADR-0002](0002-schema-and-role-per-service.md): independent migration paths per schema.
* Auditable, explicit schema evolution.
* Zero cost.

## Considered Options

* Flyway, one path per service, with seeds as migrations
* Liquibase
* `spring.jpa.hibernate.ddl-auto` plus a `CommandLineRunner` for seeding
* Plain SQL scripts mounted into the PostgreSQL entrypoint

## Decision Outcome

Chosen option: **"Flyway with one path per service"**, because it gives explicit, auditable versioning and deterministic seeding, avoiding the non-determinism of `ddl-auto`.

* Dependencies: `spring-boot-starter-flyway` and `flyway-database-postgresql`.
* Locations: `classpath:db/migration/order` and `classpath:db/migration/inventory`.
* Convention: `V1` creates the schema, `V2` seeds data with SQL.
* In `order-service`, `V3` is a **Java migration** (`db.migration.order`) that hashes `SEED_CLIENT_SECRET` with BCrypt. The secret reaches the migration as a Flyway placeholder.
* Seed data: products `SKU-001..004` and the stock rows described in the data model; `SKU-004` has no stock row on purpose.
* Flyway runs with each service's own role ([ADR-0015](0015-schema-owner-roles-and-flyway.md)).

### Consequences

* Good, because every schema change is a numbered, reviewable script.
* Good, because startup is deterministic and needs no manual step.
* Bad, because seeds are immutable once applied; changing one means adding a new migration.

### Confirmation

Integration tests apply the migrations on a real PostgreSQL (Testcontainers) and read the seeded catalog; test 6 authenticates with the seeded credentials; the clean-clone check (`docker compose up --build`, then `docs/requests.http`) needs no manual step. Verified in Phase 2 and Phase 10.

## Pros and Cons of the Options

### Flyway

* Good, because it is simple and the most common choice in Spring Boot projects.
* Good, because Java migrations allow BCrypt hashing at seed time.
* Neutral, because it is one convention among equivalent ones.

### Liquibase

* Good, because it is functionally equivalent here.
* Neutral, because its XML/YAML changelogs are more verbose than plain SQL.

### `ddl-auto` + `CommandLineRunner`

* Good, because it is the fastest to set up.
* Bad, because the schema is not versioned and behaviour can differ between environments.

### Init scripts in the PostgreSQL entrypoint

* Good, because it needs no tool.
* Bad, because it only runs on an empty volume and gives no versioned evolution.

## More Information

* Related: [ADR-0002](0002-schema-and-role-per-service.md), [ADR-0005](0005-self-issued-jwt-with-resource-server.md), [ADR-0014](0014-read-only-product-catalog.md), [ADR-0015](0015-schema-owner-roles-and-flyway.md).
* Requirements: REQ-FUNC-017, REQ-INST-001.
