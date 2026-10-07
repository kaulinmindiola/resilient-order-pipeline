package io.github.kaulinmindiola.rop.order.domain.port;

import io.github.kaulinmindiola.rop.order.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Transactional outbox (ADR-0001). The application layer only writes here; publishing to Kafka is
 * entirely an adapter concern, so there is no EventPublisher port.
 */
public interface OutboxEventRepository {

    /** Stores the event in the caller's transaction (BR-008). */
    void save(OutboxEvent event);

    /** Unpublished events, oldest first, at most {@code limit} (REQ-FUNC-010). */
    List<OutboxEvent> findPending(int limit);

    /** Marks an event as published; called only after the broker acknowledged it. */
    void markPublished(UUID eventId, Instant publishedAt);
}
