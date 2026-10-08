package io.github.kaulinmindiola.rop.order.adapter.in.rest;

import io.github.kaulinmindiola.rop.order.adapter.in.rest.dto.TokenRequest;
import io.github.kaulinmindiola.rop.order.adapter.in.rest.dto.TokenResponse;
import io.github.kaulinmindiola.rop.order.adapter.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Credential exchange endpoint; not an OAuth2 token endpoint (ADR-0005). */
@RestController
public class AuthController {

    private final TokenService tokenService;

    public AuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/auth/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return TokenResponse.from(tokenService.issue(request.clientId(), request.clientSecret()));
    }
}
