package io.github.kaulinmindiola.rop.order.domain.model;

/**
 * Lifecycle of an order: PENDING is initial, CONFIRMED and REJECTED are terminal (BR-003, BR-017).
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED;

    /** A terminal status accepts no further transitions (BR-017). */
    public boolean isTerminal() {
        return this != PENDING;
    }
}
