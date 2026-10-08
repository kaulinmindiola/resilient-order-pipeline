package io.github.kaulinmindiola.rop.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReservationRequestTest {

    private static final UUID ORDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    @DisplayName("BR-015 a valid request keeps an immutable copy of its items")
    void validRequestIsImmutable() {
        List<ReservationItem> items = new ArrayList<>(List.of(new ReservationItem("SKU-001", 2)));
        ReservationRequest request = new ReservationRequest(ORDER_ID, items);

        items.add(new ReservationItem("SKU-002", 1));

        assertThat(request.items()).containsExactly(new ReservationItem("SKU-001", 2));
        assertThatThrownBy(() -> request.items().add(new ReservationItem("SKU-003", 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("a request without an order id is invalid")
    void requiresOrderId() {
        assertThatThrownBy(
                        () ->
                                new ReservationRequest(
                                        null, List.of(new ReservationItem("SKU-001", 1))))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    @DisplayName("a request without items is invalid")
    void requiresItems() {
        assertThatThrownBy(() -> new ReservationRequest(ORDER_ID, List.of()))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> new ReservationRequest(ORDER_ID, null))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    @DisplayName("a request cannot contain null items")
    void rejectsNullItems() {
        List<ReservationItem> items = Arrays.asList(new ReservationItem("SKU-001", 1), null);

        assertThatThrownBy(() -> new ReservationRequest(ORDER_ID, items))
                .isInstanceOf(InvalidReservationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("an item must reserve a positive quantity")
    void rejectsNonPositiveQuantity(int quantity) {
        assertThatThrownBy(() -> new ReservationItem("SKU-001", quantity))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    @DisplayName("an item must name a product")
    void rejectsBlankProductId() {
        assertThatThrownBy(() -> new ReservationItem(" ", 1))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    @DisplayName("a rejection always carries its reason")
    void rejectionRequiresReason() {
        assertThatThrownBy(() -> new ReservationResult.Rejected(null))
                .isInstanceOf(NullPointerException.class);
    }
}
