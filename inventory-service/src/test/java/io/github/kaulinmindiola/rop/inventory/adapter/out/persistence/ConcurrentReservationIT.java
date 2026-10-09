package io.github.kaulinmindiola.rop.inventory.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kaulinmindiola.rop.inventory.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.inventory.domain.model.RejectionReason;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationItem;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationRequest;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationResult;
import io.github.kaulinmindiola.rop.inventory.domain.port.StockRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test 11: concurrent reservations never oversell and never deadlock (BR-015, ADR-0010). Every
 * reservation runs in its own thread and transaction; a latch releases them at the same instant.
 */
class ConcurrentReservationIT extends AbstractIntegrationTest {

    private static final int RESERVATIONS = 20;
    private static final ReservationResult RESERVED = new ReservationResult.Reserved();
    private static final ReservationResult INSUFFICIENT =
            new ReservationResult.Rejected(RejectionReason.INSUFFICIENT_STOCK);

    @Autowired private StockRepository stockRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcClient jdbc;

    private String product(int quantity) {
        String productId = "TEST-" + UUID.randomUUID();
        jdbc.sql("INSERT INTO stock (product_id, quantity) VALUES (?, ?)")
                .params(productId, quantity)
                .update();
        return productId;
    }

    private int stockOf(String productId) {
        return jdbc.sql("SELECT quantity FROM stock WHERE product_id = ?")
                .param(productId)
                .query(Integer.class)
                .single();
    }

    /**
     * Runs each reservation in its own thread and transaction, all released at once. Any exception
     * (for example a deadlock) propagates and fails the test.
     */
    private List<ReservationResult> reserveConcurrently(List<List<ReservationItem>> reservations)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(reservations.size());
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<ReservationResult>> futures = new ArrayList<>();
            for (List<ReservationItem> items : reservations) {
                futures.add(
                        executor.submit(
                                () -> {
                                    startGate.await();
                                    return transactionTemplate.execute(
                                            status ->
                                                    stockRepository.tryReserve(
                                                            new ReservationRequest(
                                                                    UUID.randomUUID(), items)));
                                }));
            }
            startGate.countDown();
            List<ReservationResult> results = new ArrayList<>();
            for (Future<ReservationResult> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private static List<List<ReservationItem>> sameReservation(
            int times, ReservationItem... items) {
        return Collections.nCopies(times, List.of(items));
    }

    @Test
    @DisplayName(
            "BR-015 ADR-0010 test 11: of 20 concurrent reservations of the last unit, exactly one succeeds")
    void lastUnitIsReservedExactlyOnce() throws Exception {
        String productId = product(1);

        List<ReservationResult> results =
                reserveConcurrently(
                        sameReservation(RESERVATIONS, new ReservationItem(productId, 1)));

        assertThat(results).filteredOn(RESERVED::equals).hasSize(1);
        assertThat(results).filteredOn(INSUFFICIENT::equals).hasSize(RESERVATIONS - 1);
        assertThat(stockOf(productId)).isZero();
    }

    @Test
    @DisplayName("BR-015 ADR-0010 with stock 5, exactly 5 of 20 concurrent reservations succeed")
    void limitedStockIsReservedExactly() throws Exception {
        String productId = product(5);

        List<ReservationResult> results =
                reserveConcurrently(
                        sameReservation(RESERVATIONS, new ReservationItem(productId, 1)));

        assertThat(results).filteredOn(RESERVED::equals).hasSize(5);
        assertThat(results).filteredOn(INSUFFICIENT::equals).hasSize(RESERVATIONS - 5);
        assertThat(stockOf(productId)).isZero();
    }

    @Test
    @DisplayName(
            "ADR-0010 reservations listing the same products in opposite orders never deadlock")
    void oppositeItemOrdersDoNotDeadlock() throws Exception {
        String a = product(100);
        String b = product(100);
        List<List<ReservationItem>> reservations =
                IntStream.range(0, RESERVATIONS)
                        .mapToObj(
                                i ->
                                        i % 2 == 0
                                                ? List.of(
                                                        new ReservationItem(a, 1),
                                                        new ReservationItem(b, 1))
                                                : List.of(
                                                        new ReservationItem(b, 1),
                                                        new ReservationItem(a, 1)))
                        .toList();

        List<ReservationResult> results = reserveConcurrently(reservations);

        assertThat(results).hasSize(RESERVATIONS).containsOnly(RESERVED);
        assertThat(stockOf(a)).isEqualTo(100 - RESERVATIONS);
        assertThat(stockOf(b)).isEqualTo(100 - RESERVATIONS);
    }
}
