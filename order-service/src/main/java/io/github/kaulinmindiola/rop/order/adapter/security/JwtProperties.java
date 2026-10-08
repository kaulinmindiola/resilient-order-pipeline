package io.github.kaulinmindiola.rop.order.adapter.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HS256 signing secret, validated at startup (REQ-SEC-001). Error messages name the variable, never
 * its value.
 */
@ConfigurationProperties("security.jwt")
public record JwtProperties(String secret) {

    static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank() || secret.contains("${")) {
            throw new IllegalStateException("JWT_SIGNING_SECRET must be set");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SIGNING_SECRET must be at least " + MIN_SECRET_BYTES + " bytes");
        }
    }
}
