---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0015: Make each service role the owner of its schema and run Flyway with that role

## Context and Problem Statement

[ADR-0002](0002-schema-and-role-per-service.md) isolates data with one schema and one role per service. Migrations still need a database identity to run with. Running them as a superuser would work, but it would give migrations power over every schema and make the isolation easy to bypass by accident. Which role owns the schema and which role runs the migrations?

## Decision Drivers

* Isolation from ADR-0002 must stay intact, including during migrations.
* Migrations should not require a superuser.
* Reproducible start (REQ-INST-001): the setup must be scripted.
* Test environments must mirror production-like privileges.

## Considered Options

* Each role owns its schema and Flyway runs with that same role
* A superuser runs all migrations
* Separate migration and runtime roles per service
* Grants without ownership (a shared owner grants privileges to each role)

## Decision Outcome

Chosen option: **"Each role owns its schema and Flyway runs with that role"**, because migrations run without a superuser and the isolation is never weakened.

* `infra/postgres/init/01-schemas-and-roles.sh` runs once as the PostgreSQL superuser. It creates `order_svc` and `inventory_svc` with passwords from `ORDER_DB_PASSWORD` and `INVENTORY_DB_PASSWORD`, creates each schema owned by its role, and grants nothing across schemas.
* Services connect with `DB_URL`, `DB_SCHEMA`, `DB_USER` and `DB_PASSWORD`; Flyway uses the same credentials and the service schema as default schema.
* Privileges are never relaxed to make a test easier. The test database is initialized with the same script.
* `.gitattributes` forces LF line endings so the script runs correctly in a Linux container.

### Consequences

* Good, because migrations need no superuser and cannot touch the other schema.
* Good, because the same script drives Compose and Testcontainers, so test and runtime privileges match.
* Bad, because the runtime role can execute DDL in its own schema. This is accepted for simplicity; a separate migration role would remove it.

### Confirmation

Verified by [`SchemaIsolationIT`](../../order-service/src/test/java/io/github/kaulinmindiola/rop/order/SchemaIsolationIT.java),
which shows that each role creates and drops objects in its own schema and that the application
connection is not a superuser, and by [`SeedDataIT`](../../order-service/src/test/java/io/github/kaulinmindiola/rop/order/SeedDataIT.java),
which shows that Flyway applied every migration, including the Java seed, with the service role.

## Pros and Cons of the Options

### Owner role runs Flyway

* Good, because it is simple and minimal.
* Bad, because the runtime role holds DDL rights on its schema.

### Superuser runs migrations

* Good, because it is easy to set up.
* Bad, because it breaks the isolation principle and needs a privileged credential at deploy time.

### Separate migration and runtime roles

* Good, because it gives least privilege at runtime.
* Bad, because it doubles the roles and configuration for little benefit at this scope.

### Grants without ownership

* Good, because ownership is centralized.
* Bad, because it needs a shared owner and fine-grained grants that are easy to get wrong.

## More Information

* Related: [ADR-0002](0002-schema-and-role-per-service.md), [ADR-0009](0009-flyway-migrations-and-seed-data.md).
* Requirements: REQ-INST-001.
