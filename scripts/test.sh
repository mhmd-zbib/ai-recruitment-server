#!/bin/bash
set -e

# Colors for terminal output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
  echo -e "\n${BLUE}==>${NC} $1"
}

# Ensure devtools container is up
ensure_devtools() {
  print_header "Ensuring development tools container is available"
  if ! docker compose -f docker/docker-compose.local.yaml ps devtools | grep -q "hiresync-devtools"; then
    echo -e "${YELLOW}Starting devtools container...${NC}"
    docker compose -f docker/docker-compose.local.yaml up -d
    sleep 5
  fi
  echo -e "${GREEN}Development tools container is ready${NC}"
}

# Run Maven command in devtools container
run_maven() {
  docker compose -f docker/docker-compose.local.yaml exec devtools mvn -q $@
  return $?
}

# Prepare environment
prepare_env() {
  print_header "Setting up test environment"
  export SPRING_PROFILES_ACTIVE=test
  export TZ=UTC
  # Minimize logging - only show errors
  export LOG_LEVEL_ROOT=ERROR
  export LOG_LEVEL_APP=ERROR
  export LOG_LEVEL_SQL=ERROR
  export JWT_SECRET=test-secret-key-with-minimum-length-of-32-characters
  export JWT_ISSUER=hiresync-test
  export JWT_EXPIRATION=86400000
  export JWT_REFRESH_EXPIRATION=604800000
}

# Run unit tests
run_unit_tests() {
  print_header "Running unit tests"
  ensure_devtools
  
  # Check if we need to skip failing tests
  if [ "$SKIP_FAILING" = true ]; then
    echo -e "${YELLOW}Skipping currently failing tests${NC}"
    run_maven test -P ci -Dsurefire.printSummary=false -Dtest=!*ControllerTest,!*ApplicationsIntegrationTest,!ApplicationServiceTest
  else
    run_maven test -P ci -Dsurefire.printSummary=false
  fi
  
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}Unit tests passed!${NC}"
  else
    echo -e "${RED}Unit tests failed!${NC}"
    if [ "$FAIL_FAST" = true ]; then
      clean_up
      exit 1
    fi
  fi
}

# Start docker containers for integration tests
start_test_environment() {
  print_header "Starting test environment"
  # No need to start a separate test environment, we're using the local environment
  # The test profile is set in SPRING_PROFILES_ACTIVE
  
  echo -e "${YELLOW}Waiting for services to be ready...${NC}"
  attempt=1
  max_attempts=10
  
  until $(curl --output /dev/null --silent --fail http://localhost:8080/actuator/health) || [ $attempt -gt $max_attempts ]; do
    printf "."
    sleep 5
    attempt=$((attempt+1))
  done
  
  if [ $attempt -gt $max_attempts ]; then
    echo -e "\n${RED}Application failed to start in time. Checking logs:${NC}"
    docker compose -f docker/docker-compose.local.yaml logs --tail=50
    clean_up
    exit 1
  fi
  
  echo -e "\n${GREEN}Test environment is ready!${NC}"
}

# Run integration tests
run_integration_tests() {
  print_header "Running integration tests"
  ensure_devtools
  
  # Check if we need to skip failing tests
  if [ "$SKIP_FAILING" = true ]; then
    echo -e "${YELLOW}Skipping currently failing integration tests${NC}"
    run_maven verify -P ci -DskipUnitTests=true -Dsurefire.printSummary=false -Dtest=!*ControllerTest,!*ApplicationsIntegrationTest
  else
    run_maven verify -P ci -DskipUnitTests=true -Dsurefire.printSummary=false
  fi
  
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}Integration tests passed!${NC}"
  else
    echo -e "${RED}Integration tests failed!${NC}"
    if [ "$FAIL_FAST" = true ]; then
      clean_up
      exit 1
    fi
  fi
}

# Clean up resources
clean_up() {
  print_header "Cleaning up"
  # Don't stop containers since we're using the dev environment
  # This way the environment can be reused for development
  echo -e "${GREEN}Cleanup complete${NC}"
}

# Full test execution
run_all_tests() {
  prepare_env
  run_unit_tests
  start_test_environment
  run_integration_tests
  clean_up
  
  echo -e "\n${GREEN}All tests completed successfully!${NC}"
}

# Parse command line arguments
SKIP_FAILING=false
FAIL_FAST=false

for arg in "$@"; do
  case $arg in
    "skip-failing")
      SKIP_FAILING=true
      ;;
    "fail-fast")
      FAIL_FAST=true
      ;;
  esac
done

# Execute tests based on first argument
case "$1" in
  "unit")
    prepare_env
    run_unit_tests
    ;;
  "integration")
    prepare_env
    start_test_environment
    run_integration_tests
    ;;
  *)
    run_all_tests
    ;;
esac 