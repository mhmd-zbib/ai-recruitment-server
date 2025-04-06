#!/usr/bin/env bash

# Description: Resets the database to a clean state by recreating containers and running fresh migrations.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"

# Container names
DEVTOOLS_CONTAINER="hiresync-devtools"
DB_CONTAINER="hiresync-postgres"

# Default values
COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.local.yaml"
CONFIRM=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      CONFIRM=true
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync db-reset [--force]"
      exit 1
      ;;
  esac
done

# Check Docker is installed and running
if ! docker info &>/dev/null; then
  log_error "Docker is not running or not installed."
  exit 1
fi

log_section "Database Reset"

# Confirmation
if [[ "$CONFIRM" != true ]]; then
  log_warning "This will DELETE ALL DATA in your local database!"
  read -p "Are you sure you want to continue? (y/N) " -r
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    log_info "Database reset cancelled"
    exit 0
  fi
fi

# Stop the containers first
log_info "Stopping running containers"
docker compose -f "$COMPOSE_FILE" down

# Recreate the containers
log_info "Recreating database"
docker compose -f "$COMPOSE_FILE" up -d "$DB_CONTAINER"

# Wait for the database to be ready
log_info "Waiting for database to be ready"
docker exec "$DB_CONTAINER" bash -c "until pg_isready -q; do sleep 1; done;"

# Start the devtools container
log_info "Starting devtools container"
docker compose -f "$COMPOSE_FILE" up -d "$DEVTOOLS_CONTAINER"

# Run the migrations
log_info "Running migrations"
docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn liquibase:update"

log_success "Database reset complete" 