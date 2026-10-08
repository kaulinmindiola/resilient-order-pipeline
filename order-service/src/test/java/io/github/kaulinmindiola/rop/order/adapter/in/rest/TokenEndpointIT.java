package io.github.kaulinmindiola.rop.order.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Test 6, issuance half: POST /auth/token (REQ-FUNC-007). */
class TokenEndpointIT extends AbstractIntegrationTest {

    @Autowired private MockMvcTester mvc;

    private MvcTestResult requestToken(String clientId, String clientSecret) {
        return mvc.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                {"clientId":"%s","clientSecret":"%s"}
                """
                                .formatted(clientId, clientSecret))
                .exchange();
    }

    @Test
    @DisplayName("REQ-FUNC-007 the seeded client obtains a Bearer token valid for 1800 seconds")
    void validCredentialsYieldToken() {
        MvcTestResult result = requestToken(SEED_CLIENT_ID, SEED_CLIENT_SECRET);

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                {"tokenType":"Bearer","expiresIn":1800}
                """);
        assertThat(result).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    }

    @Test
    @DisplayName("REQ-FUNC-007 a wrong secret and an unknown client get the same 401 ProblemDetail")
    void invalidCredentialsAreIndistinguishable() throws Exception {
        MvcTestResult wrongSecret = requestToken(SEED_CLIENT_ID, "wrong-secret");
        MvcTestResult unknownClient = requestToken("unknown-client", SEED_CLIENT_SECRET);

        assertThat(wrongSecret)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(unknownClient).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(unknownClient.getResponse().getContentAsString())
                .isEqualTo(wrongSecret.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("REQ-FUNC-007 a request without credentials is a 400")
    void missingCredentialsAreBadRequest() {
        assertThat(
                        mvc.post()
                                .uri("/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"clientId\":\"" + SEED_CLIENT_ID + "\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
