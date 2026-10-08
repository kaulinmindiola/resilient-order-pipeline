package io.github.kaulinmindiola.rop.order.adapter.security;

import io.github.kaulinmindiola.rop.order.domain.port.ClientCredentialsRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Exchanges client credentials for an HS256 access token valid for 30 minutes (REQ-FUNC-007,
 * REQ-FUNC-009, BR-013). This is a custom credential exchange, not OAuth2 (ADR-0005).
 */
@Service
public class TokenService {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final ClientCredentialsRepository credentials;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String dummyHash;

    public TokenService(
            ClientCredentialsRepository credentials,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            Clock clock) {
        this.credentials = credentials;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        // Same cost as a real hash, so unknown clients take as long as wrong secrets.
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public IssuedToken issue(String clientId, String clientSecret) {
        Optional<String> storedHash = credentials.findSecretHash(clientId);
        boolean secretMatches = passwordEncoder.matches(clientSecret, storedHash.orElse(dummyHash));
        if (storedHash.isEmpty() || !secretMatches) {
            throw new InvalidClientCredentialsException();
        }

        Instant now = clock.instant();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .subject(clientId)
                        .issuedAt(now)
                        .expiresAt(now.plus(TOKEN_TTL))
                        .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, TOKEN_TTL.toSeconds());
    }
}
