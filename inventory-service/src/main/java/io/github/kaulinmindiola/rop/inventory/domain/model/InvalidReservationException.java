package io.github.kaulinmindiola.rop.inventory.domain.model;

/** A reservation request violates the rules of a valid reservation (BR-015). */
public class InvalidReservationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidReservationException(String message) {
        super(message);
    }
}
