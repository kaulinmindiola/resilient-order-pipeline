package io.github.kaulinmindiola.rop.order.adapter.security;

/** A signed access token and its lifetime in seconds. */
public record IssuedToken(String accessToken, long expiresInSeconds) {}
