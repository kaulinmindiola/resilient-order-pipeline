package io.github.kaulinmindiola.rop.inventory.domain.model;

/** One product and the quantity to reserve for it. */
public record ReservationItem(String productId, int quantity) {

    public ReservationItem {
        if (productId == null || productId.isBlank()) {
            throw new InvalidReservationException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new InvalidReservationException(
                    "quantity must be greater than zero: " + quantity);
        }
    }
}
