package io.github.kaulinmindiola.rop.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

class StockSchemaIT extends AbstractIntegrationTest {

    @Autowired private JdbcClient jdbc;

    @Test
    @DisplayName("ADR-0009 every inventory migration is applied in order")
    void everyMigrationIsApplied() {
        List<String> applied =
                jdbc.sql(
                                "SELECT version || ':' || type || ':' || success"
                                        + " FROM flyway_schema_history"
                                        + " WHERE version IS NOT NULL"
                                        + " ORDER BY installed_rank")
                        .query(String.class)
                        .list();

        assertThat(applied).containsExactly("1:SQL:true", "2:SQL:true", "3:SQL:true");
    }

    @Test
    @DisplayName("REQ-FUNC-017 stock is seeded for SKU-001..003, and SKU-004 has no row on purpose")
    void stockIsSeeded() {
        Map<String, Integer> stock =
                jdbc.sql(
                                "SELECT product_id, quantity FROM stock"
                                        + " WHERE product_id LIKE 'SKU-%' ORDER BY product_id")
                        .query(
                                rs -> {
                                    Map<String, Integer> rows = new LinkedHashMap<>();
                                    while (rs.next()) {
                                        rows.put(rs.getString(1), rs.getInt(2));
                                    }
                                    return rows;
                                });

        assertThat(stock)
                .containsExactly(entry("SKU-001", 1000), entry("SKU-002", 1), entry("SKU-003", 0));
    }

    @Test
    @DisplayName(
            "BR-015 the database itself rejects negative stock, independently of the reservation logic")
    void negativeStockIsRejectedByTheDatabase() {
        String productId = "TEST-" + UUID.randomUUID();
        jdbc.sql("INSERT INTO stock (product_id, quantity) VALUES (?, 0)")
                .param(productId)
                .update();

        assertThatThrownBy(
                        () ->
                                jdbc.sql(
                                                "UPDATE stock SET quantity = quantity - 1 WHERE product_id = ?")
                                        .param(productId)
                                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        Integer quantity =
                jdbc.sql("SELECT quantity FROM stock WHERE product_id = ?")
                        .param(productId)
                        .query(Integer.class)
                        .single();
        assertThat(quantity).isZero();
    }
}
