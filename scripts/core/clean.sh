#!/usr/bin/env bash

# Description: Cleans build artifacts, logs, and temporary files from the project.

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
CLEAN_DIST=true
CLEAN_LOGS=false
CLEAN_ALL=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-dist)
      CLEAN_DIST=false
      shift
      ;;
    --logs)
      CLEAN_LOGS=true
      shift
      ;;
    --all)
      CLEAN_ALL=true
      CLEAN_LOGS=true
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync clean [--no-dist] [--logs] [--all]"
      exit 1
      ;;
  esac
done

log_section "Cleaning Project"

# Clean Maven build artifacts
if [[ "$CLEAN_DIST" == true ]]; then
  log_info "Cleaning build artifacts"
  
  # Use container if available, otherwise run locally
  if docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
    docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn clean"
  else
    (cd "$PROJECT_ROOT" && mvn clean)
  fi
  
  # Remove any additional build artifacts
  if [ -d "$PROJECT_ROOT/target" ]; then
    log_info "Removing target directory"
    rm -rf "$PROJECT_ROOT/target"
  fi
fi

# Clean logs
if [[ "$CLEAN_LOGS" == true ]]; then
  log_info "Cleaning log files"
  
  if [ -d "$PROJECT_ROOT/logs" ]; then
    find "$PROJECT_ROOT/logs" -type f -name "*.log*" -exec rm {} \;
    log_success "Log files removed"
  else
    log_info "No log directory found"
  fi
fi

# Clean all temporary and generated files
if [[ "$CLEAN_ALL" == true ]]; then
  log_info "Cleaning all temporary files"
  
  # Remove IDE-specific files
  find "$PROJECT_ROOT" -type d -name ".idea" -prune -exec rm -rf {} \; 2>/dev/null || true
  find "$PROJECT_ROOT" -type d -name ".vscode" -prune -exec rm -rf {} \; 2>/dev/null || true
  find "$PROJECT_ROOT" -type d -name ".settings" -prune -exec rm -rf {} \; 2>/dev/null || true
  find "$PROJECT_ROOT" -type f -name "*.iml" -exec rm {} \; 2>/dev/null || true
  find "$PROJECT_ROOT" -type f -name ".classpath" -exec rm {} \; 2>/dev/null || true
  find "$PROJECT_ROOT" -type f -name ".project" -exec rm {} \; 2>/dev/null || true
  
  # Remove OS-specific files
  find "$PROJECT_ROOT" -type f -name ".DS_Store" -exec rm {} \; 2>/dev/null || true
  find "$PROJECT_ROOT" -type f -name "Thumbs.db" -exec rm {} \; 2>/dev/null || true
  
  log_success "All temporary files removed"
fi

log_success "Clean complete" 