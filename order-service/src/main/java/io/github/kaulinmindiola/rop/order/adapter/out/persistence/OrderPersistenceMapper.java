package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import io.github.kaulinmindiola.rop.order.domain.model.Money;
import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.model.OrderItem;
import java.util.List;
import java.util.UUID;

/** Explicit mapping between the domain aggregate and its JPA representation (ADR-0006). */
final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {}

    /** Maps a new order; item ids are a persistence concern and are assigned here. */
    static OrderJpaEntity toNewEntity(Order order) {
        List<OrderItemJpaEntity> items =
                order.items().stream()
                        .map(
                                item ->
                                        new OrderItemJpaEntity(
                                                UUID.randomUUID(),
                                                item.productId(),
                                                item.quantity(),
                                                item.unitPrice().amount()))
                        .toList();
        return new OrderJpaEntity(
                order.id(),
                order.status(),
                order.total().amount(),
                order.createdAt(),
                order.updatedAt(),
                items);
    }

    /** Must run inside a transaction: items are loaded lazily. */
    static Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items =
                entity.getItems().stream()
                        .map(
                                item ->
                                        new OrderItem(
                                                item.getProductId(),
                                                item.getQuantity(),
                                                new Money(item.getUnitPriceSnapshot())))
                        .toList();
        return Order.reconstitute(
                entity.getId(),
                entity.getStatus(),
                items,
                new Money(entity.getTotal()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
