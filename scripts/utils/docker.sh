#!/bin/bash
# HireSync Docker Utilities
# Common functions for Docker and container management

# Load dependencies
source "$(dirname "$0")/logging.sh"

# Project specific variables
DOCKER_NETWORK="hiresync-network"
COMPOSE_FILE="../docker-compose.yml"
COMPOSE_DEV_FILE="../docker-compose.dev.yml"
DB_CONTAINER="hiresync-postgres"
DB_PORT="5432"

# Check if Docker is installed and running
check_docker() {
  if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed"
    return 1
  fi
  
  if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running"
    return 1
  fi
  
  log_debug "Docker is running"
  return 0
}

# Create Docker network if it doesn't exist
create_docker_network() {
  if ! docker network inspect "$DOCKER_NETWORK" &> /dev/null; then
    log_info "Creating Docker network: $DOCKER_NETWORK"
    if ! docker network create "$DOCKER_NETWORK" &> /dev/null; then
      log_error "Failed to create Docker network"
      return 1
    fi
    log_success "Created Docker network: $DOCKER_NETWORK"
  else
    log_debug "Docker network already exists: $DOCKER_NETWORK"
  fi
  return 0
}

# Check if Docker Compose is installed
check_docker_compose() {
  if docker compose version &> /dev/null; then
    log_debug "Using Docker Compose V2"
    DOCKER_COMPOSE="docker compose"
    return 0
  elif command -v docker-compose &> /dev/null; then
    log_debug "Using Docker Compose V1"
    DOCKER_COMPOSE="docker-compose"
    return 0
  else
    log_error "Docker Compose is not installed"
    return 1
  fi
}

# Start services using docker-compose
start_services() {
  local compose_file="${1:-$COMPOSE_FILE}"
  local profile="${2:-}"
  local profile_arg=""
  
  if [[ -n "$profile" ]]; then
    profile_arg="--profile $profile"
  fi
  
  log_info "Starting services using $compose_file"
  if ! $DOCKER_COMPOSE -f "$compose_file" up -d $profile_arg; then
    log_error "Failed to start services"
    return 1
  fi
  
  log_success "Services started successfully"
  return 0
}

# Stop services using docker-compose
stop_services() {
  local compose_file="${1:-$COMPOSE_FILE}"
  
  log_info "Stopping services"
  if ! $DOCKER_COMPOSE -f "$compose_file" down; then
    log_error "Failed to stop services"
    return 1
  fi
  
  log_success "Services stopped successfully"
  return 0
}

# Get services status
get_services_status() {
  local compose_file="${1:-$COMPOSE_FILE}"
  
  log_info "Services status:"
  $DOCKER_COMPOSE -f "$compose_file" ps
  return $?
}

# Show service logs
show_service_logs() {
  local compose_file="${1:-$COMPOSE_FILE}"
  local service="${2:-}"
  local tail_lines="${3:-100}"
  
  if [[ -n "$service" ]]; then
    log_info "Showing logs for service: $service"
    $DOCKER_COMPOSE -f "$compose_file" logs --tail="$tail_lines" -f "$service"
  else
    log_info "Showing logs for all services"
    $DOCKER_COMPOSE -f "$compose_file" logs --tail="$tail_lines" -f
  fi
  
  return $?
}

# Clean Docker volumes (with confirmation)
clean_volumes() {
  local compose_file="${1:-$COMPOSE_FILE}"
  
  if confirm "This will remove all Docker volumes and data. Are you sure?"; then
    log_info "Removing Docker volumes"
    
    if ! $DOCKER_COMPOSE -f "$compose_file" down -v; then
      log_error "Failed to remove Docker volumes"
      return 1
    fi
    
    log_success "Docker volumes removed"
  else
    log_info "Operation cancelled"
  fi
  
  return 0
}

# Check if database is running and accessible
check_database() {
  local host="${1:-localhost}"
  local port="${2:-$DB_PORT}"
  local max_attempts="${3:-5}"
  local wait_time="${4:-2}"
  
  log_info "Checking database connection on $host:$port"
  
  for i in $(seq 1 $max_attempts); do
    if nc -z "$host" "$port" &> /dev/null; then
      log_success "Database is accessible"
      return 0
    fi
    
    log_warning "Attempt $i/$max_attempts: Database not accessible, waiting ${wait_time}s..."
    sleep "$wait_time"
  done
  
  log_error "Database is not accessible after $max_attempts attempts"
  return 1
}

# Check if all services are running
check_services() {
  local compose_file="${1:-$COMPOSE_FILE}"
  
  log_debug "Checking service status"
  
  if ! $DOCKER_COMPOSE -f "$compose_file" ps --services --filter "status=running" | grep -q .; then
    log_warning "No services are running"
    return 1
  fi
  
  log_debug "Services are running"
  return 0
}

# Initialize and check Docker environment
init_docker_env() {
  if ! check_docker; then
    return 1
  fi
  
  if ! check_docker_compose; then
    return 1
  fi
  
  if ! create_docker_network; then
    return 1
  fi
  
  return 0
} 