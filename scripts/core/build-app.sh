#!/usr/bin/env bash

# Description: Builds the application for both local development and CI

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
SKIP_TESTS=false
CLEAN_BUILD=false
RUN_PACKAGE=true
BUILD_PROFILE="default"
USE_CONTAINER=false
MAVEN_OPTS="-B -ntp"
BUILD_ARGS=""

# Check if running in CI environment
if [ -z "$CI" ] && docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
  USE_CONTAINER=true
fi

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --clean)
      CLEAN_BUILD=true
      shift
      ;;
    --no-package)
      RUN_PACKAGE=false
      shift
      ;;
    --profile=*)
      BUILD_PROFILE="${1#*=}"
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
    --args=*)
      BUILD_ARGS="${1#*=}"
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync build-app [--skip-tests] [--clean] [--no-package] [--profile=PROFILE] [--container] [--no-container] [--maven-opts=OPTIONS] [--args=ARGS]"
      exit 1
      ;;
  esac
done

log_section "Building Application"

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

# Build the Maven command
if [ "$CLEAN_BUILD" = true ]; then
  MVN_CMD="mvn clean"
  log_info "Performing clean build"
else
  MVN_CMD="mvn"
fi

if [ "$RUN_PACKAGE" = true ]; then
  MVN_CMD="$MVN_CMD package"
  log_info "Building package"
else
  MVN_CMD="$MVN_CMD compile"
  log_info "Compiling code only"
fi

if [ "$SKIP_TESTS" = true ]; then
  MVN_CMD="$MVN_CMD -DskipTests"
  log_info "Skipping tests"
fi

if [ "$BUILD_PROFILE" != "default" ]; then
  MVN_CMD="$MVN_CMD -Dspring.profiles.active=$BUILD_PROFILE"
  log_info "Using profile: $BUILD_PROFILE"
fi

# Add Maven options
MVN_CMD="$MVN_CMD $MAVEN_OPTS"

# Add any additional build arguments
if [ -n "$BUILD_ARGS" ]; then
  MVN_CMD="$MVN_CMD $BUILD_ARGS"
  log_info "Adding build arguments: $BUILD_ARGS"
fi

# Run the build
log_info "Executing build command"
run_maven "$MVN_CMD"

EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  log_error "Build failed with exit code $EXIT_CODE"
  exit $EXIT_CODE
else
  log_success "Build completed successfully"
  
  # Show the build artifacts if we packaged
  if [ "$RUN_PACKAGE" = true ]; then
    JAR_FILE=$(find "$PROJECT_ROOT/target" -name "*.jar" | grep -v "sources" | grep -v "javadoc" | head -1)
    if [ -n "$JAR_FILE" ]; then
      log_info "Build artifact: $JAR_FILE"
    fi
  fi
fi 