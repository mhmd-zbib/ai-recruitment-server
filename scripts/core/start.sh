#!/usr/bin/env bash

# Description: Starts the application in local development mode with Docker containers.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$PROJECT_ROOT/scripts/utils/logging.sh"
source "$PROJECT_ROOT/scripts/utils/env.sh"

# Default values
ENV_FILE="$PROJECT_ROOT/.env"
COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.local.yaml"
DEVTOOLS_CONTAINER="hiresync-devtools"
DB_CONTAINER="hiresync-postgres"

log_section "Starting HireSync (Local Development)"

# Check Docker is installed and running
if ! docker info &>/dev/null; then
  log_error "Docker is not running or not installed. Please start Docker and try again."
  exit 1
fi

# Load environment variables from .env file
log_info "Loading environment variables"
if [ -f "$ENV_FILE" ]; then
  export $(grep -v '^#' "$ENV_FILE" | xargs)
else
  log_warning ".env file not found, using default values"
fi

# Check if containers are already running
log_info "Checking services status"
DEVTOOLS_RUNNING=$(docker ps --format '{{.Names}}' | grep -w "$DEVTOOLS_CONTAINER" || echo "")
DB_RUNNING=$(docker ps --format '{{.Names}}' | grep -w "$DB_CONTAINER" || echo "")

# Start only stopped containers
if [ -z "$DEVTOOLS_RUNNING" ] || [ -z "$DB_RUNNING" ]; then
  log_info "Starting containers"
  docker compose -f "$COMPOSE_FILE" up -d
else
  log_info "Containers already running, skipping startup"
fi

# Execute spring-boot:run in the devtools container with optimized settings
log_section "Starting Spring Boot Application"
log_info "Starting Spring Boot application"

docker exec -it "$DEVTOOLS_CONTAINER" bash -c 'cd /workspace && mvn spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-XX:TieredStopAtLevel=1 -Xverify:none -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"'

# If we get here, the application has stopped
log_info "Application has stopped."
exit 0