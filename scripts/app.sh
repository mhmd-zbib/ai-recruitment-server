#!/bin/bash
# HireSync App Command
# Starts only the Spring Boot application

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/app-utils.sh"

# Handle cleanup on exit (Ctrl+C)
cleanup() {
  log_success "Application stopped"
  exit 0
}

# Register cleanup function for SIGINT and SIGTERM
trap cleanup SIGINT SIGTERM

# Check if Docker is running
if ! check_docker; then
  log_error "Docker must be running to start the application"
  exit 1
fi

# Start application in foreground mode
if ! start_app; then
  log_error "Failed to start the application"
  exit 1
fi

# This point is reached only if the application exits normally
log_success "Application has exited" 