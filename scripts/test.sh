#!/bin/bash

# Navigate to the project root
cd "$(dirname "$0")/.."

# Load .env into the shell
export $(grep -v '^#' .env | xargs)

# Start Docker Compose for test environment (assuming docker-compose.test.yaml for test configurations)
docker compose -f docker/docker-compose.test.yaml up -d

# Run Maven tests
./mvnw test
