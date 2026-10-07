package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Persistence representation of an order line. Items never change after creation (BR-018). */
@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false, length = 64)
    private String productId;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(
            name = "unit_price_snapshot",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 2)
    private BigDecimal unitPriceSnapshot;

    protected OrderItemJpaEntity() {
        // required by JPA
    }

    OrderItemJpaEntity(UUID id, String productId, int quantity, BigDecimal unitPriceSnapshot) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public UUID getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }
}
