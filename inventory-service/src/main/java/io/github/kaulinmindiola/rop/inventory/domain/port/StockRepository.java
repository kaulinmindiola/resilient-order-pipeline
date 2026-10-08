package io.github.kaulinmindiola.rop.inventory.domain.port;

import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationRequest;
import io.github.kaulinmindiola.rop.inventory.domain.model.ReservationResult;

/**
 * Atomic, all-or-nothing stock reservation (BR-015, ADR-0010).
 *
 * <p>Runs inside the caller's transaction and never opens its own. A rejection is returned as a
 * result and never marks the transaction rollback-only, so the caller can commit it together with
 * other writes. Only infrastructure failures are thrown.
 */
public interface StockRepository {

    ReservationResult tryReserve(ReservationRequest request);
}
