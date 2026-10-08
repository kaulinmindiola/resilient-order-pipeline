-- V3: initial stock (REQ-FUNC-017). Product ids mirror the order_service catalog, but there is no
-- foreign key between schemas: each service owns its data (ADR-0002).
--   SKU-001  1000  happy path
--   SKU-002     1  concurrency scenario
--   SKU-003     0  INSUFFICIENT_STOCK
--   SKU-004     -  no row, on purpose: UNKNOWN_PRODUCT
-- These rows are the contract of the system tests: integration tests create their own products.
INSERT INTO stock (product_id, quantity) VALUES
                                             ('SKU-001', 1000),
                                             ('SKU-002',    1),
                                             ('SKU-003',    0);
