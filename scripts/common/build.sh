#!/usr/bin/env bash

# Get the project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Docker container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Clean the project (remove target directory and built artifacts)
clean_project() {
  echo "[INFO] Cleaning project"
  
  if ! docker ps -q -f name="$DEVTOOLS_CONTAINER" &>/dev/null; then
    echo "[ERROR] Development container is not running"
    echo "[INFO] Please start the application first with: hiresync start --local"
    return 1
  fi
  
  echo "[INFO] Executing Maven clean in $DEVTOOLS_CONTAINER container"
  if docker exec -i "$DEVTOOLS_CONTAINER" mvn clean; then
    echo "[SUCCESS] Project cleaned successfully"
    return 0
  else
    echo "[ERROR] Failed to clean project"
    return 1
  fi
}

# Build the project
build_project() {
  echo "[INFO] Building project"
  
  if ! docker ps -q -f name="$DEVTOOLS_CONTAINER" &>/dev/null; then
    echo "[ERROR] Development container is not running"
    echo "[INFO] Please start the application first with: hiresync start --local"
    return 1
  fi
  
  echo "[INFO] Executing Maven package in $DEVTOOLS_CONTAINER container"
  if docker exec -i "$DEVTOOLS_CONTAINER" mvn package -DskipTests; then
    echo "[SUCCESS] Project built successfully"
    return 0
  else
    echo "[ERROR] Failed to build project"
    return 1
  fi
}

# Clean and build the project
clean_and_build() {
  echo "[INFO] Clean and build project"
  
  if ! docker ps -q -f name="$DEVTOOLS_CONTAINER" &>/dev/null; then
    echo "[ERROR] Development container is not running"
    echo "[INFO] Please start the application first with: hiresync start --local"
    return 1
  fi
  
  echo "[INFO] Executing Maven clean install in $DEVTOOLS_CONTAINER container"
  if docker exec -i "$DEVTOOLS_CONTAINER" mvn clean install -DskipTests; then
    echo "[SUCCESS] Project cleaned and built successfully"
    return 0
  else
    echo "[ERROR] Failed to clean and build project"
    return 1
  fi
}

# Entry point
main() {
  if [ "$1" = "--build" ]; then
    build_project
  elif [ "$1" = "--all" ]; then
    clean_and_build
  else
    clean_project
  fi
}

# Execute the main function with all arguments
main "$@" 