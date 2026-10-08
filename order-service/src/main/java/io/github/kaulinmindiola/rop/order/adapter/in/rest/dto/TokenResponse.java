package io.github.kaulinmindiola.rop.order.adapter.in.rest.dto;

import io.github.kaulinmindiola.rop.order.adapter.security.IssuedToken;

/** Body of a successful token response (AI-CONTEXT §5). */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse from(IssuedToken token) {
        return new TokenResponse(token.accessToken(), "Bearer", token.expiresInSeconds());
    }
}
