package io.github.kaulinmindiola.rop.order.adapter.out.persistence.migration;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the API client used by the demo and the tests (REQ-FUNC-017, ADR-0009).
 *
 * <p>Registered as a Spring bean, which Spring Boot passes to Flyway explicitly, so the migration
 * can never be skipped silently by classpath scanning. The class name follows Flyway's convention
 * because {@link BaseJavaMigration} derives the version from it.
 *
 * <p>Only the BCrypt hash of the secret is stored (REQ-SEC-001) and nothing is logged
 * (REQ-SEC-002).
 */
@Component
public class V4__seed_client_credentials extends BaseJavaMigration {

    private static final String INSERT =
            "INSERT INTO client_credentials (client_id, client_secret_hash, created_at)"
                    + " VALUES (?, ?, ?)";

    private final String clientId;
    private final String clientSecret;

    public V4__seed_client_credentials(
            @Value("${SEED_CLIENT_ID}") String clientId,
            @Value("${SEED_CLIENT_SECRET}") String clientSecret) {
        this.clientId = requireText(clientId, "SEED_CLIENT_ID");
        this.clientSecret = requireText(clientSecret, "SEED_CLIENT_SECRET");
    }

    @Override
    public void migrate(Context context) throws Exception {
        String secretHash = new BCryptPasswordEncoder().encode(clientSecret);
        try (PreparedStatement statement = context.getConnection().prepareStatement(INSERT)) {
            statement.setString(1, clientId);
            statement.setString(2, secretHash);
            statement.setObject(3, OffsetDateTime.now(ZoneOffset.UTC));
            statement.executeUpdate();
        }
    }

    /** The error names the variable, never its value. */
    private static String requireText(String value, String variable) {
        if (value.isBlank()) {
            throw new IllegalStateException(variable + " must not be blank");
        }
        return value;
    }
}
