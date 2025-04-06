#!/usr/bin/env bash

# Description: Runs code quality checks using Checkstyle and PMD. Can fix some issues automatically.

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
FIX_ISSUES=false
CHECKSTYLE_ONLY=false
PMD_ONLY=false

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
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync lint [--fix] [--checkstyle] [--pmd]"
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

log_section "Code Linting"

# Determine which linters to run
if [[ "$CHECKSTYLE_ONLY" == true ]]; then
  log_info "Running Checkstyle only"
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn checkstyle:check"
  
  EXIT_CODE=$?
  if [ $EXIT_CODE -eq 0 ]; then
    log_success "Checkstyle passed"
  else
    log_error "Checkstyle failed"
    exit 1
  fi
elif [[ "$PMD_ONLY" == true ]]; then
  log_info "Running PMD only"
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn pmd:check"
  
  EXIT_CODE=$?
  if [ $EXIT_CODE -eq 0 ]; then
    log_success "PMD passed"
  else
    log_error "PMD failed"
    exit 1
  fi
else
  # Run full linting suite
  log_info "Running full linting suite"
  
  if [[ "$FIX_ISSUES" == true ]]; then
    log_info "Attempting to fix issues where possible"
    docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn spotless:apply"
  fi
  
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn verify -DskipTests"
  
  EXIT_CODE=$?
  if [ $EXIT_CODE -eq 0 ]; then
    log_success "All linting checks passed"
  else
    log_error "Linting failed"
    exit 1
  fi
fi 