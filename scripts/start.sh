#!/bin/bash
# HireSync Start Command
# Starts all services and the application

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/docker-utils.sh"
source "$(dirname "$0")/app-utils.sh"

# Handle cleanup on exit (Ctrl+C)
cleanup() {
  log_info "Received termination signal"
  log_success "Application stopped. Services remain running."
  exit 0
}

# Register cleanup function for SIGINT and SIGTERM
trap cleanup SIGINT SIGTERM

# Check prerequisites
if ! check_docker; then
  log_error "Docker must be running to start services"
  exit 1
fi

# Setup infrastructure
if ! setup_docker_infrastructure; then
  log_error "Failed to set up Docker infrastructure"
  exit 1
fi

# Start services
if ! start_services; then
  log_error "Failed to start services"
  exit 1
fi

log_success "Services started successfully. Starting application..."

# Start the application in foreground mode
if ! start_app; then
  log_error "Application failed to start, but services are still running"
  exit 1
fi

# This point is reached only if the application exits normally
log_info "Application has exited. Services remain running." 