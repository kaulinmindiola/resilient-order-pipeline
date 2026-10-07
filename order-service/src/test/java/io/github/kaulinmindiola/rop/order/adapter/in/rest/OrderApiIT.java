package io.github.kaulinmindiola.rop.order.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class OrderApiIT extends AbstractIntegrationTest {

    private static final String ORDERS = "/api/v1/orders";

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcClient jdbc;

    private MvcTestResult postOrder(String body) {
        return mvc.post()
                .uri(ORDERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private int ordersCount() {
        return jdbc.sql("SELECT count(*) FROM orders").query(Integer.class).single();
    }

    private int outboxCount() {
        return jdbc.sql("SELECT count(*) FROM outbox_events").query(Integer.class).single();
    }

    @Test
    @DisplayName(
            "REQ-FUNC-005 REQ-FUNC-006 a created order returns 201 with Location and can be read back")
    void createAndReadOrder() {
        MvcTestResult created =
                postOrder(
                        """
                {"items":[{"productId":"SKU-001","quantity":3},
                          {"productId":"SKU-003","quantity":2}]}
                """);

        assertThat(created).hasStatus(HttpStatus.CREATED);
        String location = created.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).startsWith(ORDERS + "/");
        String orderId = location.substring((ORDERS + "/").length());
        assertThat(created)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                {"orderId":"%s","status":"PENDING"}
                """
                                .formatted(orderId));

        assertThat(mvc.get().uri(location))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                {"orderId":"%s","status":"PENDING","total":70.97,
                 "items":[{"productId":"SKU-001","quantity":3,"unitPrice":19.99},
                          {"productId":"SKU-003","quantity":2,"unitPrice":5.50}]}
                """
                                .formatted(orderId));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"items\":[]}",
                "{}",
                "{\"items\":[{\"productId\":\"SKU-001\",\"quantity\":0}]}",
                "{\"items\":[{\"productId\":\"SKU-001\",\"quantity\":-1}]}",
                "{\"items\":[{\"productId\":\"SKU-001\"}]}",
                "{\"items\":[{\"productId\":\"\",\"quantity\":1}]}",
                "{\"items\":[null]}",
                "not json"
            })
    @DisplayName("REQ-FUNC-001 REQ-FUNC-002 a malformed request returns 400 and persists nothing")
    void malformedRequestIsRejected(String body) {
        int ordersBefore = ordersCount();
        int outboxBefore = outboxCount();

        assertThat(postOrder(body)).hasStatus(HttpStatus.BAD_REQUEST);

        assertThat(ordersCount()).isEqualTo(ordersBefore);
        assertThat(outboxCount()).isEqualTo(outboxBefore);
    }
}
