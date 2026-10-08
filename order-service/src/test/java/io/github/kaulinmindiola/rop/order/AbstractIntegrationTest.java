package io.github.kaulinmindiola.rop.order;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Real PostgreSQL and Kafka shared by every integration test in this service (DI-05). The
 * application is configured through the same variables Docker Compose uses and connects with the
 * service role, never as superuser (ADR-0015). MockMvc is configured here so that API tests share
 * the same Spring context as every other integration test.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String DB_PASSWORD = "order-it";
    protected static final String INVENTORY_DB_PASSWORD = "inventory-it";
    protected static final String SEED_CLIENT_ID = "it-client";
    protected static final String SEED_CLIENT_SECRET = "it-secret";
    protected static final String JWT_SIGNING_SECRET = "it-jwt-signing-secret-0123456789-abcdef";

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16")
                    .withEnv("ORDER_DB_PASSWORD", DB_PASSWORD)
                    .withEnv("INVENTORY_DB_PASSWORD", INVENTORY_DB_PASSWORD)
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../infra/postgres/init/01-schemas-and-roles.sh", 0755),
                            "/docker-entrypoint-initdb.d/01-schemas-and-roles.sh");

    protected static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka:4.2.2")
                    .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

    static {
        Startables.deepStart(POSTGRES, KAFKA).join();
    }

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_SCHEMA", () -> "order_service");
        registry.add("DB_USER", () -> "order_svc");
        registry.add("DB_PASSWORD", () -> DB_PASSWORD);
        registry.add("KAFKA_BOOTSTRAP_SERVERS", KAFKA::getBootstrapServers);
        registry.add("SEED_CLIENT_ID", () -> SEED_CLIENT_ID);
        registry.add("SEED_CLIENT_SECRET", () -> SEED_CLIENT_SECRET);
        registry.add("JWT_SIGNING_SECRET", () -> JWT_SIGNING_SECRET);
    }
}
