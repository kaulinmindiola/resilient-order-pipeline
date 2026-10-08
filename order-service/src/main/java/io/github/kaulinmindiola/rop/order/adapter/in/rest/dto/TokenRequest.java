package io.github.kaulinmindiola.rop.order.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /auth/token (REQ-FUNC-007). */
public record TokenRequest(@NotBlank String clientId, @NotBlank String clientSecret) {

    /** Never print the secret, even by accident (REQ-SEC-002). */
    @Override
    public String toString() {
        return "TokenRequest[clientId=" + clientId + ", clientSecret=***]";
    }
}
