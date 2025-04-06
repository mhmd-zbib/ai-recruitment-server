#!/usr/bin/env bash

# Get the project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Docker container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Run all tests
run_all_tests() {
  echo "[INFO] Running all tests"
  
  if ! docker ps -q -f name="$DEVTOOLS_CONTAINER" &>/dev/null; then
    echo "[ERROR] Development container is not running"
    echo "[INFO] Please start the application first with: hiresync start --local"
    return 1
  fi
  
  echo "[INFO] Executing tests in $DEVTOOLS_CONTAINER container"
  if docker exec -i "$DEVTOOLS_CONTAINER" mvn test; then
    echo "[SUCCESS] All tests passed successfully"
    return 0
  else
    echo "[ERROR] Some tests failed"
    return 1
  fi
}

# Run unit tests only
run_unit_tests() {
  echo "[INFO] Running unit tests only"
  
  if ! docker ps -q -f name="$DEVTOOLS_CONTAINER" &>/dev/null; then
    echo "[ERROR] Development container is not running"
    echo "[INFO] Please start the application first with: hiresync start --local"
    return 1
  fi
  
  echo "[INFO] Executing unit tests in $DEVTOOLS_CONTAINER container"
  if docker exec -i "$DEVTOOLS_CONTAINER" mvn test -Dtest="*Test" -DexcludedGroups="integration,e2e"; then
    echo "[SUCCESS] All unit tests passed successfully"
    return 0
  else
    echo "[ERROR] Some unit tests failed"
    return 1
  fi
}

# Entry point
main() {
  if [ "$1" = "--unit" ]; then
    run_unit_tests
  else
    run_all_tests
  fi
}

# Execute the main function with all arguments
main "$@" 