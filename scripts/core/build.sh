#!/usr/bin/env bash

# Description: Builds the application without starting it. Supports options to skip tests and create packages.

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
PACKAGE_ONLY=false
BUILD_ARGS=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --package)
      PACKAGE_ONLY=true
      shift
      ;;
    --clean)
      BUILD_ARGS="$BUILD_ARGS clean"
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync build [--skip-tests] [--package] [--clean]"
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

log_section "Building Application"

# Build the application
if [[ "$SKIP_TESTS" == true ]]; then
  BUILD_ARGS="$BUILD_ARGS -DskipTests"
  log_info "Skipping tests"
fi

if [[ "$PACKAGE_ONLY" == true ]]; then
  BUILD_ARGS="$BUILD_ARGS package"
  log_info "Building package only"
else
  BUILD_ARGS="$BUILD_ARGS compile"
  log_info "Compiling code"
fi

# Run the build
log_info "Executing build command"
docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn $BUILD_ARGS"

log_success "Build complete" 