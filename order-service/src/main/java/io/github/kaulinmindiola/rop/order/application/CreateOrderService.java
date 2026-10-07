package io.github.kaulinmindiola.rop.order.application;

import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.model.OrderItem;
import io.github.kaulinmindiola.rop.order.domain.model.OutboxEvent;
import io.github.kaulinmindiola.rop.order.domain.port.OrderRepository;
import io.github.kaulinmindiola.rop.order.domain.port.OutboxEventRepository;
import io.github.kaulinmindiola.rop.order.domain.port.ProductCatalog;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates an order and its OrderCreated outbox event in a single local transaction (REQ-FUNC-004,
 * BR-008). It never talks to Kafka: publishing is an adapter concern (ADR-0001).
 */
@Service
public class CreateOrderService {

    private final ProductCatalog productCatalog;
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    public CreateOrderService(
            ProductCatalog productCatalog,
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            Clock clock) {
        this.productCatalog = productCatalog;
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.clock = clock;
    }

    @Transactional
    public Order create(CreateOrderCommand command) {
        List<OrderItem> items = command.items().stream().map(this::pricedItem).toList();
        Instant now = clock.instant();
        Order order = Order.create(UUID.randomUUID(), items, now);

        orderRepository.save(order);
        outboxEventRepository.save(orderCreated(order, now));
        return order;
    }

    /** The price is always taken from the catalog, never from the client (ADR-0014, BR-018). */
    private OrderItem pricedItem(CreateOrderCommand.Item item) {
        return productCatalog
                .priceOf(item.productId())
                .map(price -> new OrderItem(item.productId(), item.quantity(), price))
                .orElseThrow(() -> new UnknownProductException(item.productId()));
    }

    /** OrderCreated contract (AI-CONTEXT §6.2): orderId and items, without prices. */
    private static OutboxEvent orderCreated(Order order, Instant now) {
        String orderId = order.id().toString();
        List<Map<String, Object>> items =
                order.items().stream()
                        .map(
                                item ->
                                        Map.<String, Object>of(
                                                "productId", item.productId(),
                                                "quantity", item.quantity()))
                        .toList();
        return new OutboxEvent(
                UUID.randomUUID(),
                OrderEvents.ORDER_CREATED,
                orderId,
                OrderEvents.ORDER_CREATED_TOPIC,
                Map.of("orderId", orderId, "items", items),
                now);
    }
}
