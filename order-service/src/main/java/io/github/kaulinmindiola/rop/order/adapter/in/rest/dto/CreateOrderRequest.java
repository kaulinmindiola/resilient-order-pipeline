package io.github.kaulinmindiola.rop.order.adapter.in.rest.dto;

import io.github.kaulinmindiola.rop.order.application.CreateOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Body of POST /api/v1/orders. Clients never send prices (ADR-0014). */
public record CreateOrderRequest(@NotEmpty List<@Valid @NotNull ItemRequest> items) {

    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(
                items.stream()
                        .map(item -> new CreateOrderCommand.Item(item.productId(), item.quantity()))
                        .toList());
    }
}
