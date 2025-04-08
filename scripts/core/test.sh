#!/usr/bin/env bash

# Description: Runs unit tests or all tests in the development container.

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"

# Container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Generate test JWT secret if not exists
generate_test_jwt_secret() {
  local test_props="src/test/resources/application-test.properties"
  if [ ! -f "$test_props" ] || ! grep -q "^jwt.secret=" "$test_props"; then
    log_info "Generating test JWT secret..."
    mkdir -p "$(dirname "$test_props")"
    echo "jwt.secret=test-secret-key-with-minimum-length-of-32-characters" > "$test_props"
  fi
}

# Run all tests
run_all_tests() {
  log_info "Running All Tests"
  generate_test_jwt_secret
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn test -Dspring.profiles.active=test"
}

# Run unit tests only
run_unit_tests() {
  log_info "Running Unit Tests"
  generate_test_jwt_secret
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn test -Dtest=\"*Test\" -DexcludedGroups=\"integration,e2e\" -Dspring.profiles.active=test"
}

# Check if running unit tests only
if [[ "$1" == "--unit" ]]; then
  run_unit_tests
else
  run_all_tests
fi
