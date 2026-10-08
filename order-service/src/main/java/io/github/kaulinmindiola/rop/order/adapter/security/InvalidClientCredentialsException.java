package io.github.kaulinmindiola.rop.order.adapter.security;

/** Unknown client or wrong secret: deliberately indistinguishable (mapped to 401). */
public class InvalidClientCredentialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidClientCredentialsException() {
        super("Invalid client credentials");
    }
}
