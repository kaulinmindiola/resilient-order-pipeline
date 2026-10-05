---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0014: Keep a read-only product catalog in order-service and never accept client prices

## Context and Problem Statement

An order has a `total`, and that total needs a trustworthy source of price. If the client sent prices, the total would be client-controlled. Catalog management, carts and checkout are out of scope. Where do prices come from, and when are they fixed?

## Decision Drivers

* REQ-FUNC-003: `total = Σ(quantity × unit_price_snapshot)`, scale 2.
* BR-018: `unit_price_snapshot` is fixed at creation and never recalculated.
* No synchronous interaction between services; everything crosses Kafka.
* Minimal scope: no catalog CRUD.

## Considered Options

* Read-only `products` table in `order_service`, seeded by Flyway
* Prices sent by the client in the request
* Synchronous price lookup against `inventory-service`
* Prices stored together with stock in `inventory_service`

## Decision Outcome

Chosen option: **"Read-only `products` table in `order_service`"**, because it gives a reliable price source inside the service that owns the total, with no cross-service call.

* `products(product_id, unit_price)` is seeded by migration ([ADR-0009](0009-flyway-migrations-and-seed-data.md)) and never written by the service.
* The request carries only `productId` and `quantity`. A `productId` absent from the catalog returns `422`.
* On creation, each item copies the current price into `order_items.unit_price_snapshot`.
* Seeded data: `SKU-001` (19.99), `SKU-002` (49.90), `SKU-003` (5.50), `SKU-004` (12.00). `SKU-004` exists in the catalog but has no `stock` row, which exercises the `UNKNOWN_PRODUCT` rejection.

### Consequences

* Good, because the total is computed from a trusted source.
* Good, because the snapshot keeps historical orders stable even if prices change later.
* Bad, because there is no catalog management; changing a price requires a new migration.
* Bad, because catalog and stock are separate sources, so a product can be orderable yet have no stock (by design for `SKU-004`).

### Confirmation

Unit tests for total calculation and decimal precision; API tests for `422` on an unknown `productId` and for `201` with a correct total; integration test reading the seeded catalog. Verified in Phases 2 and 3.

## Pros and Cons of the Options

### Read-only catalog in `order_service`

* Good, because it is simple and local.
* Bad, because it cannot be managed at runtime.

### Prices sent by the client

* Good, because it needs no catalog.
* Bad, because the client controls the total, which is unacceptable.

### Synchronous lookup against `inventory-service`

* Good, because there is a single source of product data.
* Bad, because it creates a synchronous dependency between services and a new failure mode, contradicting the event-only interaction model.

### Prices in `inventory_service`

* Good, because it keeps product data in one place.
* Bad, because `order-service` cannot read that schema ([ADR-0002](0002-schema-and-role-per-service.md)).

## More Information

* Related: [ADR-0002](0002-schema-and-role-per-service.md), [ADR-0009](0009-flyway-migrations-and-seed-data.md).
* Requirements: REQ-FUNC-003, BR-018.
