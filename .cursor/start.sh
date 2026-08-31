#!/usr/bin/env bash
# Per-boot reconciliation for the Klarblatt Cloud Agent dev environment.
# Starts PostgreSQL and a local mail catcher (mailpit), then ensures the
# application database and role exist. Safe to run repeatedly.
set -euo pipefail

DB_NAME="${POSTGRES_DB:-kindle_rss}"
DB_USER="${POSTGRES_USER:-kindle}"
DB_PASSWORD="${POSTGRES_PASSWORD:-kindle}"

echo "[start] Ensuring PostgreSQL cluster is running..."
sudo pg_ctlcluster 16 main start >/dev/null 2>&1 || true

# Wait for the server to accept connections.
for _ in $(seq 1 30); do
  if sudo -u postgres pg_isready -q; then
    break
  fi
  sleep 1
done

echo "[start] Ensuring database role and database exist..."
sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASSWORD}';"
sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};"

echo "[start] Ensuring mailpit (local SMTP + web UI) is running..."
if ! pgrep -x mailpit >/dev/null 2>&1; then
  nohup mailpit --smtp 0.0.0.0:1025 --listen 0.0.0.0:8025 \
    >/tmp/mailpit.log 2>&1 &
fi

echo "[start] Ready. PostgreSQL on :5432, mailpit SMTP on :1025, mailpit UI on :8025."
