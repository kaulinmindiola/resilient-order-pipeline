package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kaulinmindiola.rop.order.AbstractIntegrationTest;
import io.github.kaulinmindiola.rop.order.domain.model.Money;
import io.github.kaulinmindiola.rop.order.domain.port.ClientCredentialsRepository;
import io.github.kaulinmindiola.rop.order.domain.port.ProductCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CatalogAndCredentialsIT extends AbstractIntegrationTest {

    @Autowired private ProductCatalog productCatalog;
    @Autowired private ClientCredentialsRepository credentials;

    @Test
    @DisplayName("ADR-0014 the catalog returns the seeded price of a known product")
    void knownProductHasPrice() {
        assertThat(productCatalog.priceOf("SKU-001")).contains(Money.of("19.99"));
    }

    @Test
    @DisplayName("ADR-0014 an unknown product has no price")
    void unknownProductHasNoPrice() {
        assertThat(productCatalog.priceOf("SKU-999")).isEmpty();
    }

    @Test
    @DisplayName("REQ-SEC-001 the seeded client has a BCrypt hash and an unknown client has none")
    void credentialsLookup() {
        assertThat(credentials.findSecretHash(SEED_CLIENT_ID))
                .hasValueSatisfying(hash -> assertThat(hash).startsWith("$2"));
        assertThat(credentials.findSecretHash("unknown-client")).isEmpty();
    }
}
