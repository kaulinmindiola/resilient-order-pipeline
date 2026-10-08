package io.github.kaulinmindiola.rop.inventory.domain.model;

import java.util.Objects;

/**
 * Outcome of a reservation attempt. A rejection is a business result, never an error: it is
 * committed together with its InventoryRejected event (ADR-0010).
 */
public sealed interface ReservationResult {

    /** Every item was reserved. */
    record Reserved() implements ReservationResult {}

    /** Nothing was reserved, for the given reason. */
    record Rejected(RejectionReason reason) implements ReservationResult {

        public Rejected {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
