#!/bin/bash
# HireSync Start Script
# Start the application in production mode

# Load common utilities
source "$(dirname "$0")/../utils/logging.sh"
source "$(dirname "$0")/../utils/docker.sh"
source "$(dirname "$0")/../utils/app.sh"

# Handle cleanup on exit (Ctrl+C)
cleanup() {
  log_info "Received termination signal"
  log_success "Application stopped. Services remain running."
  exit 0
}

# Register cleanup function for SIGINT and SIGTERM
trap cleanup SIGINT SIGTERM

# Main execution
main() {
  log_section "HireSync Production Mode"
  
  # Check prerequisites
  if ! check_docker; then
    log_error "Docker must be running to start services"
    exit 1
  fi
  
  # Set up network
  if ! create_docker_network; then
    log_error "Failed to set up Docker network"
    exit 1
  fi
  
  # Start services
  if ! start_services; then
    log_error "Failed to start services"
    exit 1
  fi
  
  log_success "Services started successfully"
  
  # Wait for services to be ready
  if ! check_services; then
    log_error "Required services are not ready"
    exit 1
  fi
  
  log_section "Starting HireSync in Production Mode"
  
  # Change to project root
  cd ../..
  
  # Start application
  run_app prod
  
  # This point is reached only on exit
  log_info "Application has exited. Services remain running."
  return 0
}

# Execute main function
main 