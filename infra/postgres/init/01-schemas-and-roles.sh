#!/usr/bin/env bash
# One schema and one owner role per service (ADR-0002, ADR-0015).
# Runs once, on first initialization of an empty data directory.
set -euo pipefail

: "${ORDER_DB_PASSWORD:?ORDER_DB_PASSWORD is required}"
: "${INVENTORY_DB_PASSWORD:?INVENTORY_DB_PASSWORD is required}"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -v db_name="$POSTGRES_DB" \
     -v order_password="$ORDER_DB_PASSWORD" \
     -v inventory_password="$INVENTORY_DB_PASSWORD" <<'EOSQL'
-- Nothing is accessible unless explicitly granted.
REVOKE ALL ON DATABASE :"db_name" FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;

CREATE ROLE order_svc LOGIN PASSWORD :'order_password';
CREATE ROLE inventory_svc LOGIN PASSWORD :'inventory_password';

GRANT CONNECT ON DATABASE :"db_name" TO order_svc, inventory_svc;

-- Each role owns its schema: full rights there, none on the other one.
CREATE SCHEMA order_service AUTHORIZATION order_svc;
CREATE SCHEMA inventory_service AUTHORIZATION inventory_svc;

ALTER ROLE order_svc SET search_path = order_service;
ALTER ROLE inventory_svc SET search_path = inventory_service;
EOSQL
