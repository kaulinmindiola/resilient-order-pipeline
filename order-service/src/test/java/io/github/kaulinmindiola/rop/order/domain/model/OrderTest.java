package io.github.kaulinmindiola.rop.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

    private static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-15T10:00:05Z");

    private static OrderItem item(String productId, int quantity, String unitPrice) {
        return new OrderItem(productId, quantity, Money.of(unitPrice));
    }

    @Test
    @DisplayName("BR-003 a new order starts in PENDING")
    void newOrderStartsInPending() {
        Order order = Order.create(ORDER_ID, List.of(item("SKU-001", 1, "19.99")), NOW);

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("REQ-FUNC-003 the total is the sum of quantity x unit price with scale 2")
    void totalIsTheSumOfSubtotals() {
        Order order =
                Order.create(
                        ORDER_ID,
                        List.of(item("SKU-001", 3, "19.99"), item("SKU-003", 2, "5.50")),
                        NOW);

        assertThat(order.total()).isEqualTo(Money.of("70.97"));
        assertThat(order.total().amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("BR-001 rejects an order with an empty item list")
    void rejectsEmptyItems() {
        assertThatThrownBy(() -> Order.create(ORDER_ID, List.of(), NOW))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("BR-001 rejects an order without an item list")
    void rejectsNullItems() {
        assertThatThrownBy(() -> Order.create(ORDER_ID, null, NOW))
                .isInstanceOf(InvalidOrderException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("BR-002 rejects an item whose quantity is not greater than zero")
    void rejectsNonPositiveQuantity(int quantity) {
        assertThatThrownBy(() -> item("SKU-001", quantity, "19.99"))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("rejects an item without a product id")
    void rejectsBlankProductId() {
        assertThatThrownBy(() -> item(" ", 1, "19.99")).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("BR-018 items are a snapshot that cannot change after creation")
    void itemsAreAnImmutableSnapshot() {
        List<OrderItem> items = new ArrayList<>(List.of(item("SKU-001", 1, "19.99")));
        Order order = Order.create(ORDER_ID, items, NOW);

        items.add(item("SKU-002", 1, "49.90"));

        assertThat(order.items()).hasSize(1);
        assertThat(order.total()).isEqualTo(Money.of("19.99"));
        assertThatThrownBy(() -> order.items().add(item("SKU-002", 1, "49.90")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("creation and update timestamps are the instant supplied by the caller")
    void timestampsComeFromTheCaller() {
        Order order = Order.create(ORDER_ID, List.of(item("SKU-001", 1, "19.99")), NOW);

        assertThat(order.createdAt()).isEqualTo(NOW);
        assertThat(order.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("BR-017 BR-018 reconstitute restores the persisted state without recalculating it")
    void reconstituteRestoresPersistedState() {
        Order order =
                Order.reconstitute(
                        ORDER_ID,
                        OrderStatus.CONFIRMED,
                        List.of(item("SKU-001", 1, "19.99")),
                        Money.of("999.99"),
                        NOW,
                        LATER);

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.total()).isEqualTo(Money.of("999.99"));
        assertThat(order.createdAt()).isEqualTo(NOW);
        assertThat(order.updatedAt()).isEqualTo(LATER);
        assertThat(order.confirm(LATER.plusSeconds(60)))
                .isEqualTo(
                        new TransitionResult.Ignored(OrderStatus.CONFIRMED, OrderStatus.CONFIRMED));
    }
}
