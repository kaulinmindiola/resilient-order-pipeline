package io.github.kaulinmindiola.rop.order.domain.model;

import java.util.Objects;

/**
 * One line of an order. The unit price is a snapshot taken when the order is created and is never
 * recalculated (BR-018).
 */
public record OrderItem(String productId, int quantity, Money unitPrice) {

    public OrderItem {
        if (productId == null || productId.isBlank()) {
            throw new InvalidOrderException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException("quantity must be greater than zero: " + quantity);
        }
        Objects.requireNonNull(unitPrice, "unitPrice");
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
