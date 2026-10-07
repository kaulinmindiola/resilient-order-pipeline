package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import io.github.kaulinmindiola.rop.order.domain.model.Money;
import io.github.kaulinmindiola.rop.order.domain.port.ProductCatalog;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Read-only catalog backed by the products table (ADR-0014). */
@Repository
public class JdbcProductCatalog implements ProductCatalog {

    private final JdbcClient jdbc;

    public JdbcProductCatalog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Money> priceOf(String productId) {
        return jdbc.sql("SELECT unit_price FROM products WHERE product_id = :productId")
                .param("productId", productId)
                .query(BigDecimal.class)
                .optional()
                .map(Money::new);
    }
}
