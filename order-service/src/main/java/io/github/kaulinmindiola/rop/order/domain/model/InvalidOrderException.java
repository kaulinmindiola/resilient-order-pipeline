package io.github.kaulinmindiola.rop.order.domain.model;

/** A business rule for creating an order was violated (BR-001, BR-002). */
public class InvalidOrderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidOrderException(String message) {
        super(message);
    }
}
