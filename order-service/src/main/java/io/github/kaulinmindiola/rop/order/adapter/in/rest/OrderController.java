package io.github.kaulinmindiola.rop.order.adapter.in.rest;

import io.github.kaulinmindiola.rop.order.LogFields;
import io.github.kaulinmindiola.rop.order.adapter.in.rest.dto.CreateOrderRequest;
import io.github.kaulinmindiola.rop.order.adapter.in.rest.dto.CreateOrderResponse;
import io.github.kaulinmindiola.rop.order.adapter.in.rest.dto.OrderResponse;
import io.github.kaulinmindiola.rop.order.application.CreateOrderService;
import io.github.kaulinmindiola.rop.order.application.GetOrderService;
import io.github.kaulinmindiola.rop.order.domain.model.Order;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for orders (AI-CONTEXT §5). Only maps between HTTP and the use cases. */
@RestController
@RequestMapping(OrderController.BASE_PATH)
public class OrderController {

    static final String BASE_PATH = "/api/v1/orders";

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final CreateOrderService createOrderService;
    private final GetOrderService getOrderService;

    public OrderController(CreateOrderService createOrderService, GetOrderService getOrderService) {
        this.createOrderService = createOrderService;
        this.getOrderService = getOrderService;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request) {
        Order order = createOrderService.create(request.toCommand());
        MDC.put(LogFields.ORDER_ID, order.id().toString());
        try {
            log.info("Order created with status {}", order.status());
        } finally {
            MDC.remove(LogFields.ORDER_ID);
        }
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + order.id()))
                .body(CreateOrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable UUID orderId) {
        MDC.put(LogFields.ORDER_ID, orderId.toString());
        try {
            return OrderResponse.from(getOrderService.get(orderId));
        } finally {
            MDC.remove(LogFields.ORDER_ID);
        }
    }
}
