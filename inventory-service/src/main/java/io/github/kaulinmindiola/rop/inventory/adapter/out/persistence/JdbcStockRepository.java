package io.github.kaulinmindiola.rop.inventory.adapter.out.persistence;

import io.github.kaulinmindiola.rop.inventory.domain.model.RejectionReason;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationItem;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationRequest;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationResult;
import io.github.kaulinmindiola.rop.inventory.domain.port.StockRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * All-or-nothing stock reservation with a conditional UPDATE per item (ADR-0010, BR-015).
 *
 * <p>Atomicity is delegated to the database: the check and the decrement are one statement, and the
 * row lock serializes concurrent reservations of the same product, so stock can never go negative.
 * Items are processed in productId order so that concurrent reservations always lock rows in the
 * same order and cannot deadlock.
 *
 * <p>A rejection rolls back to a savepoint taken on the caller's transaction connection: only this
 * reservation's decrements are undone and the transaction stays usable, so the caller can commit
 * the rejection together with its other writes. The transaction is never marked rollback-only.
 */
@Repository
public class JdbcStockRepository implements StockRepository {

    private static final String DECREMENT =
            """
        UPDATE stock SET quantity = quantity - :quantity
        WHERE product_id = :productId AND quantity >= :quantity
        """;

    private static final String EXISTS =
            "SELECT EXISTS (SELECT 1 FROM stock WHERE product_id = :productId)";

    private final DataSource dataSource;
    private final JdbcClient jdbc;

    public JdbcStockRepository(DataSource dataSource, JdbcClient jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public ReservationResult tryReserve(ReservationRequest request) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("tryReserve must run inside the caller's transaction");
        }
        // The connection bound to the current transaction: the same one JdbcClient uses.
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            Savepoint savepoint = connection.setSavepoint();
            for (ReservationItem item : inLockOrder(request.items())) {
                if (!decrement(item)) {
                    connection.rollback(savepoint);
                    connection.releaseSavepoint(savepoint);
                    return new ReservationResult.Rejected(reasonFor(item.productId()));
                }
            }
            connection.releaseSavepoint(savepoint);
            return new ReservationResult.Reserved();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Savepoint handling failed", e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static List<ReservationItem> inLockOrder(List<ReservationItem> items) {
        return items.stream().sorted(Comparator.comparing(ReservationItem::productId)).toList();
    }

    private boolean decrement(ReservationItem item) {
        return jdbc.sql(DECREMENT)
                        .param("quantity", item.quantity())
                        .param("productId", item.productId())
                        .update()
                == 1;
    }

    private RejectionReason reasonFor(String productId) {
        boolean exists =
                jdbc.sql(EXISTS).param("productId", productId).query(Boolean.class).single();
        return exists ? RejectionReason.INSUFFICIENT_STOCK : RejectionReason.UNKNOWN_PRODUCT;
    }
}
