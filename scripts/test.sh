#!/bin/bash

# Navigate to the project root
cd "$(dirname "$0")/.."

# Load .env into the shell
export $(grep -v '^#' .env | xargs)

# Start Docker Compose in the background
docker compose -f docker/docker-compose.local.yaml up -d

# Run Maven tests
./mvnw test -Dtest=com.zbib.hiresync.unit.*Test
