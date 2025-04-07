#!/usr/bin/env bash

# Description: Runs code quality checks for both local development and CI

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
FIX_ISSUES=true
RUN_CHECKSTYLE=true
RUN_PMD=true
MAVEN_OPTS="-B -ntp"
USE_CONTAINER=false

# Check if running in CI environment
if [ -z "$CI" ] && docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
  USE_CONTAINER=true
fi

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-fix)
      FIX_ISSUES=false
      shift
      ;;
    --no-checkstyle)
      RUN_CHECKSTYLE=false
      shift
      ;;
    --no-pmd)
      RUN_PMD=false
      shift
      ;;
    --maven-opts=*)
      MAVEN_OPTS="${1#*=}"
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
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync code-quality [--no-fix] [--no-checkstyle] [--no-pmd] [--maven-opts=OPTIONS] [--container] [--no-container]"
      exit 1
      ;;
  esac
done

log_section "Running Code Quality Checks"

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

# Format code with spotless
if [ "$FIX_ISSUES" == "true" ]; then
  log_info "Formatting code with Spotless"
  run_maven "mvn spotless:apply $MAVEN_OPTS"
  
  if [ $? -ne 0 ]; then
    log_error "Code formatting failed"
    exit 1
  else
    log_success "Code formatted successfully"
  fi
fi

# Run Checkstyle
if [ "$RUN_CHECKSTYLE" == "true" ]; then
  log_info "Running Checkstyle checks"
  run_maven "mvn checkstyle:check $MAVEN_OPTS"
  
  if [ $? -ne 0 ]; then
    log_error "Checkstyle check failed"
    exit 1
  else
    log_success "Checkstyle check passed"
  fi
fi

# Run PMD
if [ "$RUN_PMD" == "true" ]; then
  log_info "Running PMD checks"
  run_maven "mvn pmd:check $MAVEN_OPTS"
  
  if [ $? -ne 0 ]; then
    log_error "PMD check failed"
    exit 1
  else
    log_success "PMD check passed"
  fi
fi

log_success "All code quality checks passed" 