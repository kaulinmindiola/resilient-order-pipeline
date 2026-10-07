package io.github.kaulinmindiola.rop.order.application;

import io.github.kaulinmindiola.rop.order.domain.model.OutboxEvent;
import io.github.kaulinmindiola.rop.order.domain.port.OutboxEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Records outbox writes; the use cases under test never read or publish. */
final class RecordingOutboxEventRepository implements OutboxEventRepository {

    private final List<OutboxEvent> saved = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
        saved.add(event);
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        throw new UnsupportedOperationException("not used by the use cases under test");
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        throw new UnsupportedOperationException("not used by the use cases under test");
    }

    List<OutboxEvent> saved() {
        return List.copyOf(saved);
    }
}
