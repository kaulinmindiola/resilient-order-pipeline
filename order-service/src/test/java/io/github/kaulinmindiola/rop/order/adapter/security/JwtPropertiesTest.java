package io.github.kaulinmindiola.rop.order.adapter.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    @DisplayName("REQ-SEC-001 a secret shorter than 32 bytes is rejected without revealing it")
    void rejectsShortSecret() {
        String shortSecret = "a".repeat(31);

        assertThatThrownBy(() -> new JwtProperties(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SIGNING_SECRET")
                .hasMessageNotContaining(shortSecret);
    }

    @Test
    @DisplayName("REQ-SEC-001 a missing or unresolved secret is rejected")
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> new JwtProperties(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties("${JWT_SIGNING_SECRET}-padding-to-32-bytes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SIGNING_SECRET must be set");
    }

    @Test
    @DisplayName("REQ-SEC-001 a 32-byte secret is accepted")
    void acceptsMinimumLength() {
        assertThatCode(() -> new JwtProperties("a".repeat(32))).doesNotThrowAnyException();
    }
}
