#!/bin/bash
# HireSync Services Command
# Starts only the supporting services (PostgreSQL, etc.)

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/docker-utils.sh"

# Handle cleanup on exit (Ctrl+C)
cleanup() {
  log_info "Exiting script. Services will continue running."
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

log_success "Supporting services have been started. Press Ctrl+C to exit this script (services will continue running)."

# Keep the script running until Ctrl+C is pressed
while true; do
  sleep 1
done 