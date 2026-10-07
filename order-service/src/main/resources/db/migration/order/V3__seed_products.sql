-- V3: read-only product catalog, the only source of prices (ADR-0014, REQ-FUNC-017).
-- Stock for these products lives in inventory_service; SKU-004 intentionally has none.
--   SKU-001  happy path            (stock 1000)
--   SKU-002  concurrency scenario  (stock 1)
--   SKU-003  INSUFFICIENT_STOCK    (stock 0)
--   SKU-004  UNKNOWN_PRODUCT       (no stock row)
INSERT INTO products (product_id, unit_price) VALUES
                                                  ('SKU-001', 19.99),
                                                  ('SKU-002', 49.90),
                                                  ('SKU-003',  5.50),
                                                  ('SKU-004', 12.00);
