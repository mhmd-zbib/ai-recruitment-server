#!/usr/bin/env bash

# Description: Runs application tests for both local development and CI

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
TEST_TYPE="all"        # Options: all, unit, integration, e2e
TEST_PROFILE="default" # Spring profile to use
USE_CONTAINER=false
MAVEN_OPTS="-B -ntp"
GENERATE_REPORTS=true
TEST_ARGS=""

# Check if running in CI environment
if [ -z "$CI" ] && docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
  USE_CONTAINER=true
fi

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --type=*)
      TEST_TYPE="${1#*=}"
      shift
      ;;
    --profile=*)
      TEST_PROFILE="${1#*=}"
      shift
      ;;
    --container)
      USE_CONTAINER=true
      shift
      ;;
    --no-container)
      USE_CONTAINER=false
      shift
      ;;
    --maven-opts=*)
      MAVEN_OPTS="${1#*=}"
      shift
      ;;
    --no-reports)
      GENERATE_REPORTS=false
      shift
      ;;
    --args=*)
      TEST_ARGS="${1#*=}"
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync run-tests [--type=all|unit|integration|e2e] [--profile=PROFILE] [--container] [--no-container] [--maven-opts=OPTIONS] [--no-reports] [--args=ARGS]"
      exit 1
      ;;
  esac
done

log_section "Running Tests"

# Function to run maven commands in container or locally
run_maven() {
  local command=$1
  
  if [ "$USE_CONTAINER" = true ]; then
    log_info "Running in container: $command"
    docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && $command"
    return $?
  else
    log_info "Running locally: $command"
    (cd "$PROJECT_ROOT" && eval "$command")
    return $?
  fi
}

# Set up the test command based on type
case "$TEST_TYPE" in
  all)
    log_info "Running all tests"
    TEST_CMD="mvn test $MAVEN_OPTS"
    ;;
  unit)
    log_info "Running unit tests only"
    TEST_CMD="mvn test -Dtest=\"*Test\" -DexcludedGroups=\"integration,e2e\" $MAVEN_OPTS"
    ;;
  integration)
    log_info "Running integration tests only"
    TEST_CMD="mvn test -Dgroups=\"integration\" $MAVEN_OPTS"
    ;;
  e2e)
    log_info "Running end-to-end tests only"
    TEST_CMD="mvn test -Dgroups=\"e2e\" $MAVEN_OPTS"
    ;;
  *)
    log_error "Unknown test type: $TEST_TYPE"
    echo "Valid test types: all, unit, integration, e2e"
    exit 1
    ;;
esac

# Add profile if specified and not default
if [ "$TEST_PROFILE" != "default" ]; then
  TEST_CMD="$TEST_CMD -Dspring.profiles.active=$TEST_PROFILE"
  log_info "Using profile: $TEST_PROFILE"
fi

# Add report generation flags
if [ "$GENERATE_REPORTS" = true ]; then
  TEST_CMD="$TEST_CMD -Djacoco.skip=false"
  log_info "Generating test reports"
fi

# Add any additional test arguments
if [ -n "$TEST_ARGS" ]; then
  TEST_CMD="$TEST_CMD $TEST_ARGS"
  log_info "Adding test arguments: $TEST_ARGS"
fi

# Run the tests
log_info "Executing test command"
run_maven "$TEST_CMD"

EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  log_error "Tests failed with exit code $EXIT_CODE"
  exit $EXIT_CODE
else
  log_success "All tests passed successfully"
  
  # Show report paths if reports were generated
  if [ "$GENERATE_REPORTS" = true ]; then
    log_info "Test reports available at: target/site/jacoco/index.html"
    log_info "Surefire reports available at: target/surefire-reports"
  fi
fi 