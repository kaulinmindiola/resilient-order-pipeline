package io.github.kaulinmindiola.rop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SeedDataIT extends AbstractIntegrationTest {

    @Autowired private JdbcClient jdbc;

    @Test
    @DisplayName("ADR-0009 every migration, including the Java seed, is applied in order")
    void everyMigrationIsApplied() {
        List<String> applied =
                jdbc.sql(
                                "SELECT version || ':' || type || ':' || success"
                                        + " FROM flyway_schema_history"
                                        + " WHERE version IS NOT NULL"
                                        + " ORDER BY installed_rank")
                        .query(String.class)
                        .list();

        assertThat(applied)
                .containsExactly("1:SQL:true", "2:SQL:true", "3:SQL:true", "4:JDBC:true");
    }

    @Test
    @DisplayName("REQ-FUNC-017 the product catalog is seeded with the four reference products")
    void catalogIsSeeded() {
        Map<String, BigDecimal> prices =
                jdbc.sql("SELECT product_id, unit_price FROM products ORDER BY product_id")
                        .query(
                                rs -> {
                                    Map<String, BigDecimal> rows = new LinkedHashMap<>();
                                    while (rs.next()) {
                                        rows.put(rs.getString(1), rs.getBigDecimal(2));
                                    }
                                    return rows;
                                });

        assertThat(prices)
                .containsExactly(
                        entry("SKU-001", new BigDecimal("19.99")),
                        entry("SKU-002", new BigDecimal("49.90")),
                        entry("SKU-003", new BigDecimal("5.50")),
                        entry("SKU-004", new BigDecimal("12.00")));
    }

    @Test
    @DisplayName(
            "REQ-FUNC-017 REQ-SEC-001 the API client is seeded with a BCrypt hash, never in clear")
    void clientCredentialsAreSeededAsBcryptHash() {
        String secretHash =
                jdbc.sql("SELECT client_secret_hash FROM client_credentials WHERE client_id = ?")
                        .param(SEED_CLIENT_ID)
                        .query(String.class)
                        .single();

        assertThat(secretHash).isNotEqualTo(SEED_CLIENT_SECRET).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches(SEED_CLIENT_SECRET, secretHash)).isTrue();
    }
}
