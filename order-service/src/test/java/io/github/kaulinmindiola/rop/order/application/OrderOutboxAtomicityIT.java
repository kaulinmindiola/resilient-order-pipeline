package io.github.kaulinmindiola.rop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.model.OutboxEvent;
import io.github.kaulinmindiola.rop.order.domain.port.OutboxEventRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Test 1: the order and its outbox event are committed or rolled back together (BR-008). */
class OrderOutboxAtomicityIT extends AbstractIntegrationTest {

    /**
     * Dedicated failure for the simulation. It must not be an IllegalStateException or
     * IllegalArgumentException: Spring's persistence exception translation would rewrap those as
     * InvalidDataAccessApiUsageException when they leave a persistence adapter.
     */
    private static final class SimulatedOutboxFailure extends RuntimeException {

        private static final long serialVersionUID = 1L;

        SimulatedOutboxFailure(String message) {
            super(message);
        }
    }

    @Autowired private CreateOrderService createOrderService;
    @Autowired private JdbcClient jdbc;
    @MockitoSpyBean private OutboxEventRepository outboxEventRepository;

    private static CreateOrderCommand twoItemOrder() {
        return new CreateOrderCommand(
                List.of(
                        new CreateOrderCommand.Item("SKU-001", 3),
                        new CreateOrderCommand.Item("SKU-003", 2)));
    }

    private int count(String table, String column, Object value) {
        return jdbc.sql("SELECT count(*) FROM " + table + " WHERE " + column + " = ?")
                .param(value)
                .query(Integer.class)
                .single();
    }

    @Test
    @DisplayName(
            "REQ-FUNC-004 BR-008 the order, its items and its outbox event are committed together")
    void commitsOrderAndEventTogether() {
        Order order = createOrderService.create(twoItemOrder());

        assertThat(count("orders", "id", order.id())).isEqualTo(1);
        assertThat(count("order_items", "order_id", order.id())).isEqualTo(2);
        assertThat(count("outbox_events", "aggregate_id", order.id().toString())).isEqualTo(1);

        String eventRow =
                jdbc.sql(
                                "SELECT event_type || '|' || topic FROM outbox_events"
                                        + " WHERE aggregate_id = ?")
                        .param(order.id().toString())
                        .query(String.class)
                        .single();
        assertThat(eventRow).isEqualTo("OrderCreated|order.created");
    }

    @Test
    @DisplayName(
            "REQ-FUNC-004 BR-008 if the outbox write fails, the already-written order is rolled back")
    void rollsBackOrderWhenOutboxWriteFails() {
        AtomicReference<UUID> orderId = new AtomicReference<>();
        AtomicInteger ordersVisibleInsideTransaction = new AtomicInteger(-1);
        doAnswer(
                        invocation -> {
                            OutboxEvent event = invocation.getArgument(0);
                            orderId.set(UUID.fromString(event.aggregateId()));
                            ordersVisibleInsideTransaction.set(
                                    count("orders", "id", orderId.get()));
                            throw new SimulatedOutboxFailure("simulated outbox failure");
                        })
                .when(outboxEventRepository)
                .save(any());

        assertThatThrownBy(() -> createOrderService.create(twoItemOrder()))
                .isInstanceOf(SimulatedOutboxFailure.class)
                .hasMessage("simulated outbox failure");

        // The order had really been written inside the transaction (DI-16)...
        assertThat(ordersVisibleInsideTransaction.get()).isEqualTo(1);
        // ...and the rollback removed it together with everything else.
        assertThat(count("orders", "id", orderId.get())).isZero();
        assertThat(count("order_items", "order_id", orderId.get())).isZero();
        assertThat(count("outbox_events", "aggregate_id", orderId.get().toString())).isZero();
    }
}
