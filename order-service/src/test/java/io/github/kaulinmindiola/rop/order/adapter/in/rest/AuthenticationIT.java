package io.github.kaulinmindiola.rop.order.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Test 6, protection half: only a valid, unexpired token signed with our key gets through. */
class AuthenticationIT extends AbstractIntegrationTest {

    private static final String FOREIGN_SECRET = "a-foreign-signing-secret-0123456789-abcdef";

    @Autowired private JdbcClient jdbc;

    /** Signs a token like the service would, but with an arbitrary key and validity window. */
    private static String signedToken(String secret, Instant issuedAt, Instant expiresAt) {
        NimbusJwtEncoder encoder =
                new NimbusJwtEncoder(
                        new ImmutableSecret<>(
                                new SecretKeySpec(
                                        secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .subject(SEED_CLIENT_ID)
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private MvcTestResult getUnknownOrder(String authorization) {
        var request = mvc.get().uri("/api/v1/orders/" + UUID.randomUUID());
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return request.exchange();
    }

    private static void assertUnauthorized(MvcTestResult result) {
        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                {"status":401,"detail":"A valid bearer token is required."}
                """);
        assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Bearer");
    }

    @Test
    @DisplayName("REQ-FUNC-008 a valid token reaches the API (unknown order → 404, not 401)")
    void validTokenIsAccepted() {
        assertThat(getUnknownOrder(bearerToken())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("BR-012 a request without Authorization header is a 401 ProblemDetail")
    void missingTokenIsRejected() {
        assertUnauthorized(getUnknownOrder(null));
    }

    @Test
    @DisplayName("REQ-FUNC-008 a malformed token is a 401 ProblemDetail")
    void malformedTokenIsRejected() {
        assertUnauthorized(getUnknownOrder("Bearer not-a-jwt"));
    }

    @Test
    @DisplayName("REQ-FUNC-008 a token signed with another key is a 401 ProblemDetail")
    void foreignSignatureIsRejected() {
        Instant now = Instant.now();
        assertUnauthorized(
                getUnknownOrder(
                        "Bearer " + signedToken(FOREIGN_SECRET, now, now.plusSeconds(1800))));
    }

    @Test
    @DisplayName("REQ-FUNC-009 BR-013 an expired token is a 401 ProblemDetail")
    void expiredTokenIsRejected() {
        Instant now = Instant.now();
        String expired =
                signedToken(JWT_SIGNING_SECRET, now.minusSeconds(3600), now.minusSeconds(1));

        assertUnauthorized(getUnknownOrder("Bearer " + expired));
    }

    @Test
    @DisplayName("BR-012 creating an order without a token is rejected and persists nothing")
    void createWithoutTokenPersistsNothing() {
        int ordersBefore = jdbc.sql("SELECT count(*) FROM orders").query(Integer.class).single();

        assertUnauthorized(
                mvc.post()
                        .uri("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"SKU-001\",\"quantity\":1}]}")
                        .exchange());

        assertThat(jdbc.sql("SELECT count(*) FROM orders").query(Integer.class).single())
                .isEqualTo(ordersBefore);
    }
}
