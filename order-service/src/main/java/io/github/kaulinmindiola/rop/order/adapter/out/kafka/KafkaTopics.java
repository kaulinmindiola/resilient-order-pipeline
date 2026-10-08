package io.github.kaulinmindiola.rop.order.adapter.out.kafka;

import io.github.kaulinmindiola.rop.order.application.OrderEvents;

/** Topic names used by this service, defined once (AI-CONTEXT §6.1). */
public final class KafkaTopics {

    public static final String ORDER_CREATED = OrderEvents.ORDER_CREATED_TOPIC;
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_REJECTED = "inventory.rejected";
    public static final String DLT_SUFFIX = ".DLT";

    public static final int PARTITIONS = 3;
    public static final int REPLICAS = 1;

    private KafkaTopics() {}
}
