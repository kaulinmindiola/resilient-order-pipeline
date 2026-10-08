package io.github.kaulinmindiola.rop.inventory.domain.port;

import java.time.Instant;
import java.util.UUID;

/** Consumer-side idempotency keyed by (event_id, consumer) (REQ-FUNC-016, ADR-0011). */
public interface ProcessedEventRepository {

    /**
     * Records that {@code consumer} is processing {@code eventId} in the caller's transaction.
     *
     * @return {@code true} the first time, {@code false} if the event was already processed
     */
    boolean claim(UUID eventId, String consumer, Instant processedAt);
}
