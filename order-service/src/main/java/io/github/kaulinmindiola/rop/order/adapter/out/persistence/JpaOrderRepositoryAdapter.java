package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import io.github.kaulinmindiola.rop.order.domain.model.Order;
import io.github.kaulinmindiola.rop.order.domain.port.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of {@link OrderRepository} (ADR-0018). */
@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public JpaOrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /**
     * Inserts a new order, or updates only status and updatedAt of an existing one (BR-018). Joins
     * the caller's transaction and flushes so every write reaches the database inside it (DI-16).
     */
    @Override
    @Transactional
    public void save(Order order) {
        jpaRepository
                .findById(order.id())
                .ifPresentOrElse(
                        existing -> existing.updateStatus(order.status(), order.updatedAt()),
                        () -> jpaRepository.save(OrderPersistenceMapper.toNewEntity(order)));
        jpaRepository.flush();
    }

    /** Maps inside the transaction: with open-in-view disabled, lazy items need a live session. */
    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id).map(OrderPersistenceMapper::toDomain);
    }
}
