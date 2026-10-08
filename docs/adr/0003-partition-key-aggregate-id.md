---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0003: Use the aggregate ID as the Kafka message key

## Context and Problem Statement

The topics `order.created`, `inventory.reserved` and `inventory.rejected` have several partitions ([ADR-0012](0012-declarative-topics-three-partitions.md)). Without a deliberate key, Kafka gives no guarantee that events about the same order land in the same partition, so they could be consumed out of publication order. Which key do producers set?

## Decision Drivers

* Order of events must be preserved per aggregate (per order).
* REQ-OBS-001: all events of an order should be easy to correlate.
* The choice is cheap now and costly to change once consumers depend on it.
* The behaviour must be demonstrable with a test.

## Considered Options

* Key = `aggregateId` (the `orderId`)
* No key (round-robin / sticky partitioning)
* Key = `eventType`
* Key = `eventId`

## Decision Outcome

Chosen option: **"Key = `aggregateId`"**, because every event of the same order goes to the same partition and is therefore consumed in publication order, and because correlation for debugging becomes trivial.

The outbox publisher uses `outbox_events.aggregate_id` as the record key ([ADR-0001](0001-transactional-outbox-polling-publisher.md)). Records sent to a dead-letter topic keep their original key ([ADR-0004](0004-consumer-retry-and-dead-letter-topics.md)).

### Consequences

* Good, because per-aggregate ordering is guaranteed.
* Good, because the key is already present in the event envelope.
* Bad, because partition distribution depends on the keys actually used; with a few keys it can be uneven. Acceptable at this volume.

### Confirmation

Verified by [`PartitionOrderingIT`](../../order-service/src/test/java/io/github/kaulinmindiola/rop/order/adapter/out/kafka/PartitionOrderingIT.java):
interleaved events of several aggregates are published, and each aggregate's events land in a
single partition and are consumed in publication order. [`OutboxPublisherIT`](../../order-service/src/test/java/io/github/kaulinmindiola/rop/order/adapter/out/kafka/OutboxPublisherIT.java)
verifies that the record key is the order id.

## Pros and Cons of the Options

### Key = `aggregateId`

* Good, because it preserves order per order.
* Good, because it is trivial to implement.
* Bad, because the distribution is subject to the key space.

### No key

* Good, because it is marginally simpler.
* Bad, because events of one order may be processed out of order across partitions.

### Key = `eventType`

* Bad, because it groups unrelated orders together and does nothing to order the events of one order.

### Key = `eventId`

* Bad, because every event gets a unique key, which is equivalent to having no ordering guarantee.

## More Information

* Related: [ADR-0001](0001-transactional-outbox-polling-publisher.md), [ADR-0004](0004-consumer-retry-and-dead-letter-topics.md), [ADR-0012](0012-declarative-topics-three-partitions.md).
