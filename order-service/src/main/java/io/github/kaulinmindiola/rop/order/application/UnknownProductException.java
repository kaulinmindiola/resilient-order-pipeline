package io.github.kaulinmindiola.rop.order.application;

/** A requested product does not exist in the catalog (mapped to 422). */
public class UnknownProductException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnknownProductException(String productId) {
        super("Unknown product: " + productId);
    }
}
