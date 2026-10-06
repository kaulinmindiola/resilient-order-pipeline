package io.github.kaulinmindiola.rop.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderStateMachineTest {

    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-15T10:00:05Z");
    private static final Instant MUCH_LATER = Instant.parse("2026-01-15T10:05:00Z");

    private static Order pendingOrder() {
        return Order.create(
                ORDER_ID, List.of(new OrderItem("SKU-001", 1, Money.of("19.99"))), CREATED);
    }

    private static Order orderIn(OrderStatus status) {
        Order order = pendingOrder();
        switch (status) {
            case PENDING -> {}
            case CONFIRMED -> order.confirm(LATER);
            case REJECTED -> order.reject(LATER);
        }
        return order;
    }

    @Test
    @DisplayName("confirm moves a PENDING order to CONFIRMED")
    void confirmMovesPendingToConfirmed() {
        Order order = pendingOrder();

        TransitionResult result = order.confirm(LATER);

        assertThat(result)
                .isEqualTo(
                        new TransitionResult.Applied(OrderStatus.PENDING, OrderStatus.CONFIRMED));
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.updatedAt()).isEqualTo(LATER);
        assertThat(order.createdAt()).isEqualTo(CREATED);
    }

    @Test
    @DisplayName("reject moves a PENDING order to REJECTED")
    void rejectMovesPendingToRejected() {
        Order order = pendingOrder();

        TransitionResult result = order.reject(LATER);

        assertThat(result)
                .isEqualTo(new TransitionResult.Applied(OrderStatus.PENDING, OrderStatus.REJECTED));
        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.updatedAt()).isEqualTo(LATER);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"CONFIRMED", "REJECTED"})
    @DisplayName("BR-014 BR-017 confirm on a terminal order is an ignored no-op")
    void confirmOnTerminalOrderIsIgnored(OrderStatus terminal) {
        Order order = orderIn(terminal);

        TransitionResult result = order.confirm(MUCH_LATER);

        assertThat(result).isEqualTo(new TransitionResult.Ignored(terminal, OrderStatus.CONFIRMED));
        assertThat(order.status()).isEqualTo(terminal);
        assertThat(order.updatedAt()).isEqualTo(LATER);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"CONFIRMED", "REJECTED"})
    @DisplayName("BR-014 BR-017 reject on a terminal order is an ignored no-op")
    void rejectOnTerminalOrderIsIgnored(OrderStatus terminal) {
        Order order = orderIn(terminal);

        TransitionResult result = order.reject(MUCH_LATER);

        assertThat(result).isEqualTo(new TransitionResult.Ignored(terminal, OrderStatus.REJECTED));
        assertThat(order.status()).isEqualTo(terminal);
        assertThat(order.updatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("BR-017 only PENDING accepts transitions")
    void onlyPendingIsNonTerminal() {
        assertThat(OrderStatus.PENDING.isTerminal()).isFalse();
        assertThat(OrderStatus.CONFIRMED.isTerminal()).isTrue();
        assertThat(OrderStatus.REJECTED.isTerminal()).isTrue();
    }
}
