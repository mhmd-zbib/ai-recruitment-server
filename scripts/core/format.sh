#!/usr/bin/env bash

# Description: Formats code according to project standards using Spotless. Can check formatting without changing files.

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
CHECK_ONLY=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --check)
      CHECK_ONLY=true
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync format [--check]"
      exit 1
      ;;
  esac
done

# Check if container is running
if ! docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
  log_error "Development container is not running"
  log_info "Start it first with ./hiresync start"
  exit 1
fi

log_section "Code Formatting"

# Format code or just check for formatting issues
if [[ "$CHECK_ONLY" == true ]]; then
  log_info "Checking code formatting"
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn spotless:check"
  
  if [ $? -eq 0 ]; then
    log_success "Code formatting check passed"
  else
    log_error "Code formatting check failed"
    log_info "Run './hiresync format' to fix formatting issues"
    exit 1
  fi
else
  log_info "Formatting code"
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn spotless:apply"
  log_success "Code formatted successfully"
fi 