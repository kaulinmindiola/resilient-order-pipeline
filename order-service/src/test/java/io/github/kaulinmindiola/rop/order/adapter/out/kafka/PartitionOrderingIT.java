package io.github.kaulinmindiola.rop.order.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.order.domain.model.OutboxEvent;
import io.github.kaulinmindiola.rop.order.domain.port.OutboxEventRepository;
import io.github.kaulinmindiola.rop.order.support.TestKafkaConsumer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test 13: key = aggregateId keeps each aggregate's events in one partition, in order (ADR-0003).
 */
class PartitionOrderingIT extends AbstractIntegrationTest {

    private static final int EVENTS_PER_AGGREGATE = 5;
    private static final Instant BASE = Instant.parse("2026-01-15T11:00:00Z");

    @Autowired private OutboxEventRepository outbox;
    @Autowired private OutboxPublisher outboxPublisher;

    private void drainOutbox() {
        while (outboxPublisher.publishPending() > 0) {
            // keep publishing until nothing is pending
        }
    }

    @Test
    @DisplayName(
            "ADR-0003 ADR-0012 events of one aggregate land in a single partition and are consumed in order")
    void eventsOfOneAggregateShareAPartitionInOrder() {
        List<String> aggregates =
                IntStream.range(0, 3).mapToObj(i -> "aggregate-" + UUID.randomUUID()).toList();
        int occurrence = 0;
        for (int sequence = 0; sequence < EVENTS_PER_AGGREGATE; sequence++) {
            for (String aggregate : aggregates) {
                outbox.save(
                        new OutboxEvent(
                                UUID.randomUUID(),
                                "OrderCreated",
                                aggregate,
                                "order.created",
                                Map.of("orderId", aggregate, "sequence", sequence),
                                BASE.plusMillis(occurrence++)));
            }
        }

        drainOutbox();

        try (TestKafkaConsumer consumer =
                new TestKafkaConsumer(KAFKA.getBootstrapServers(), "order.created")) {
            List<ConsumerRecord<String, String>> records =
                    consumer.poll(
                            record -> aggregates.contains(record.key()),
                            aggregates.size() * EVENTS_PER_AGGREGATE,
                            Duration.ofSeconds(30));
            assertThat(records).hasSize(aggregates.size() * EVENTS_PER_AGGREGATE);

            Map<String, List<ConsumerRecord<String, String>>> byAggregate =
                    records.stream().collect(Collectors.groupingBy(ConsumerRecord::key));
            assertThat(byAggregate)
                    .allSatisfy(
                            (aggregate, events) -> {
                                assertThat(events)
                                        .extracting(ConsumerRecord::partition)
                                        .containsOnly(events.getFirst().partition());
                                assertThat(events)
                                        .extracting(
                                                event ->
                                                        (Integer)
                                                                JsonPath.read(
                                                                        event.value(),
                                                                        "$.payload.sequence"))
                                        .containsExactly(0, 1, 2, 3, 4);
                            });
        }
    }
}
