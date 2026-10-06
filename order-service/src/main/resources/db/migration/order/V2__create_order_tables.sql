-- V2: business tables of the order bounded context.
-- BR-003/BR-017 (status), BR-018 (price snapshot), ADR-0014 (catalog), REQ-FUNC-007 (credentials).

CREATE TABLE orders (
                        id         uuid          PRIMARY KEY,
                        status     varchar(16)   NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED')),
                        total      numeric(19,2) NOT NULL CHECK (total >= 0),
                        created_at timestamptz   NOT NULL,
                        updated_at timestamptz   NOT NULL
);

-- No FK to products: the price is a snapshot and must outlive catalog changes (BR-018).
CREATE TABLE order_items (
                             id                  uuid          PRIMARY KEY,
                             order_id            uuid          NOT NULL REFERENCES orders (id),
                             product_id          varchar(64)   NOT NULL,
                             quantity            int           NOT NULL CHECK (quantity > 0),
                             unit_price_snapshot numeric(19,2) NOT NULL CHECK (unit_price_snapshot >= 0)
);

-- PostgreSQL does not index foreign keys automatically; items are always loaded by order.
CREATE INDEX ix_order_items_order_id ON order_items (order_id);

-- Read-only catalog seeded by Flyway; the single source of prices (ADR-0014).
CREATE TABLE products (
                          product_id varchar(64)   PRIMARY KEY,
                          unit_price numeric(19,2) NOT NULL CHECK (unit_price >= 0)
);

-- API client credentials; the secret is stored only as a BCrypt hash (REQ-SEC-001).
CREATE TABLE client_credentials (
                                    client_id          varchar(64)  PRIMARY KEY,
                                    client_secret_hash varchar(100) NOT NULL,
                                    created_at         timestamptz  NOT NULL
);
