---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0010: Reserve stock with an atomic conditional UPDATE and a savepoint

## Context and Problem Statement

REQ-FUNC-012 and REQ-FUNC-013 require `inventory-service` to reserve stock when it suffices for **all** items of an order, and otherwise to reject the whole order without decrementing anything (BR-015). Under concurrent orders for the same product there must be no overselling. The guarantee must live in the database (BR-009) and be resolved inside one local transaction.

A rejection also has a subtle requirement: it must be **committed**. The same transaction contains the idempotency claim and the outgoing `InventoryRejected` outbox event ([ADR-0011](0011-idempotent-consumer-single-local-transaction.md)), so a rejection cannot abort the consumer transaction.

## Decision Drivers

* REQ-FUNC-012 / REQ-FUNC-013 and BR-015: all-or-nothing reservation.
* Correctness under concurrency, enforced by PostgreSQL.
* No distributed transactions.
* Avoid deadlocks when an order has several items.
* A rejection is a *result*, not a failure.

## Considered Options

* Conditional atomic `UPDATE ... WHERE quantity >= :qty`, checking `rowsAffected`, with a savepoint
* Optimistic locking (`@Version`) with retry
* Pessimistic locking (`SELECT ... FOR UPDATE`)
* `SERIALIZABLE` isolation with retry
* Let a rejection roll back the whole consumer transaction

## Decision Outcome

Chosen option: **"Conditional atomic UPDATE with a savepoint"**, because the engine resolves the race in one statement, with no explicit locks and no retry logic.

```text
tryReserve(items):                       # runs inside the consumer transaction
    connection = DataSourceUtils.getConnection(dataSource)
    sp = connection.setSavepoint()
    for item in items sorted by productId:
        rows = UPDATE stock SET quantity = quantity - :qty
               WHERE product_id = :id AND quantity >= :qty
        if rows != 1:
            connection.rollback(sp)
            connection.releaseSavepoint(sp)
            reason = stockRowExists(item.productId) ? INSUFFICIENT_STOCK : UNKNOWN_PRODUCT
            return REJECTED(reason)
    connection.releaseSavepoint(sp)
    return RESERVED
```

* Items are processed in `productId` order, so concurrent orders acquire row locks in the same order and cannot deadlock.
* The savepoint is taken before the first decrement on the caller's JDBC transaction connection via `DataSourceUtils.getConnection(dataSource)`. A rejection rolls back only to the savepoint, undoing partial decrements while leaving the surrounding transaction healthy; the rejection is then committed with the claim and the outbox event.
* `tryReserve` requires an active transaction (`IllegalStateException` if missing) and never marks the transaction rollback-only. Only infrastructure failures surface as exceptions.
* `stock.quantity` is *available* stock with `CHECK (quantity >= 0)`. Reserved stock is never released in v1 (BR-016).

### Consequences

* Good, because atomicity is delegated to PostgreSQL in a single statement.
* Good, because no explicit lock is held during consumer processing beyond the row lock taken by the `UPDATE` itself.
* Good, because sorted access prevents deadlocks, and the savepoint keeps rejections committable.
* Bad, because the single-column stock model has no history and no releasable reservations.
* Bad, because savepoint handling is more subtle than a plain transaction and must be tested explicitly.

### Confirmation

Verified by [`StockReservationIT`](../../inventory-service/src/test/java/io/github/kaulinmindiola/rop/inventory/adapter/out/persistence/StockReservationIT.java)
(all-or-nothing: a partial reservation is undone; insufficient stock and unknown products are
rejected with their reason; a rejection does not abort the caller's transaction, whose other
writes commit) and by [`ConcurrentReservationIT`](../../inventory-service/src/test/java/io/github/kaulinmindiola/rop/inventory/adapter/out/persistence/ConcurrentReservationIT.java)
(test 11: of 20 concurrent reservations of the last unit exactly one succeeds and stock never goes
negative; reservations listing the same products in opposite orders complete without deadlocks).
[`StockSchemaIT`](../../inventory-service/src/test/java/io/github/kaulinmindiola/rop/inventory/StockSchemaIT.java)
verifies that the database rejects negative stock independently of the reservation logic.

## Pros and Cons of the Options

### Conditional UPDATE with savepoint

* Good, because it is simple, with no retries or held locks.
* Good, because it is fully handled by the engine.
* Neutral, because multi-item handling needs explicit code.

### Optimistic locking

* Good, because it works well under low contention.
* Bad, because it needs retry logic on version conflicts.

### Pessimistic locking

* Good, because the guarantee is explicit and easy to reason about.
* Bad, because row locks are held for the whole processing time, with more contention across multi-item orders.

### SERIALIZABLE isolation

* Good, because it is a strong, declarative guarantee.
* Bad, because serialization failures must be retried and it penalizes the whole transaction.

### Roll back the whole transaction on rejection

* Bad, because it would discard the idempotency claim and the outgoing event, so the rejection would be reprocessed and eventually dead-lettered instead of being a normal outcome.

## More Information

* Related: [ADR-0011](0011-idempotent-consumer-single-local-transaction.md), [ADR-0018](0018-jpa-for-order-aggregate-jdbcclient-elsewhere.md) (JDBC transaction manager supports the savepoint with no special configuration).
* Requirements: REQ-FUNC-012, REQ-FUNC-013, BR-015, BR-016, BR-009.
