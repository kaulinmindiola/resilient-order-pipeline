package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import io.github.kaulinmindiola.rop.order.domain.model.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Persistence representation of the order aggregate (ADR-0018). Only status and updatedAt are
 * updatable; items and total are fixed at creation (BR-018).
 */
@Entity
@Table(name = "orders")
public class OrderJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrderStatus status;

    @Column(name = "total", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    /** Lets Spring Data persist directly instead of merging (no redundant SELECT). */
    @Transient private boolean newEntity = true;

    protected OrderJpaEntity() {
        // required by JPA
    }

    OrderJpaEntity(
            UUID id,
            OrderStatus status,
            BigDecimal total,
            Instant createdAt,
            Instant updatedAt,
            List<OrderItemJpaEntity> items) {
        this.id = id;
        this.status = status;
        this.total = total;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = new ArrayList<>(items);
    }

    /** The only mutation allowed on a persisted order. */
    void updateStatus(OrderStatus status, Instant updatedAt) {
        this.status = status;
        this.updatedAt = updatedAt;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItemJpaEntity> getItems() {
        return items;
    }
}
