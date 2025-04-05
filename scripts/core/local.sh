#!/bin/bash
# HireSync Local Development Script
# Run the application in local development mode with hot-reload

# Load common utilities
source "$(dirname "$0")/../utils/logging.sh"
source "$(dirname "$0")/../utils/docker.sh"
source "$(dirname "$0")/../utils/app.sh"

# Handle cleanup on exit (Ctrl+C)
cleanup() {
  log_info "Stopping local development environment"
  exit 0
}

# Register cleanup function for SIGINT and SIGTERM
trap cleanup SIGINT SIGTERM

# Display usage information
show_usage() {
  echo -e "${BOLD}Usage:${NC} $0 [OPTIONS]"
  echo
  echo -e "${BOLD}Description:${NC}"
  echo "  Start the HireSync application in local development mode with hot reload"
  echo
  echo -e "${BOLD}Options:${NC}"
  echo "  --no-services  Don't start Docker services"
  echo "  --debug        Enable remote debugging"
  echo "  --seed         Seed development data"
  echo "  --migrate      Run database migrations"
  echo "  --help, -h     Show this help message"
  echo
  exit 0
}

# Main execution
main() {
  # Process arguments
  local start_services=true
  local seed_data=false
  local run_migrations=false
  
  for arg in "$@"; do
    case "$arg" in
      "--no-services")
        start_services=false
        ;;
      "--debug")
        export DEBUG_MODE=true
        ;;
      "--seed")
        seed_data=true
        ;;
      "--migrate")
        run_migrations=true
        ;;
      "--help"|"-h")
        show_usage
        ;;
      *)
        log_warning "Unknown argument: $arg"
        ;;
    esac
  done
  
  # Display local mode header
  log_section "Starting HireSync Local Development Environment"
  
  # Initialize Docker environment
  if [ "$start_services" = "true" ]; then
    if ! init_docker_env; then
      log_error "Failed to initialize Docker environment"
      exit 1
    fi
    
    # Start necessary services
    if ! start_services "$COMPOSE_DEV_FILE"; then
      log_error "Failed to start services"
      exit 1
    fi
    
    # Check database connectivity
    check_database "localhost" "$DB_PORT" 10 2
  else
    log_info "Skipping services startup (--no-services flag used)"
  fi
  
  # Run database migrations if requested
  if [ "$run_migrations" = "true" ]; then
    # Change to project root
    cd ../..
    if ! run_migrations "local"; then
      log_error "Failed to run migrations"
      exit 1
    fi
  fi
  
  # Seed development data if requested
  if [ "$seed_data" = "true" ]; then
    # Change to project root (if not already there)
    cd ../..
    log_info "Seeding development data..."
    
    if ! $MVN_CMD exec:java -Dexec.mainClass="com.zbib.hiresync.tools.DevDataSeeder" -Dexec.classpathScope=test; then
      log_error "Failed to seed development data"
      exit 1
    fi
    
    log_success "Development data seeded successfully"
  fi
  
  # Start the application in local development mode
  log_info "Starting application in local development mode"
  
  # Change to project root (if not already there)
  cd ../..
  
  # Run the application with hot reload
  run_app "local" true
  
  return 0
}

# Check if help is requested
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
  show_usage
else
  main "$@"
fi 