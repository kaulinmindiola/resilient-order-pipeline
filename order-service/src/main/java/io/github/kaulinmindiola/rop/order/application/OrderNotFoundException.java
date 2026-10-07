package io.github.kaulinmindiola.rop.order.application;

import java.util.UUID;

/** No order exists with the requested id (mapped to 404). */
public class OrderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId);
    }
}
