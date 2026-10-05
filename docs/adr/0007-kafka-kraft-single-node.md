---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0007: Run Kafka in KRaft mode as a single node

## Context and Problem Statement

REQ-INST-001 requires the whole environment to start with a single `docker compose up --build`, with no manual steps. The project targets Apache Kafka 4.x. How is the broker deployed?

## Decision Drivers

* REQ-INST-001: minimal operational complexity, one command.
* REQ-COST-001: no paid services or licences.
* Fewer containers and less configuration surface.
* Kafka 4.x only operates in KRaft mode; ZooKeeper is no longer an option.

## Considered Options

* Apache Kafka in KRaft mode, one node acting as broker and controller
* Kafka with ZooKeeper
* A Kafka-compatible alternative broker
* A managed Kafka service

## Decision Outcome

Chosen option: **"Kafka in KRaft mode, one node"**, using the official `apache/kafka:4.2.2` image. ZooKeeper does not exist in Kafka 4.x, and a single node removes one container from the Compose file.

* Internal listener `kafka:9092` for the services; external listener `localhost:9094` for the host.
* A healthcheck on the broker and `depends_on: service_healthy` in the services.
* `auto.create.topics.enable=false`; topics are declared by the services ([ADR-0012](0012-declarative-topics-three-partitions.md)).

### Consequences

* Good, because there is one container less and a simpler, faster start.
* Good, because it is the only supported mode going forward.
* Neutral, because older tutorials assume ZooKeeper, so the KRaft listener setup needs careful, one-time verification.
* Bad, because a single node has no broker fault tolerance (replication factor 1). Kafka outages are handled by the outbox ([ADR-0001](0001-transactional-outbox-polling-publisher.md)), not by the broker.

### Confirmation

`docker compose up --build` brings up a healthy broker that both services reach, with no ZooKeeper container; the system tests run against that same Compose file. Verified in Phase 1 and Phase 9.

## Pros and Cons of the Options

### KRaft, single node

* Good, because it is minimal and official.
* Bad, because it provides no broker redundancy.

### Kafka with ZooKeeper

* Good, because there is a lot of historical documentation.
* Bad, because it is unavailable in Kafka 4.x and would force an older version ([ADR-0017](0017-java-21-spring-boot-4-kafka-4.md)).

### Alternative Kafka-compatible broker

* Good, because some are lightweight.
* Bad, because it deviates from the Apache Kafka behaviour the project intends to demonstrate and test against.

### Managed Kafka service

* Bad, because it violates REQ-COST-001 and the one-command reproducibility goal.

## More Information

* Related: [ADR-0012](0012-declarative-topics-three-partitions.md), [ADR-0017](0017-java-21-spring-boot-4-kafka-4.md).
* Requirements: REQ-INST-001, REQ-COST-001, REQ-PORT-001.
