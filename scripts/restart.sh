#!/bin/bash
# HireSync Restart Command
# Restarts all services and the application

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/docker-utils.sh"
source "$(dirname "$0")/app-utils.sh"

# Check Docker 
if ! check_docker; then
  log_error "Docker must be running for restart operation"
  exit 1
fi

# Stop services first
if ! stop_services; then
  log_warning "Failed to stop some services, continuing with restart anyway"
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

# Start application
if ! start_app; then
  log_error "Failed to start application, but services are running"
  exit 1
fi

log_success "HireSync has been restarted successfully" 