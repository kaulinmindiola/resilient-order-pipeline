package io.github.kaulinmindiola.rop.order.domain.port;

import io.github.kaulinmindiola.rop.order.domain.model.Money;
import java.util.Optional;

/** Read-only catalog, the single source of prices (ADR-0014). */
public interface ProductCatalog {

    Optional<Money> priceOf(String productId);
}
