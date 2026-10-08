package io.github.kaulinmindiola.rop.order.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.order.application.CreateOrderCommand;
import io.github.kaulinmindiola.rop.order.application.CreateOrderService;
import io.github.kaulinmindiola.rop.order.domain.port.OutboxEventRepository;
import io.github.kaulinmindiola.rop.order.support.TestKafkaConsumer;
import java.time.Duration;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** ADR-0001: ack-then-mark trades lost events for duplicates, which consumers absorb (ADR-0011). */
class OutboxRepublicationIT extends AbstractIntegrationTest {

    @MockitoSpyBean private OutboxEventRepository outbox;
    @Autowired private OutboxPublisher outboxPublisher;
    @Autowired private CreateOrderService createOrderService;

    private void drainOutbox() {
        while (outboxPublisher.publishPending() > 0) {
            // keep publishing until nothing is pending
        }
    }

    @Test
    @DisplayName(
            "ADR-0001 ADR-0011 if marking fails after the ack, the same event reaches Kafka twice")
    void markFailureRepublishesTheEvent() {
        drainOutbox();
        String orderId =
                createOrderService
                        .create(
                                new CreateOrderCommand(
                                        List.of(new CreateOrderCommand.Item("SKU-001", 1))))
                        .id()
                        .toString();
        doThrow(new IllegalStateException("simulated failure while marking"))
                .doCallRealMethod()
                .when(outbox)
                .markPublished(any(), any());

        assertThat(outboxPublisher.publishPending()).isZero();
        assertThat(outboxPublisher.publishPending()).isEqualTo(1);

        try (TestKafkaConsumer consumer =
                new TestKafkaConsumer(KAFKA.getBootstrapServers(), "order.created")) {
            List<ConsumerRecord<String, String>> records =
                    consumer.poll(
                            record -> orderId.equals(record.key()), 2, Duration.ofSeconds(30));

            assertThat(records).hasSize(2);
            assertThat(records.get(0).value()).isEqualTo(records.get(1).value());
        }
    }
}
