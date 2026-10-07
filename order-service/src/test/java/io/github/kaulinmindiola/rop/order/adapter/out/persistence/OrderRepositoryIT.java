package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.order.domain.model.Money;
import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.model.OrderItem;
import io.github.kaulinmindiola.rop.order.domain.model.OrderStatus;
import io.github.kaulinmindiola.rop.order.domain.port.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class OrderRepositoryIT extends AbstractIntegrationTest {

    private static final Instant CREATED = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-15T10:00:05Z");

    @Autowired private OrderRepository orderRepository;
    @Autowired private JdbcClient jdbc;

    private static Order newOrder() {
        return Order.create(
                UUID.randomUUID(),
                List.of(
                        new OrderItem("SKU-001", 3, Money.of("19.99")),
                        new OrderItem("SKU-003", 2, Money.of("5.50"))),
                CREATED);
    }

    private List<UUID> itemIdsOf(UUID orderId) {
        return jdbc.sql("SELECT id FROM order_items WHERE order_id = ? ORDER BY id")
                .param(orderId)
                .query(UUID.class)
                .list();
    }

    @Test
    @DisplayName("ADR-0018 an order survives a round trip through JPA unchanged")
    void roundTrip() {
        Order order = newOrder();

        orderRepository.save(order);
        Order loaded = orderRepository.findById(order.id()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(loaded.total()).isEqualTo(Money.of("70.97"));
        assertThat(loaded.items()).containsExactlyInAnyOrderElementsOf(order.items());
        assertThat(loaded.createdAt()).isEqualTo(CREATED);
        assertThat(loaded.updatedAt()).isEqualTo(CREATED);
    }

    @Test
    @DisplayName("BR-018 a status change updates the order without rewriting its items")
    void statusChangeKeepsItems() {
        Order order = newOrder();
        orderRepository.save(order);
        List<UUID> itemIdsBefore = itemIdsOf(order.id());

        Order loaded = orderRepository.findById(order.id()).orElseThrow();
        loaded.confirm(LATER);
        orderRepository.save(loaded);

        Order reloaded = orderRepository.findById(order.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(reloaded.updatedAt()).isEqualTo(LATER);
        assertThat(reloaded.createdAt()).isEqualTo(CREATED);
        assertThat(itemIdsOf(order.id())).hasSize(2).isEqualTo(itemIdsBefore);
    }

    @Test
    @DisplayName("finding an unknown order returns empty")
    void unknownOrderIsEmpty() {
        assertThat(orderRepository.findById(UUID.randomUUID())).isEmpty();
    }
}
