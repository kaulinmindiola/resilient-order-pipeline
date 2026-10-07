package io.github.kaulinmindiola.rop.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    @DisplayName("BR-008 the payload is an immutable copy of the caller's map")
    void payloadIsAnImmutableCopy() {
        Map<String, Object> source = new HashMap<>(Map.of("orderId", "abc"));
        OutboxEvent event =
                new OutboxEvent(EVENT_ID, "OrderCreated", "abc", "order.created", source, NOW);

        source.put("tampered", true);

        assertThat(event.payload()).containsOnlyKeys("orderId");
        assertThatThrownBy(() -> event.payload().put("x", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("every field of an outbox event is mandatory")
    void everyFieldIsMandatory() {
        assertThatThrownBy(
                        () -> new OutboxEvent(EVENT_ID, "OrderCreated", "abc", null, Map.of(), NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("topic");
    }
}
