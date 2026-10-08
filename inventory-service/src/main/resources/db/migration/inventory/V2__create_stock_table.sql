-- V2: available stock per product (ADR-0010, BR-015, BR-016).
-- quantity is AVAILABLE stock: a reservation decrements it, and v0.1 never releases it (BR-016).
-- The CHECK is a second barrier against overselling, independent of the conditional UPDATE used
-- by reservations: any future bug that tries to go below zero fails instead of overselling.
CREATE TABLE stock (
                       product_id varchar(64) PRIMARY KEY,
                       quantity   int         NOT NULL CHECK (quantity >= 0)
);
