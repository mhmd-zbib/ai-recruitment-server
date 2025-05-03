#!/bin/bash
set -e

MAX_RETRIES=30
RETRY_INTERVAL=2

echo "Waiting for PostgreSQL to be ready..."
retries=0
until docker exec test-db pg_isready -U test -d testdb || [ $retries -eq $MAX_RETRIES ]; do
  echo "PostgreSQL is unavailable - sleeping for ${RETRY_INTERVAL}s"
  sleep $RETRY_INTERVAL
  retries=$((retries+1))
done

if [ $retries -eq $MAX_RETRIES ]; then
  echo "Error: PostgreSQL did not become ready in time"
  docker logs test-db
  exit 1
fi

echo "PostgreSQL is up and running!"