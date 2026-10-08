package io.github.kaulinmindiola.rop.order.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.order.support.TestKafkaConsumer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Test 7 (integration): a Kafka outage never loses a persisted order (REQ-FUNC-011).
 *
 * <p>The broker is paused, not stopped, so it keeps its mapped port: to the client it is a broker
 * that does not answer, detectable only by timeout. A timed-out send is ambiguous: the request may
 * still be written when the broker resumes, and the publisher, which did not mark the event, sends
 * it again. Delivery is therefore at-least-once: the test proves that no order is lost and that a
 * duplicate is the same event, which idempotent consumers absorb (ADR-0001, ADR-0011).
 */
class OutboxKafkaOutageIT extends AbstractIntegrationTest {

    @Autowired private OutboxPublisher outboxPublisher;
    @Autowired private JdbcClient jdbc;

    private boolean kafkaPaused;

    @AfterEach
    void resumeKafka() {
        if (kafkaPaused) {
            unpauseKafka();
        }
    }

    private void pauseKafka() {
        KAFKA.getDockerClient().pauseContainerCmd(KAFKA.getContainerId()).exec();
        kafkaPaused = true;
    }

    private void unpauseKafka() {
        KAFKA.getDockerClient().unpauseContainerCmd(KAFKA.getContainerId()).exec();
        kafkaPaused = false;
    }

    private void drainOutbox() {
        while (outboxPublisher.publishPending() > 0) {
            // keep publishing until nothing is pending
        }
    }

    private String createOrder(String token) {
        MvcTestResult result =
                mvc.post()
                        .uri("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"SKU-001\",\"quantity\":1}]}")
                        .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private int outboxEventsOf(List<String> orderIds, boolean pendingOnly) {
        return jdbc.sql(
                        "SELECT count(*) FROM outbox_events WHERE aggregate_id IN (:ids)"
                                + (pendingOnly ? " AND published_at IS NULL" : ""))
                .param("ids", orderIds)
                .query(Integer.class)
                .single();
    }

    @Test
    @DisplayName(
            "REQ-FUNC-011 with Kafka down orders are still created, and every event reaches Kafka once it returns")
    void ordersSurviveKafkaOutage() {
        drainOutbox();
        String token = bearerToken();

        pauseKafka();
        List<String> orderIds = List.of(createOrder(token), createOrder(token));

        assertThat(outboxPublisher.publishPending()).isZero();
        assertThat(outboxEventsOf(orderIds, true)).isEqualTo(2);

        unpauseKafka();
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> {
                            outboxPublisher.publishPending();
                            assertThat(outboxEventsOf(orderIds, true)).isZero();
                        });

        // One outbox event per order: any duplicate below is a redelivery, not a second event.
        assertThat(outboxEventsOf(orderIds, false)).isEqualTo(2);

        try (TestKafkaConsumer consumer =
                new TestKafkaConsumer(KAFKA.getBootstrapServers(), "order.created")) {
            List<ConsumerRecord<String, String>> records =
                    consumer.pollUntil(
                            record -> orderIds.contains(record.key()),
                            received ->
                                    received.stream()
                                            .map(ConsumerRecord::key)
                                            .collect(Collectors.toSet())
                                            .containsAll(orderIds),
                            Duration.ofSeconds(30));

            Map<String, Set<String>> eventIdsByOrder =
                    records.stream()
                            .collect(
                                    Collectors.groupingBy(
                                            ConsumerRecord::key,
                                            Collectors.mapping(
                                                    record ->
                                                            JsonPath.<String>read(
                                                                    record.value(), "$.eventId"),
                                                    Collectors.toSet())));

            // No order is lost, and every copy of an order's event is the same event.
            assertThat(eventIdsByOrder)
                    .containsOnlyKeys(orderIds)
                    .allSatisfy((orderId, eventIds) -> assertThat(eventIds).hasSize(1));
        }
    }
}
