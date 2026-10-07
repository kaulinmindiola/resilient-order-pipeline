package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import io.github.kaulinmindiola.rop.order.domain.port.ClientCredentialsRepository;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reads the stored BCrypt hash of an API client secret (REQ-SEC-001). */
@Repository
public class JdbcClientCredentialsRepository implements ClientCredentialsRepository {

    private final JdbcClient jdbc;

    public JdbcClientCredentialsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> findSecretHash(String clientId) {
        return jdbc.sql(
                        "SELECT client_secret_hash FROM client_credentials WHERE client_id = :clientId")
                .param("clientId", clientId)
                .query(String.class)
                .optional();
    }
}
