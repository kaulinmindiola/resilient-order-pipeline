package io.github.kaulinmindiola.rop.order.domain.model;

/**
 * Outcome of a requested status change. An invalid transition is a result, never an exception
 * (BR-014), so duplicate or late events can be acknowledged as no-ops.
 */
public sealed interface TransitionResult {

    /** The order moved from one status to another. */
    record Applied(OrderStatus from, OrderStatus to) implements TransitionResult {}

    /** The order was already in a terminal status; nothing changed. */
    record Ignored(OrderStatus current, OrderStatus requested) implements TransitionResult {}
}
