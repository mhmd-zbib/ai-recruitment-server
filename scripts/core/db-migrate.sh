#!/usr/bin/env bash

# Description: Runs database migrations using Liquibase. Supports checking status, rollback, and dry-run.

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
MIGRATION_ARGS=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --check)
      MIGRATION_ARGS="$MIGRATION_ARGS liquibase:status"
      shift
      ;;
    --rollback)
      if [[ -z "$2" || "$2" =~ ^- ]]; then
        log_error "Missing rollback count"
        exit 1
      fi
      MIGRATION_ARGS="$MIGRATION_ARGS liquibase:rollback -Dliquibase.rollbackCount=$2"
      shift 2
      ;;
    --dry-run)
      MIGRATION_ARGS="$MIGRATION_ARGS -Dliquibase.dryRun=true"
      shift
      ;;
    *)
      MIGRATION_ARGS="$MIGRATION_ARGS liquibase:update"
      break
      ;;
  esac
done

# Check if container is running
if ! docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
  log_error "Development container is not running"
  log_info "Start it first with ./hiresync start"
  exit 1
fi

log_section "Database Migration"

# Run the migration
log_info "Running database migration"
docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn $MIGRATION_ARGS"

log_success "Migration complete" 