package io.github.kaulinmindiola.rop.order.adapter.in.rest.dto;

import java.math.BigDecimal;

/** One order line as persisted, with its price snapshot (BR-018). */
public record ItemResponse(String productId, int quantity, BigDecimal unitPrice) {}
