package io.github.kaulinmindiola.rop.order.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class TokenServiceTest {

    private static final SecretKey KEY =
            new SecretKeySpec(
                    "unit-test-signing-secret-0123456789-abcdef".getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
    // Truncated to seconds: JWT timestamps have second precision.
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    // Low BCrypt cost keeps the unit test fast; production uses the default cost.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Map<String, String> hashes =
            Map.of("known-client", passwordEncoder.encode("right-secret"));
    private final TokenService tokenService =
            new TokenService(
                    clientId -> Optional.ofNullable(hashes.get(clientId)),
                    passwordEncoder,
                    new NimbusJwtEncoder(new ImmutableSecret<>(KEY)),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    private static Jwt decode(String token) {
        return NimbusJwtDecoder.withSecretKey(KEY)
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
                .decode(token);
    }

    @Test
    @DisplayName(
            "REQ-FUNC-007 REQ-FUNC-009 BR-013 valid credentials yield an HS256 token valid for exactly 30 minutes")
    void issuesTokenForValidCredentials() {
        IssuedToken issued = tokenService.issue("known-client", "right-secret");

        Jwt jwt = decode(issued.accessToken());
        assertThat(issued.expiresInSeconds()).isEqualTo(1800);
        assertThat(jwt.getSubject()).isEqualTo("known-client");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(1800));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    @DisplayName("REQ-FUNC-007 a wrong secret is rejected")
    void rejectsWrongSecret() {
        assertThatThrownBy(() -> tokenService.issue("known-client", "wrong-secret"))
                .isInstanceOf(InvalidClientCredentialsException.class)
                .hasMessage("Invalid client credentials");
    }

    @Test
    @DisplayName("REQ-FUNC-007 an unknown client is rejected exactly like a wrong secret")
    void rejectsUnknownClientIndistinguishably() {
        assertThatThrownBy(() -> tokenService.issue("unknown-client", "right-secret"))
                .isInstanceOf(InvalidClientCredentialsException.class)
                .hasMessage("Invalid client credentials");
    }
}
