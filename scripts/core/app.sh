#!/bin/bash
# HireSync App Script
# Start the Spring Boot application without starting services

# Load common utilities
source "$(dirname "$0")/../utils/logging.sh"
source "$(dirname "$0")/../utils/app.sh"

# Display usage information
show_usage() {
  echo -e "${BOLD}Usage:${NC} $0 [PROFILE] [OPTIONS]"
  echo
  echo -e "${BOLD}Profiles:${NC}"
  echo "  local    Development mode with hot reload (default)"
  echo "  dev      Development mode with remote database"
  echo "  test     Testing environment"
  echo "  prod     Production mode"
  echo
  echo -e "${BOLD}Options:${NC}"
  echo "  --debug  Enable remote debugging"
  echo
  echo -e "${BOLD}Examples:${NC}"
  echo "  $0                # Start with local profile"
  echo "  $0 prod           # Start with production profile"
  echo "  $0 dev --debug    # Start with dev profile and debugging enabled"
  echo
  exit 0
}

# Main execution
main() {
  # Process arguments
  local profile="local"
  local debug_mode=false
  
  for arg in "$@"; do
    case "$arg" in
      "--debug")
        debug_mode=true
        ;;
      "local"|"dev"|"test"|"prod")
        profile="$arg"
        ;;
      "--help"|"-h")
        show_usage
        ;;
      *)
        log_warning "Unknown argument: $arg"
        ;;
    esac
  done
  
  log_section "Starting HireSync Application (profile: $profile)"
  
  # Set debug mode if requested
  if [ "$debug_mode" = "true" ]; then
    export DEBUG_MODE=true
    log_info "Debug mode enabled on port ${DEBUG_PORT:-5005}"
  fi
  
  # Check for database before starting
  log_info "Checking for database connectivity..."
  if ! check_services; then
    log_warning "Database connection failed - application may not start properly"
  fi
  
  # Change to project root
  cd ../..
  
  # Start the application
  run_app "$profile"
  
  # This point is reached only on exit
  log_info "Application has exited"
  return 0
}

# Check if help is requested
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
  show_usage
else
  main "$@"
fi 