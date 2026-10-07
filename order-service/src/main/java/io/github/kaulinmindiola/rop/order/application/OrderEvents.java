package io.github.kaulinmindiola.rop.order.application;

/**
 * Event types written by this service and the topic each one is published to. The topic is resolved
 * when the event is written to the outbox (AI-CONTEXT §6.1).
 */
public final class OrderEvents {

    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_CREATED_TOPIC = "order.created";

    private OrderEvents() {}
}
