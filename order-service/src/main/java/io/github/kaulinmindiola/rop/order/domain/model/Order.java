package io.github.kaulinmindiola.rop.order.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Order aggregate. The only way to create a new order is {@link #create}, which guarantees that
 * every instance satisfies BR-001, BR-002, BR-003 and BR-018. Status changes go through {@link
 * #confirm} and {@link #reject}, which only act on a PENDING order (BR-014, BR-017).
 */
public final class Order {

    private final UUID id;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final Money total;
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(
            UUID id,
            OrderStatus status,
            List<OrderItem> items,
            Money total,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.status = status;
        this.items = items;
        this.total = total;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a new order in PENDING. The id and the current instant are supplied by the caller so
     * that the domain stays deterministic (DI-08).
     */
    public static Order create(UUID id, List<OrderItem> items, Instant now) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(now, "now");
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("An order must contain at least one item");
        }
        List<OrderItem> snapshot = List.copyOf(items);
        Money total = snapshot.stream().map(OrderItem::subtotal).reduce(Money.ZERO, Money::add);
        return new Order(id, OrderStatus.PENDING, snapshot, total, now, now);
    }

    /** PENDING → CONFIRMED; a no-op on a terminal order. */
    public TransitionResult confirm(Instant now) {
        return transitionTo(OrderStatus.CONFIRMED, now);
    }

    /** PENDING → REJECTED; a no-op on a terminal order. */
    public TransitionResult reject(Instant now) {
        return transitionTo(OrderStatus.REJECTED, now);
    }

    private TransitionResult transitionTo(OrderStatus target, Instant now) {
        Objects.requireNonNull(now, "now");
        if (status.isTerminal()) {
            return new TransitionResult.Ignored(status, target);
        }
        OrderStatus from = status;
        status = target;
        updatedAt = now;
        return new TransitionResult.Applied(from, target);
    }

    public UUID id() {
        return id;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderItem> items() {
        return items;
    }

    public Money total() {
        return total;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
