#!/bin/bash

# Navigate to the project root
cd "$(dirname "$0")/.."

# Load test environment variables
export $(grep -v '^#' .env.test | xargs)

# Start the test database using docker-compose
docker compose -f docker/docker-compose.test.yaml up -d postgres

# Wait for the database to be ready
./scripts/wait-for-db.sh

# Run Maven tests with the correct database URL
./mvnw test -Dtest=com.zbib.hiresync.integration.*Test \
  -Dspring.datasource.url=jdbc:postgresql://postgres:5432/testdb \
  -Dspring.datasource.username=test \
  -Dspring.datasource.password=test \
  -Dspring.profiles.active=test
