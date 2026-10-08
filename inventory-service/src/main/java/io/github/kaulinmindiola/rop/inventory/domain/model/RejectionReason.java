package io.github.kaulinmindiola.rop.inventory.domain.model;

/** Why a reservation was rejected (AI-CONTEXT §6.2, InventoryRejected.reason). */
public enum RejectionReason {
    INSUFFICIENT_STOCK,
    UNKNOWN_PRODUCT
}
