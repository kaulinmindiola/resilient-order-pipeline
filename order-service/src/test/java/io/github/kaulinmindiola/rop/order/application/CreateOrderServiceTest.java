package io.github.kaulinmindiola.rop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kaulinmindiola.rop.order.domain.model.InvalidOrderException;
import io.github.kaulinmindiola.rop.order.domain.model.Money;
import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.model.OrderItem;
import io.github.kaulinmindiola.rop.order.domain.model.OrderStatus;
import io.github.kaulinmindiola.rop.order.domain.model.OutboxEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Map<String, Money> PRICES =
            Map.of("SKU-001", Money.of("19.99"), "SKU-003", Money.of("5.50"));

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final RecordingOutboxEventRepository outbox = new RecordingOutboxEventRepository();
    private final CreateOrderService service =
            new CreateOrderService(
                    productId -> Optional.ofNullable(PRICES.get(productId)),
                    orders,
                    outbox,
                    Clock.fixed(NOW, ZoneOffset.UTC));

    private static CreateOrderCommand command(CreateOrderCommand.Item... items) {
        return new CreateOrderCommand(List.of(items));
    }

    private static CreateOrderCommand.Item item(String productId, int quantity) {
        return new CreateOrderCommand.Item(productId, quantity);
    }

    @Test
    @DisplayName(
            "BR-003 BR-018 a new order is PENDING, timestamped by the clock and priced from the catalog")
    void createsPendingOrderPricedFromCatalog() {
        Order order = service.create(command(item("SKU-001", 3), item("SKU-003", 2)));

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.createdAt()).isEqualTo(NOW);
        assertThat(order.total()).isEqualTo(Money.of("70.97"));
        assertThat(order.items())
                .containsExactly(
                        new OrderItem("SKU-001", 3, Money.of("19.99")),
                        new OrderItem("SKU-003", 2, Money.of("5.50")));
        assertThat(orders.findById(order.id())).contains(order);
    }

    @Test
    @DisplayName("REQ-FUNC-004 BR-008 the order is written together with its OrderCreated event")
    void writesOrderCreatedEvent() {
        Order order = service.create(command(item("SKU-001", 3), item("SKU-003", 2)));

        assertThat(outbox.saved()).hasSize(1);
        OutboxEvent event = outbox.saved().getFirst();
        assertThat(event.eventType()).isEqualTo(OrderEvents.ORDER_CREATED);
        assertThat(event.topic()).isEqualTo(OrderEvents.ORDER_CREATED_TOPIC);
        assertThat(event.aggregateId()).isEqualTo(order.id().toString());
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.payload())
                .isEqualTo(
                        Map.of(
                                "orderId",
                                order.id().toString(),
                                "items",
                                List.of(
                                        Map.of("productId", "SKU-001", "quantity", 3),
                                        Map.of("productId", "SKU-003", "quantity", 2))));
    }

    @Test
    @DisplayName("ADR-0014 an unknown product is rejected and nothing is written")
    void unknownProductIsRejected() {
        assertThatThrownBy(() -> service.create(command(item("SKU-001", 1), item("SKU-999", 1))))
                .isInstanceOf(UnknownProductException.class)
                .hasMessageContaining("SKU-999");

        assertThat(orders.size()).isZero();
        assertThat(outbox.saved()).isEmpty();
    }

    @Test
    @DisplayName("BR-001 an order without items is rejected and nothing is written")
    void emptyOrderIsRejected() {
        assertThatThrownBy(() -> service.create(new CreateOrderCommand(null)))
                .isInstanceOf(InvalidOrderException.class);

        assertThat(orders.size()).isZero();
        assertThat(outbox.saved()).isEmpty();
    }

    @Test
    @DisplayName("BR-002 an item with a non-positive quantity is rejected and nothing is written")
    void nonPositiveQuantityIsRejected() {
        assertThatThrownBy(() -> service.create(command(item("SKU-001", 0))))
                .isInstanceOf(InvalidOrderException.class);

        assertThat(orders.size()).isZero();
        assertThat(outbox.saved()).isEmpty();
    }
}
