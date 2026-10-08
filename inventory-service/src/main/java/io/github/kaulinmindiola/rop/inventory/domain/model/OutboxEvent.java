package io.github.kaulinmindiola.rop.inventory.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An outgoing event stored in the same transaction as the state change that produced it (BR-008,
 * ADR-0001). The topic is resolved when the event is written. The payload is a technology-neutral
 * map: the domain never sees JSON (DI-14).
 */
public record OutboxEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        String topic,
        Map<String, Object> payload,
        Instant occurredAt) {

    public OutboxEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(occurredAt, "occurredAt");
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
    }
}
