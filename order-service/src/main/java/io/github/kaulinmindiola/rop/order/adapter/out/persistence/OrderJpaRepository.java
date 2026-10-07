package io.github.kaulinmindiola.rop.order.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository; only the adapter uses it, never the application layer. */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {}
