package io.github.kaulinmindiola.rop.order.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.order.application.CreateOrderCommand;
import io.github.kaulinmindiola.rop.order.application.CreateOrderService;
import io.github.kaulinmindiola.rop.order.support.TestKafkaConsumer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Test 2: an outbox event reaches Kafka with the full envelope and is then marked. */
class OutboxPublisherIT extends AbstractIntegrationTest {

    @Autowired private CreateOrderService createOrderService;
    @Autowired private OutboxPublisher outboxPublisher;
    @Autowired private JdbcClient jdbc;

    private void drainOutbox() {
        while (outboxPublisher.publishPending() > 0) {
            // keep publishing until nothing is pending
        }
    }

    @Test
    @DisplayName(
            "REQ-FUNC-010 ADR-0003 an OrderCreated event is published keyed by orderId with the full envelope, then marked")
    void publishesOrderCreatedEvent() {
        String orderId =
                createOrderService
                        .create(
                                new CreateOrderCommand(
                                        List.of(new CreateOrderCommand.Item("SKU-001", 2))))
                        .id()
                        .toString();
        UUID eventId =
                jdbc.sql("SELECT event_id FROM outbox_events WHERE aggregate_id = ?")
                        .param(orderId)
                        .query(UUID.class)
                        .single();

        drainOutbox();

        try (TestKafkaConsumer consumer =
                new TestKafkaConsumer(KAFKA.getBootstrapServers(), "order.created")) {
            List<ConsumerRecord<String, String>> records =
                    consumer.poll(
                            record -> orderId.equals(record.key()), 1, Duration.ofSeconds(30));

            assertThat(records).hasSize(1);
            String envelope = records.getFirst().value();
            assertThat((String) JsonPath.read(envelope, "$.eventId")).isEqualTo(eventId.toString());
            assertThat((String) JsonPath.read(envelope, "$.eventType")).isEqualTo("OrderCreated");
            assertThat((String) JsonPath.read(envelope, "$.aggregateId")).isEqualTo(orderId);
            assertThat((String) JsonPath.read(envelope, "$.occurredAt")).endsWith("Z");
            assertThat((String) JsonPath.read(envelope, "$.payload.orderId")).isEqualTo(orderId);
            assertThat((String) JsonPath.read(envelope, "$.payload.items[0].productId"))
                    .isEqualTo("SKU-001");
            assertThat((Integer) JsonPath.read(envelope, "$.payload.items[0].quantity"))
                    .isEqualTo(2);
        }

        OffsetDateTime publishedAt =
                jdbc.sql("SELECT published_at FROM outbox_events WHERE event_id = ?")
                        .param(eventId)
                        .query(OffsetDateTime.class)
                        .single();
        assertThat(publishedAt).isNotNull();
    }
}
