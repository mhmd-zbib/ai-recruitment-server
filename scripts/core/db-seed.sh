#!/usr/bin/env bash

# Description: Populates the database with test data for development, testing, or demo purposes.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"

# Container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Default values
ENVIRONMENT="dev"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --environment=*)
      ENVIRONMENT="${1#*=}"
      shift
      ;;
    --env=*)
      ENVIRONMENT="${1#*=}"
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync db-seed [--environment=dev|test|demo]"
      exit 1
      ;;
  esac
done

log_section "Database Seed"
log_info "Seeding database with $ENVIRONMENT data"

# Run the seed command according to the environment
case "$ENVIRONMENT" in
  dev)
    docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn spring-boot:run -Dspring-boot.run.arguments=--seed=development"
    ;;
  test)
    docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn spring-boot:run -Dspring-boot.run.arguments=--seed=test"
    ;;
  demo)
    docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn spring-boot:run -Dspring-boot.run.arguments=--seed=demo"
    ;;
  *)
    log_error "Unknown environment: $ENVIRONMENT"
    echo "Valid environments: dev, test, demo"
    exit 1
    ;;
esac

log_success "Database seeded successfully with $ENVIRONMENT data" 