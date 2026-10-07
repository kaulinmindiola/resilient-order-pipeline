package io.github.kaulinmindiola.rop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kaulinmindiola.rop.order.domain.model.Money;
import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.model.OrderItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetOrderServiceTest {

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final GetOrderService service = new GetOrderService(orders);

    @Test
    @DisplayName("REQ-FUNC-006 an existing order is returned as persisted")
    void returnsExistingOrder() {
        Order order =
                Order.create(
                        UUID.randomUUID(),
                        List.of(new OrderItem("SKU-001", 1, Money.of("19.99"))),
                        Instant.parse("2026-01-15T10:00:00Z"));
        orders.save(order);

        assertThat(service.get(order.id())).isSameAs(order);
    }

    @Test
    @DisplayName("REQ-FUNC-006 an unknown order raises OrderNotFoundException")
    void unknownOrderIsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.get(unknown))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }
}
