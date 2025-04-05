#!/bin/bash
# HireSync Local Development Start Command
# Starts all services and the application with hot reloading

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/docker-utils.sh"
source "$(dirname "$0")/app-utils.sh"

# Handle cleanup on exit (Ctrl+C)
cleanup() {
  log_info "Received termination signal"
  log_success "Development server stopped. Services remain running."
  exit 0
}

# Register cleanup function for SIGINT and SIGTERM
trap cleanup SIGINT SIGTERM

# Check prerequisites
if ! check_docker; then
  log_error "Docker must be running to start services"
  exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
  log_error "Maven is required for development mode"
  exit 1
fi

log_section "HireSync Local Development Mode (with Hot Reload)"

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

log_success "Services started successfully. Starting application in development mode..."

# Configure Spring Boot devtools for hot reloading
export SPRING_DEVTOOLS_RESTART_ENABLED=true
export SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true

# Start the application with Spring Boot DevTools for hot reloading
log_info "Starting application with hot reloading enabled..."
log_warning "Any changes to Java files will trigger automatic restart"

cd ..
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true"

# This point is reached only if the application exits normally
log_info "Development server has exited. Services remain running." 