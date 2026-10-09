---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0018: Use JPA only for the Order aggregate and JdbcClient everywhere else

## Context and Problem Statement

The system has two kinds of persistence needs. `Order` is an aggregate with child items and a state machine, where an object-relational mapper adds real value. Outbox, processed events, catalog, credentials and stock are technical or single-statement tables where explicit SQL is clearer. Stock reservation in particular relies on one atomic `UPDATE` and a savepoint ([ADR-0010](0010-atomic-conditional-update-for-stock-reservation.md)). What persistence technology is used where?

## Decision Drivers

* The reservation must be one atomic SQL statement with a savepoint.
* The domain must not import `jakarta.persistence` ([ADR-0006](0006-lightweight-hexagonal-architecture.md)).
* Use ORM where it pays off, explicit SQL where control matters.
* Avoid extra dependencies and special transaction configuration.

## Considered Options

* JPA only for the `Order` aggregate; `JdbcClient` for technical tables and stock
* JPA for everything
* `JdbcClient` for everything
* An alternative SQL mapper (for example jOOQ or MyBatis)

## Decision Outcome

Chosen option: **"JPA for `Order`, `JdbcClient` elsewhere"**, because JPA is valuable for an aggregate with relationships, while the reservation is a single atomic `UPDATE` and the savepoint works with no special configuration on the JDBC transaction manager.

* `order-service`: JPA (Hibernate 7) for `Order` and its items; `JdbcClient` for `outbox_events`, `processed_events` (the `claim`), `products` and `client_credentials`.
* `inventory-service` uses no JPA at all: `JdbcClient` for `stock`, `outbox_events` and `processed_events`.
* JPA entities live in the adapter, and the domain `Order` is mapped explicitly to and from them.
* `@Transactional` sits on application use cases. The reservation savepoint is created in the adapter directly on the current transaction's connection using `DataSourceUtils.getConnection(dataSource)`.

### Consequences

* Good, because each tool is used where it adds value.
* Good, because the atomic `UPDATE` and the savepoint need no extra configuration with the JDBC transaction manager.
* Good, because `inventory-service` stays lean, with no ORM.
* Bad, because `order-service` mixes two persistence styles in one transaction, which requires discipline.
* Bad, because mapping between the domain `Order` and its JPA entity adds code.

### Confirmation

In `order-service`, verified by [`OrderRepositoryIT`](../../order-service/src/test/java/io/github/kaulinmindiola/rop/order/adapter/out/persistence/OrderRepositoryIT.java)
(JPA round trip of the order aggregate; a status change does not rewrite its items) and by the
`JdbcClient` adapter tests in the same package. In `inventory-service`, savepoint-based stock
reservation with the JDBC transaction manager will be verified by its reservation tests.

## Pros and Cons of the Options

### JPA for `Order`, `JdbcClient` elsewhere

* Good, because each use case gets the fitting tool.
* Bad, because there are two styles to maintain in `order-service`.

### JPA for everything

* Good, because there is one persistence style.
* Bad, because the reservation `UPDATE` and the savepoint fight the persistence context, and technical tables gain nothing from an ORM.

### `JdbcClient` for everything

* Good, because there is one explicit, simple style.
* Bad, because the `Order` aggregate would need manual relationship handling that JPA does for free.

### Alternative SQL mapper

* Good, because it offers typed SQL.
* Bad, because it adds a dependency and a learning surface without solving a problem the current tools leave open.

## More Information

* Related: [ADR-0006](0006-lightweight-hexagonal-architecture.md), [ADR-0010](0010-atomic-conditional-update-for-stock-reservation.md), [ADR-0017](0017-java-21-spring-boot-4-kafka-4.md).
* Requirements: REQ-FUNC-003, REQ-FUNC-004, REQ-FUNC-012.
