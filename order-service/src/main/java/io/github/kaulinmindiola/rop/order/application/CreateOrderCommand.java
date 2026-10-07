package io.github.kaulinmindiola.rop.order.application;

import java.util.List;

/** Input of the create-order use case. Clients never send prices (ADR-0014). */
public record CreateOrderCommand(List<Item> items) {

    public CreateOrderCommand {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item(String productId, int quantity) {}
}
