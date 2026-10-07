package io.github.kaulinmindiola.rop.order.domain.port;

import io.github.kaulinmindiola.rop.order.domain.model.Order;
import java.util.Optional;
import java.util.UUID;

/** Persistence of the order aggregate (ADR-0018: implemented with JPA). */
public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(UUID id);
}
