#!/usr/bin/env bash

# Description: Runs code quality checks using Checkstyle and PMD. Can fix some issues automatically.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"

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
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync lint [--fix] [--checkstyle] [--pmd] [--container]"
      exit 1
      ;;
  esac
done

log_section "Code Linting"

# Function to run commands either in container or locally
run_command() {
  local command=$1
  
  if [[ "$USE_CONTAINER" == true ]]; then
    if ! docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
      log_error "Container hiresync-devtools is not running"
      exit 1
    fi
    
    log_info "Running in container: $command"
    docker exec -it "hiresync-devtools" bash -c "cd /workspace && $command"
  else
    log_info "Running locally: $command"
    (cd "$PROJECT_ROOT" && eval "$command")
  fi
  
  return $?
}

# Determine which linters to run
if [[ "$CHECKSTYLE_ONLY" == true ]]; then
  log_info "Running Checkstyle only"
  run_command "mvn checkstyle:check"
  
  EXIT_CODE=$?
  if [ $EXIT_CODE -eq 0 ]; then
    log_success "Checkstyle passed"
  else
    log_error "Checkstyle failed"
    exit 1
  fi
elif [[ "$PMD_ONLY" == true ]]; then
  log_info "Running PMD only"
  run_command "mvn pmd:check"
  
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
    run_command "mvn spotless:apply"
  fi
  
  run_command "mvn verify -DskipTests"
  
  EXIT_CODE=$?
  if [ $EXIT_CODE -eq 0 ]; then
    log_success "All linting checks passed"
  else
    log_error "Linting failed"
    exit 1
  fi
fi 