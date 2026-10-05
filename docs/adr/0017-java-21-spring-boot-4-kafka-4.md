---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0017: Build on Java 21, Spring Boot 4.1.x and Kafka 4.x

## Context and Problem Statement

The project needs a runtime platform that is still supported when it is reviewed. Java 21 is a hard project constraint. Spring Boot 3.x no longer receives open-source support since 2026-06-30, so choosing it would present an unsupported platform in a portfolio piece. Which platform versions are used?

## Decision Drivers

* Java 21 is a hard constraint, enforced in the build.
* Use a platform with current open-source support.
* Reproducible, centrally managed dependency versions.
* The project demonstrates modern backend engineering to a technical reviewer.

## Considered Options

* Java 21 with Spring Boot 4.1.x and Apache Kafka 4.x
* Java 21 with Spring Boot 3.x
* Java 17 with Spring Boot 3.x

## Decision Outcome

Chosen option: **"Java 21, Spring Boot 4.1.x, Kafka 4.x"**, because it is the supported platform and aligns with the rest of the stack (Spring Framework 7, Hibernate 7, Spring Security 7, Jackson 3, Testcontainers 2.x).

* The parent POM pins the exact Spring Boot 4.1.x version; its BOM is the only source of dependency versions. A version is set by hand only when the BOM does not manage it.
* Maven Enforcer requires Java 21 and Maven 3.9 or newer.
* Kafka runs the `apache/kafka:4.2.2` image in KRaft mode ([ADR-0007](0007-kafka-kraft-single-node.md)).
* Build and runtime images: `maven:3.9-eclipse-temurin-21` and `eclipse-temurin:21-jre`.
* Starters used: `webmvc`, `validation`, `security`, `security-oauth2-resource-server`, `data-jpa`, `jdbc`, `flyway` and `kafka` in `order-service`; `jdbc`, `flyway` and `kafka` in `inventory-service`.

### Consequences

* Good, because the platform is currently supported.
* Good, because the BOM keeps versions consistent and upgrades manageable (Dependabot proposes them).
* Bad, because reference material for Boot 4, Jackson 3 and Testcontainers 2 is scarcer and sometimes outdated. Phase 1 validates the platform with minimal apps before any business logic is written.

### Confirmation

The Maven Enforcer rule fails the build on any other JDK; CI builds on JDK 21; the Phase 1 skeleton starts both services on the chosen versions. Verified in Phase 1.

## Pros and Cons of the Options

### Java 21 with Spring Boot 4.1.x and Kafka 4.x

* Good, because it is supported and modern.
* Bad, because there is less community material.

### Java 21 with Spring Boot 3.x

* Good, because there is more documentation.
* Bad, because it has had no open-source support since 2026-06-30.

### Java 17 with Spring Boot 3.x

* Good, because it is widely used.
* Bad, because it contradicts the Java 21 constraint and shares the unsupported Boot 3.x problem.

## More Information

* Related: [ADR-0007](0007-kafka-kraft-single-node.md), [ADR-0018](0018-jpa-for-order-aggregate-jdbcclient-elsewhere.md).
* Requirements: REQ-COMP-001, REQ-BUILD-001.
