#!/bin/bash
# HireSync Services Script
# Manage supporting services (PostgreSQL, etc.)

# Load common utilities
source "$(dirname "$0")/../utils/logging.sh"
source "$(dirname "$0")/../utils/docker.sh"

# Display usage information
show_usage() {
  echo -e "${BOLD}Usage:${NC} $0 [COMMAND]"
  echo
  echo -e "${BOLD}Commands:${NC}"
  echo "  start    Start all services"
  echo "  stop     Stop all services"
  echo "  restart  Restart all services"
  echo "  status   Show service status"
  echo "  logs     Show service logs"
  echo "  clean    Stop services and remove data volumes"
  echo
  echo -e "${BOLD}Examples:${NC}"
  echo "  $0 start    # Start all services"
  echo "  $0 stop     # Stop all services"
  echo "  $0 restart  # Restart all services"
  echo "  $0 logs     # Show logs from all services"
  echo
  exit 0
}

# Start services
start_cmd() {
  log_section "Starting HireSync Services"
  
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
  return 0
}

# Stop services
stop_cmd() {
  log_section "Stopping HireSync Services"
  
  if ! stop_services; then
    log_warning "Some services may still be running"
    return 1
  fi
  
  log_success "Services stopped successfully"
  return 0
}

# Restart services
restart_cmd() {
  log_section "Restarting HireSync Services"
  
  stop_cmd
  start_cmd
  
  return 0
}

# Show service status
status_cmd() {
  log_section "HireSync Services Status"
  
  get_services_status
  
  return 0
}

# Show service logs
logs_cmd() {
  log_section "HireSync Services Logs"
  
  show_service_logs
  
  return 0
}

# Clean services and data
clean_cmd() {
  log_section "Cleaning HireSync Environment"
  
  # Stop services first
  stop_cmd
  
  # Clean volumes with confirmation
  clean_volumes
  
  return 0
}

# Process command
process_command() {
  local command="$1"
  
  case "$command" in
    "start")
      start_cmd
      ;;
    "stop")
      stop_cmd
      ;;
    "restart")
      restart_cmd
      ;;
    "status")
      status_cmd
      ;;
    "logs")
      logs_cmd
      ;;
    "clean")
      clean_cmd
      ;;
    *)
      show_usage
      ;;
  esac
}

# Check if command is provided
if [ $# -eq 0 ]; then
  show_usage
else
  process_command "$1"
fi 