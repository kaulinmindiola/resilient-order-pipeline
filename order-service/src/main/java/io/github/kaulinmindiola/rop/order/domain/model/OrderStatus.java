package io.github.kaulinmindiola.rop.order.domain.model;

/**
 * Lifecycle of an order: PENDING is initial, CONFIRMED and REJECTED are terminal (BR-003, BR-017).
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}
