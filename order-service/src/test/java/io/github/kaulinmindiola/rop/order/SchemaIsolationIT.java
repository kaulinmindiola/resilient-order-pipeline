package io.github.kaulinmindiola.rop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Test 15: schema isolation is enforced by the database, not by convention (ADR-0002, ADR-0015).
 * Each role connects with its own credentials; a denied operation must fail with SQLSTATE 42501
 * (insufficient_privilege).
 */
class SchemaIsolationIT extends AbstractIntegrationTest {

    private static final String INSUFFICIENT_PRIVILEGE = "42501";

    @Autowired private JdbcClient jdbc;

    static Stream<Arguments> crossSchemaAccess() {
        return Stream.of(
                Arguments.of("order_svc", DB_PASSWORD, "SELECT 1 FROM inventory_service.stock"),
                Arguments.of(
                        "order_svc", DB_PASSWORD, "CREATE TABLE inventory_service.probe (id int)"),
                Arguments.of(
                        "inventory_svc",
                        INVENTORY_DB_PASSWORD,
                        "CREATE TABLE order_service.probe (id int)"),
                Arguments.of("order_svc", DB_PASSWORD, "CREATE TABLE public.probe (id int)"));
    }

    @ParameterizedTest(name = "{0} is denied: {2}")
    @MethodSource("crossSchemaAccess")
    @DisplayName("ADR-0002 ADR-0015 a role cannot touch another service's schema or public")
    void crossSchemaAccessIsDenied(String role, String password, String sql) {
        assertThatThrownBy(() -> execute(role, password, sql))
                .isInstanceOfSatisfying(
                        SQLException.class,
                        e -> assertThat(e.getSQLState()).isEqualTo(INSUFFICIENT_PRIVILEGE));
    }

    @Test
    @DisplayName("ADR-0015 a role owns its schema and can create and drop objects in it")
    void roleOwnsItsOwnSchema() {
        String table = "probe_" + UUID.randomUUID().toString().replace("-", "");

        assertThatCode(
                        () ->
                                execute(
                                        "order_svc",
                                        DB_PASSWORD,
                                        "CREATE TABLE order_service."
                                                + table
                                                + " (id int); DROP TABLE order_service."
                                                + table))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ADR-0015 the application does not connect as a superuser")
    void applicationIsNotSuperuser() {
        Boolean superuser =
                jdbc.sql("SELECT rolsuper FROM pg_roles WHERE rolname = current_user")
                        .query(Boolean.class)
                        .single();

        assertThat(superuser).isFalse();
    }

    private static void execute(String role, String password, String sql) throws SQLException {
        try (Connection connection =
                        DriverManager.getConnection(POSTGRES.getJdbcUrl(), role, password);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
