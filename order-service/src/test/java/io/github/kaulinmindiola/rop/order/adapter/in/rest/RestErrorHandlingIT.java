package io.github.kaulinmindiola.rop.order.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Every API error is an RFC 9457 ProblemDetail with the right status (AI-CONTEXT §5). */
class RestErrorHandlingIT extends AbstractIntegrationTest {

    private static final String ORDERS = "/api/v1/orders";

    @Autowired private JdbcClient jdbc;

    private MvcTestResult postOrder(String body) {
        return mvc.post()
                .uri(ORDERS)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult get(String path) {
        return mvc.get().uri(path).header(HttpHeaders.AUTHORIZATION, bearerToken()).exchange();
    }

    private int ordersCount() {
        return jdbc.sql("SELECT count(*) FROM orders").query(Integer.class).single();
    }

    @Test
    @DisplayName(
            "REQ-FUNC-002 a validation failure is a 400 ProblemDetail listing the invalid field")
    void validationFailureListsFieldErrors() {
        MvcTestResult result =
                postOrder("{\"items\":[{\"productId\":\"SKU-001\",\"quantity\":0}]}");

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                {"status":400,"errors":[{"field":"items[0].quantity"}]}
                """);
    }

    @Test
    @DisplayName("an unreadable JSON body is a 400 ProblemDetail")
    void unreadableBodyIsBadRequest() {
        assertThat(postOrder("not json"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @DisplayName("an order id that is not a UUID is a 400 ProblemDetail")
    void malformedOrderIdIsBadRequest() {
        assertThat(get(ORDERS + "/not-a-uuid"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @DisplayName("ADR-0014 an unknown product is a 422 ProblemDetail and nothing is persisted")
    void unknownProductIsUnprocessable() {
        int ordersBefore = ordersCount();

        assertThat(postOrder("{\"items\":[{\"productId\":\"SKU-999\",\"quantity\":1}]}"))
                .hasStatus(422)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                {"status":422,"detail":"Unknown product: SKU-999"}
                """);
        assertThat(ordersCount()).isEqualTo(ordersBefore);
    }

    @Test
    @DisplayName("REQ-FUNC-006 an unknown order is a 404 ProblemDetail")
    void unknownOrderIsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThat(get(ORDERS + "/" + unknown))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                {"status":404,"detail":"Order not found: %s"}
                """
                                .formatted(unknown));
    }
}
