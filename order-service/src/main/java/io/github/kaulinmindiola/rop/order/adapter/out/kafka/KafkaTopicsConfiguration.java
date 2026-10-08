package io.github.kaulinmindiola.rop.order.adapter.out.kafka;

import static io.github.kaulinmindiola.rop.order.adapter.out.kafka.KafkaTopics.DLT_SUFFIX;
import static io.github.kaulinmindiola.rop.order.adapter.out.kafka.KafkaTopics.INVENTORY_REJECTED;
import static io.github.kaulinmindiola.rop.order.adapter.out.kafka.KafkaTopics.INVENTORY_RESERVED;
import static io.github.kaulinmindiola.rop.order.adapter.out.kafka.KafkaTopics.ORDER_CREATED;

import java.util.stream.Stream;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declares the topics this service produces to or consumes from, created at startup by KafkaAdmin
 * (REQ-FUNC-018, ADR-0012). Broker-side auto-creation is disabled.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaTopicsConfiguration {

    @Bean
    KafkaAdmin.NewTopics orderServiceTopics() {
        return new KafkaAdmin.NewTopics(
                Stream.of(
                                ORDER_CREATED,
                                INVENTORY_RESERVED,
                                INVENTORY_REJECTED,
                                INVENTORY_RESERVED + DLT_SUFFIX,
                                INVENTORY_REJECTED + DLT_SUFFIX)
                        .map(KafkaTopicsConfiguration::topic)
                        .toArray(NewTopic[]::new));
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(KafkaTopics.PARTITIONS)
                .replicas(KafkaTopics.REPLICAS)
                .build();
    }
}
