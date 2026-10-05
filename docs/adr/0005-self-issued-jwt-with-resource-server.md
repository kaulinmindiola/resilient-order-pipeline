---
status: accepted
date: 2026-10-05
decision-makers: KM
---

# ADR-0005: Issue JWTs in-house with Nimbus and validate them with Spring Security Resource Server

## Context and Problem Statement

REQ-FUNC-007, REQ-FUNC-008 and REQ-FUNC-009 require issuing a self-issued JWT (HS256, no external identity provider), protecting `/api/v1/**`, and expiring tokens after 30 minutes without a refresh token. Client credentials are seeded, never self-registered. User management, OAuth2 and an external IdP are out of scope. How are tokens issued and validated?

## Decision Drivers

* REQ-FUNC-007 / 008 / 009, BR-012, BR-013.
* REQ-SEC-001 / REQ-SEC-002: externalized secret of at least 256 bits; never log tokens or secrets.
* No IdP, no user management.
* Reuse mature validation code; avoid custom, unproven security logic.
* No extra dependencies beyond the Spring Security stack.

## Considered Options

* Issue with `NimbusJwtEncoder` (HS256) and validate with Spring Security Resource Server (`NimbusJwtDecoder.withSecretKey`)
* Custom servlet filter validating the JWT manually (for example with `jjwt`)
* Full Spring Authorization Server
* Static API key

## Decision Outcome

Chosen option: **"Nimbus encoder for issuance + Resource Server for validation"**, because it reuses mature validation (signature, expiry) with no extra dependencies, while a full Authorization Server would be disproportionate for a single grant.

* `POST /auth/token` exchanges `clientId` / `clientSecret` for a Bearer JWT with `exp = iat + 30 min`, using an injected `Clock`. The endpoint is a custom credential exchange and **does not claim OAuth2 compliance**; the README states this.
* Secrets are verified with BCrypt against `client_credentials`. Unknown client and wrong secret return the same `401`, so clients cannot be enumerated.
* `JWT_SIGNING_SECRET` comes from the environment and its length (at least 32 bytes) is validated at startup.
* The `SecurityFilterChain` is stateless: `/auth/token` is public, `/api/v1/**` requires authentication, and all `401` responses are RFC 9457 `ProblemDetail`.

### Consequences

* Good, because it minimizes custom security code and the risk of subtle validation bugs.
* Good, because it fits the `Authorization: Bearer` contract naturally.
* Bad, because it is not OAuth2, and there is no refresh token and no key rotation.
* Bad, because a leaked shared HS256 secret compromises every token; mitigated by externalizing it and never committing it.
* Bad, because there is only HTTP and no rate limiting on `/auth/token`; both are declared limitations.

### Confirmation

Test 6 (*authentication*): valid and invalid credentials; absent, malformed, foreign-signed and expired tokens. Test 16 (*logs*): no `Authorization` header, token or secret appears in logs. Verified in Phase 4.

## Pros and Cons of the Options

### Nimbus encoder + Resource Server

* Good, because of the maturity and test coverage of the libraries.
* Good, because it already ships with the Spring Security 7 stack.
* Bad, because the filter-chain configuration is more verbose than a simple filter.

### Custom servlet filter

* Good, because it has the smallest footprint.
* Bad, because it reinvents JWT validation, with a higher risk of subtle errors.

### Spring Authorization Server

* Good, because it is a full standard.
* Bad, because it is significant overengineering for one grant and no end users.

### Static API key

* Good, because it is the simplest.
* Bad, because it offers no expiry and does not exercise token-based authentication.

## More Information

* Related: [ADR-0009](0009-flyway-migrations-and-seed-data.md) (seeded credentials with a BCrypt hash), [ADR-0008](0008-native-structured-logging-with-mdc.md) (no secrets in logs).
* Requirements: REQ-FUNC-007 / 008 / 009, REQ-SEC-001 / 002, BR-012, BR-013.
