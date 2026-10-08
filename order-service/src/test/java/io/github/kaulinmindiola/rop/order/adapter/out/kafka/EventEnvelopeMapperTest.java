package io.github.kaulinmindiola.rop.order.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kaulinmindiola.rop.order.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class EventEnvelopeMapperTest {

    private static final TypeReference<Map<String, Object>> ENVELOPE_TYPE =
            new TypeReference<>() {};

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final EventEnvelopeMapper mapper = new EventEnvelopeMapper(jsonMapper);

    @Test
    @DisplayName(
            "REQ-MAINT-002 the envelope carries eventId, eventType, ISO-8601 UTC occurredAt, aggregateId and payload")
    void buildsEnvelopeAccordingToContract() {
        UUID eventId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Map<String, Object> payload =
                Map.of(
                        "orderId",
                        "order-a",
                        "items",
                        List.of(Map.of("productId", "SKU-001", "quantity", 3)));
        OutboxEvent event =
                new OutboxEvent(
                        eventId,
                        "OrderCreated",
                        "order-a",
                        "order.created",
                        payload,
                        Instant.parse("2026-01-15T10:00:00Z"));

        Map<String, Object> envelope =
                jsonMapper.readValue(mapper.toEnvelopeJson(event), ENVELOPE_TYPE);

        assertThat(envelope)
                .containsOnlyKeys("eventId", "eventType", "occurredAt", "aggregateId", "payload")
                .containsEntry("eventId", eventId.toString())
                .containsEntry("eventType", "OrderCreated")
                .containsEntry("occurredAt", "2026-01-15T10:00:00Z")
                .containsEntry("aggregateId", "order-a")
                .containsEntry("payload", payload);
    }
}
