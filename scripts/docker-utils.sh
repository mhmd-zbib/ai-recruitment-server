#!/bin/bash
# HireSync Docker Management Utilities

# Load common utilities
source "$(dirname "$0")/common.sh"

# Set up Docker infrastructure (networks, volumes)
setup_docker_infrastructure() {
  log_header "Setting up Docker infrastructure..."
  
  # Check Docker first
  if ! check_docker; then
    log_error "Cannot setup infrastructure without Docker running"
    return 1
  fi
  
  # Create network if it doesn't exist
  if ! docker network inspect "$NETWORK_NAME" &> /dev/null; then
    log_info "Creating Docker network '$NETWORK_NAME'..."
    if ! run_command "docker network create --driver bridge \"$NETWORK_NAME\"" "Failed to create Docker network"; then
      return 1
    fi
  fi
  
  # Create PostgreSQL volume if it doesn't exist
  if ! docker volume inspect "$POSTGRES_VOLUME" &> /dev/null; then
    log_info "Creating Docker volume '$POSTGRES_VOLUME'..."
    if ! run_command "docker volume create \"$POSTGRES_VOLUME\"" "Failed to create PostgreSQL volume"; then
      return 1
    fi
  fi
  
  # Create Maven repository volume if it doesn't exist
  if ! docker volume inspect "$MAVEN_REPO_VOLUME" &> /dev/null; then
    log_info "Creating Docker volume '$MAVEN_REPO_VOLUME'..."
    if ! run_command "docker volume create \"$MAVEN_REPO_VOLUME\"" "Failed to create Maven repository volume"; then
      return 1
    fi
  fi
  
  log_success "Docker infrastructure ready"
  return 0
}

# Start services using docker-compose
start_services() {
  log_header "Starting services..."
  
  # Check Docker first
  if ! check_docker; then
    log_error "Cannot start services without Docker running"
    return 1
  fi
  
  local compose_file="$PROJECT_ROOT/docker/docker-compose.local.yaml"
  
  if [ ! -f "$compose_file" ]; then
    log_error "Docker Compose file not found: $compose_file"
    return 1
  fi
  
  # Start the services
  if ! run_command "$DOCKER_COMPOSE_CMD -f \"$compose_file\" up -d" "Failed to start services"; then
    return 1
  fi
  
  log_success "Services started successfully"
  return 0
}

# Stop services
stop_services() {
  log_header "Stopping services..."
  
  # Check Docker first
  if ! check_docker; then
    log_warning "Docker is not running, cannot stop services properly"
    return 1
  fi
  
  local compose_file="$PROJECT_ROOT/docker/docker-compose.local.yaml"
  
  if [ ! -f "$compose_file" ]; then
    log_error "Docker Compose file not found: $compose_file"
    return 1
  fi
  
  # Stop the services
  if ! run_command "$DOCKER_COMPOSE_CMD -f \"$compose_file\" down" "Failed to stop services"; then
    log_warning "Some services may still be running"
    return 1
  fi
  
  log_success "Services stopped successfully"
  return 0
}

# Show status of services
show_status() {
  echo -e "${BOLD}HireSync Services Status${NC}"
  echo
  
  # Check Docker
  echo -e "${BOLD}Docker:${NC}"
  if docker info &> /dev/null; then
    echo -e "  Status: ${GREEN}Running${NC}"
  else
    echo -e "  Status: ${RED}Not Running${NC}"
    return 1
  fi
  
  # Check PostgreSQL
  echo -e "${BOLD}PostgreSQL:${NC}"
  if docker ps --format '{{.Names}}' | grep -q "hiresync-postgres"; then
    echo -e "  Status: ${GREEN}Running${NC}"
    echo -e "  Port: ${CYAN}$DB_PORT${NC}"
    echo -e "  Database: ${CYAN}$DB_NAME${NC}"
  else
    echo -e "  Status: ${RED}Not Running${NC}"
  fi
  
  # Check DevTools
  echo -e "${BOLD}DevTools:${NC}"
  if docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
    echo -e "  Status: ${GREEN}Running${NC}"
  else
    echo -e "  Status: ${RED}Not Running${NC}"
  fi
  
  echo
  return 0
}

# Clean up environment
clean_environment() {
  log_header "Cleaning environment..."
  
  # Stop services first
  stop_services
  
  # Ask for confirmation
  read -p "Are you sure you want to delete all data (volumes)? [y/N] " -n 1 -r
  echo
  
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Remove volumes
    log_info "Removing Docker volumes..."
    
    if docker volume inspect "$POSTGRES_VOLUME" &> /dev/null; then
      if ! run_command "docker volume rm \"$POSTGRES_VOLUME\"" "Failed to remove PostgreSQL volume"; then
        log_warning "PostgreSQL volume may still be in use by a container"
      else
        log_info "Removed volume '$POSTGRES_VOLUME'"
      fi
    fi
    
    if docker volume inspect "$MAVEN_REPO_VOLUME" &> /dev/null; then
      if ! run_command "docker volume rm \"$MAVEN_REPO_VOLUME\"" "Failed to remove Maven repository volume"; then
        log_warning "Maven repository volume may still be in use by a container"
      else
        log_info "Removed volume '$MAVEN_REPO_VOLUME'"
      fi
    fi
    
    log_success "Environment cleaned successfully"
  else
    log_warning "Clean operation cancelled"
  fi
  
  return 0
} 