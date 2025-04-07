#!/usr/bin/env bash

# Description: Runs code quality checks using Checkstyle and PMD. Can fix some issues automatically.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default values
FIX_ISSUES=false
CHECKSTYLE_ONLY=false
PMD_ONLY=false
USE_CONTAINER=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --fix)
      FIX_ISSUES=true
      shift
      ;;
    --checkstyle)
      CHECKSTYLE_ONLY=true
      shift
      ;;
    --pmd)
      PMD_ONLY=true
      shift
      ;;
    --container)
      USE_CONTAINER=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: ./lint.sh [--fix] [--checkstyle] [--pmd] [--container]"
      exit 1
      ;;
  esac
done

echo "=== Code Linting ==="

# Function to run commands either in container or locally
run_command() {
  local command=$1
  
  if [[ "$USE_CONTAINER" == true ]]; then
    if ! docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
      echo "Error: Container hiresync-devtools is not running"
      exit 1
    fi
    
    echo "Running in container: $command"
    docker exec -it "hiresync-devtools" bash -c "cd /workspace && $command"
  else
    echo "Running locally: $command"
    (cd "$PROJECT_ROOT" && eval "$command")
  fi
  
  return $?
}

# Determine which linters to run
if [[ "$CHECKSTYLE_ONLY" == true ]]; then
  echo "Running Checkstyle only"
  run_command "mvn checkstyle:check -s settings.xml"
  
  EXIT_CODE=$?
  if [ $EXIT_CODE -eq 0 ]; then
    echo "Checkstyle passed"
  else
    echo "Checkstyle failed"
    exit 1
  fi
elif [[ "$PMD_ONLY" == true ]]; then
  echo "Running PMD only"
  run_command "mvn pmd:check -s settings.xml"
  
  EXIT_CODE=$?
  if [ $EXIT_CODE -eq 0 ]; then
    echo "PMD passed"
  else
    echo "PMD failed"
    exit 1
  fi
else
  # Run full linting suite
  echo "Running full linting suite"
  
  if [[ "$FIX_ISSUES" == true ]]; then
    echo "Attempting to fix issues where possible"
    run_command "mvn spotless:apply -s settings.xml"
  fi
  
  run_command "mvn verify -DskipTests -s settings.xml"
  
  EXIT_CODE=$?
  if [ $EXIT_CODE -eq 0 ]; then
    echo "All linting checks passed"
  else
    echo "Linting failed"
    exit 1
  fi
fi 