package io.github.kaulinmindiola.rop.order.adapter.in.rest.dto;

import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.model.OrderStatus;
import java.util.UUID;

/** Body of the 201 response (REQ-FUNC-005). */
public record CreateOrderResponse(UUID orderId, OrderStatus status) {

    public static CreateOrderResponse from(Order order) {
        return new CreateOrderResponse(order.id(), order.status());
    }
}
