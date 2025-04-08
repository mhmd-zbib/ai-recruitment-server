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

# Check if devtools container is running
check_devtools_container() {
  if ! docker ps | grep -q "$DEVTOOLS_CONTAINER"; then
    log_error "Devtools container ($DEVTOOLS_CONTAINER) is not running!"
    log_info "Please start the development environment first:"
    log_info "docker compose -f docker/docker-compose.local.yaml up -d"
    exit 1
  fi
}

# Run all tests
run_all_tests() {
  log_info "Running All Tests"
  check_devtools_container
  generate_test_jwt_secret
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn test -Dspring.profiles.active=test"
}

# Run unit tests only
run_unit_tests() {
  log_info "Running Unit Tests"
  check_devtools_container
  generate_test_jwt_secret
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn test -Dtest=\"*Test\" -DexcludedGroups=\"integration,e2e\" -Dspring.profiles.active=test"
}

# Run integration tests only
run_integration_tests() {
  log_info "Running Integration Tests"
  check_devtools_container
  generate_test_jwt_secret
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn verify -DskipTests=true -Dspring.profiles.active=test"
}

# Parse command line arguments
case "$1" in
  --unit)
    run_unit_tests
    ;;
  --integration)
    run_integration_tests
    ;;
  --help|-h)
    echo "Usage: $0 [OPTION]"
    echo "Options:"
    echo "  --unit          Run only unit tests"
    echo "  --integration   Run only integration tests"
    echo "  --help, -h      Show this help message"
    echo "  (no option)     Run all tests"
    exit 0
    ;;
  *)
    run_all_tests
    ;;
esac
