package io.github.kaulinmindiola.rop.inventory.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Everything an order needs reserved, as a single all-or-nothing unit (BR-015). */
public record ReservationRequest(UUID orderId, List<ReservationItem> items) {

    public ReservationRequest {
        if (orderId == null) {
            throw new InvalidReservationException("orderId is required");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidReservationException("a reservation needs at least one item");
        }
        if (items.stream().anyMatch(Objects::isNull)) {
            throw new InvalidReservationException("a reservation cannot contain null items");
        }
        items = List.copyOf(items);
    }
}
