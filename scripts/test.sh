#!/bin/bash

# Navigate to the project root
cd "$(dirname "$0")/.."

# Load .env into the shell
export $(grep -v '^#' .env | xargs)

# Run Maven tests
./mvnw test -Dtest=com.zbib.hiresync.integration.*Test
