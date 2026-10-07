package io.github.kaulinmindiola.rop.order.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.order.application.GetOrderService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** An unexpected failure never leaks internal details to the client (AI-CONTEXT §5). */
class UnexpectedErrorIT extends AbstractIntegrationTest {

    private static final String INTERNAL_DETAIL = "connection to db-internal-host:5432 refused";

    @Autowired private MockMvcTester mvc;
    @MockitoBean private GetOrderService getOrderService;

    @Test
    @DisplayName("an unexpected error is a generic 500 ProblemDetail without internal details")
    void unexpectedErrorIsGeneric() throws Exception {
        when(getOrderService.get(any())).thenThrow(new IllegalStateException(INTERNAL_DETAIL));

        MvcTestResult result = mvc.get().uri("/api/v1/orders/" + UUID.randomUUID()).exchange();

        assertThat(result)
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                {"status":500,"detail":"An unexpected error occurred."}
                """);
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(INTERNAL_DETAIL)
                .doesNotContain("IllegalStateException")
                .doesNotContain("at io.github");
    }
}
