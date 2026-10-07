package io.github.kaulinmindiola.rop.order.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** One requested line: product and quantity only (REQ-FUNC-001, REQ-FUNC-002). */
public record ItemRequest(
        @NotBlank @Size(max = 64) String productId, @NotNull @Positive Integer quantity) {}
