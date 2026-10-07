package io.github.kaulinmindiola.rop.order.domain.port;

import java.util.Optional;

/** API client credentials; only the BCrypt hash of the secret is ever stored (REQ-SEC-001). */
public interface ClientCredentialsRepository {

    Optional<String> findSecretHash(String clientId);
}
