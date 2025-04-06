#!/usr/bin/env bash

# Description: Checks for security vulnerabilities in project dependencies and generates a report.

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
REPORT_FORMAT="HTML"
UPDATE_ONLY=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --format=*)
      REPORT_FORMAT="${1#*=}"
      shift
      ;;
    --update)
      UPDATE_ONLY=true
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync dependency-check [--format=HTML|XML|CSV|JSON] [--update]"
      exit 1
      ;;
  esac
done

# Convert report format to uppercase
REPORT_FORMAT=$(echo "$REPORT_FORMAT" | tr '[:lower:]' '[:upper:]')

# Check if container is running
if ! docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
  log_error "Development container is not running"
  log_info "Start it first with ./hiresync start"
  exit 1
fi

# Validate report format
case "$REPORT_FORMAT" in
  HTML|XML|CSV|JSON)
    ;;
  *)
    log_error "Invalid report format: $REPORT_FORMAT"
    echo "Valid formats: HTML, XML, CSV, JSON"
    exit 1
    ;;
esac

log_section "Dependency Security Check"

if [[ "$UPDATE_ONLY" == true ]]; then
  # Just update the local database
  log_info "Updating dependency check database"
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn org.owasp:dependency-check-maven:update-only"
  log_success "Dependency check database updated"
else
  # Run the full check
  log_info "Running dependency check (format: $REPORT_FORMAT)"
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn org.owasp:dependency-check-maven:check -Dformat=$REPORT_FORMAT"
  
  # Show report location
  log_success "Dependency check complete"
  log_info "Report available at: target/dependency-check-report.${REPORT_FORMAT,,}"
fi 