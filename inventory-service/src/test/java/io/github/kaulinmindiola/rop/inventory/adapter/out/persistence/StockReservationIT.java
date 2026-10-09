package io.github.kaulinmindiola.rop.inventory.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kaulinmindiola.rop.inventory.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.inventory.domain.model.RejectionReason;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationItem;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationRequest;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationResult;
import io.github.kaulinmindiola.rop.inventory.domain.port.StockRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * All-or-nothing reservation against a real database (ADR-0010, BR-015). Every test creates its own
 * TEST- products and never touches the seeded SKUs (DI-18).
 */
class StockReservationIT extends AbstractIntegrationTest {

    @Autowired private StockRepository stockRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcClient jdbc;

    /** Product ids sharing a prefix, so their lock order is known: "-a" sorts before "-b". */
    private final String prefix = "TEST-" + UUID.randomUUID();

    private String product(String suffix, int quantity) {
        String productId = prefix + suffix;
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

    private ReservationResult reserveInTransaction(ReservationItem... items) {
        return transactionTemplate.execute(
                status ->
                        stockRepository.tryReserve(
                                new ReservationRequest(UUID.randomUUID(), List.of(items))));
    }

    @Test
    @DisplayName("REQ-FUNC-012 enough stock for every item: reserved and decremented")
    void reservesWhenEveryItemHasStock() {
        String a = product("-a", 5);
        String b = product("-b", 3);

        ReservationResult result =
                reserveInTransaction(new ReservationItem(a, 2), new ReservationItem(b, 3));

        assertThat(result).isEqualTo(new ReservationResult.Reserved());
        assertThat(stockOf(a)).isEqualTo(3);
        assertThat(stockOf(b)).isZero();
    }

    @Test
    @DisplayName(
            "REQ-FUNC-013 not enough stock: rejected with INSUFFICIENT_STOCK and nothing decremented")
    void rejectsInsufficientStock() {
        String a = product("-a", 1);

        ReservationResult result = reserveInTransaction(new ReservationItem(a, 2));

        assertThat(result)
                .isEqualTo(new ReservationResult.Rejected(RejectionReason.INSUFFICIENT_STOCK));
        assertThat(stockOf(a)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "BR-015 a partial reservation is fully undone: the first item's decrement is rolled back")
    void partialReservationIsUndone() {
        String a = product("-a", 5);
        String b = product("-b", 1);

        // a is decremented first (lock order), then b fails.
        ReservationResult result =
                reserveInTransaction(new ReservationItem(b, 2), new ReservationItem(a, 2));

        assertThat(result)
                .isEqualTo(new ReservationResult.Rejected(RejectionReason.INSUFFICIENT_STOCK));
        assertThat(stockOf(a)).isEqualTo(5);
        assertThat(stockOf(b)).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-FUNC-013 a product without a stock row is rejected with UNKNOWN_PRODUCT")
    void rejectsUnknownProduct() {
        String a = product("-a", 5);
        String unknown = prefix + "-z";

        ReservationResult result =
                reserveInTransaction(new ReservationItem(a, 1), new ReservationItem(unknown, 1));

        assertThat(result)
                .isEqualTo(new ReservationResult.Rejected(RejectionReason.UNKNOWN_PRODUCT));
        assertThat(stockOf(a)).isEqualTo(5);
    }

    @Test
    @DisplayName(
            "ADR-0010 a rejection does not abort the caller's transaction: its other writes commit")
    void rejectionCommitsWithTheCallersOtherWrites() {
        String a = product("-a", 5);
        String b = product("-b", 0);
        UUID marker = UUID.randomUUID();

        ReservationResult result =
                transactionTemplate.execute(
                        status -> {
                            jdbc.sql(
                                            "INSERT INTO processed_events (event_id, consumer, processed_at)"
                                                    + " VALUES (?, 'stock-test', now())")
                                    .param(marker)
                                    .update();
                            return stockRepository.tryReserve(
                                    new ReservationRequest(
                                            UUID.randomUUID(),
                                            List.of(
                                                    new ReservationItem(a, 2),
                                                    new ReservationItem(b, 1))));
                        });

        assertThat(result)
                .isEqualTo(new ReservationResult.Rejected(RejectionReason.INSUFFICIENT_STOCK));
        int markers =
                jdbc.sql("SELECT count(*) FROM processed_events WHERE event_id = ?")
                        .param(marker)
                        .query(Integer.class)
                        .single();
        assertThat(markers).isEqualTo(1);
        assertThat(stockOf(a)).isEqualTo(5);
    }

    @Test
    @DisplayName("tryReserve outside a transaction is a programming error and fails loudly")
    void requiresATransaction() {
        String a = product("-a", 5);

        assertThatThrownBy(
                        () ->
                                stockRepository.tryReserve(
                                        new ReservationRequest(
                                                UUID.randomUUID(),
                                                List.of(new ReservationItem(a, 1)))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(stockOf(a)).isEqualTo(5);
    }
}
