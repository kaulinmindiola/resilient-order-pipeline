package io.github.kaulinmindiola.rop.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The service starts, migrates and connects with its own role (ADR-0015, REQ-INST-001). */
class OrderServiceStartupIT extends AbstractIntegrationTest {

    @Autowired private JdbcClient jdbc;

    @Test
    void connectsWithServiceRoleInsteadOfSuperuser() {
        String currentUser = jdbc.sql("SELECT current_user").query(String.class).single();

        assertThat(currentUser).isEqualTo("order_svc");
    }

    @Test
    void appliesTechnicalTablesMigrationV1() {
        Boolean success =
                jdbc.sql("SELECT success FROM flyway_schema_history WHERE version = '1'")
                        .query(Boolean.class)
                        .single();

        assertThat(success).isTrue();
    }
}
