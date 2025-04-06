#!/usr/bin/env bash

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/logging.sh"

# Container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Run all tests
run_all_tests() {
  log_info "Running All Tests"
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn test"
}

# Run unit tests only
run_unit_tests() {
  log_info "Running Unit Tests"
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn test -Dtest=\"*Test\" -DexcludedGroups=\"integration,e2e\""
}

# Check if container is running
if ! docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
  log_error "Development container is not running"
  log_info "Start it first with ./hiresync start"
  exit 1
fi

# Check if running unit tests only
if [[ "$1" == "--unit" ]]; then
  run_unit_tests
else
  run_all_tests
fi
