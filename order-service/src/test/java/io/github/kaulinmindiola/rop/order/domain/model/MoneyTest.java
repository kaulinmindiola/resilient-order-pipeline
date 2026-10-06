package io.github.kaulinmindiola.rop.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    @DisplayName("REQ-FUNC-003 normalizes the scale to 2 so equal amounts are equal")
    void normalizesScale() {
        assertThat(Money.of("2")).isEqualTo(Money.of("2.00"));
        assertThat(Money.of("2").amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("rejects negative amounts")
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> Money.of("-0.01")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects amounts with more than two decimals instead of rounding silently")
    void rejectsMoreThanTwoDecimals() {
        assertThatThrownBy(() -> Money.of("19.999")).isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("adds and multiplies exactly")
    void addsAndMultipliesExactly() {
        assertThat(Money.of("0.10").add(Money.of("0.20"))).isEqualTo(Money.of("0.30"));
        assertThat(Money.of("19.99").multiply(3)).isEqualTo(Money.of("59.97"));
    }
}
