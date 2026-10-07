package io.github.kaulinmindiola.rop.order.adapter.in.rest.dto;

import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Body of GET /api/v1/orders/{id} (REQ-FUNC-006). */
public record OrderResponse(
        UUID orderId, OrderStatus status, List<ItemResponse> items, BigDecimal total) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id(),
                order.status(),
                order.items().stream()
                        .map(
                                item ->
                                        new ItemResponse(
                                                item.productId(),
                                                item.quantity(),
                                                item.unitPrice().amount()))
                        .toList(),
                order.total().amount());
    }
}
