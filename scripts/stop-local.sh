#!/bin/bash
# HireSync Local Development Environment Stopper
# This script stops the local development environment

set -e

# Set script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOG_DIR="$PROJECT_ROOT/logs"
LOG_FILE="$LOG_DIR/hiresync-stop-$(date +%Y%m%d-%H%M%S).log"
START_TIME=$(date +%s)

# Create logs directory if it doesn't exist
mkdir -p "$LOG_DIR"

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Logging functions
log() {
  local level=$1
  local message=$2
  local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
  local elapsed=$(( $(date +%s) - START_TIME ))
  echo -e "[$timestamp] [$level] [${elapsed}s] $message" | tee -a "$LOG_FILE"
}

log_info() {
  log "INFO" "${CYAN}$1${NC}"
}

log_warning() {
  log "WARNING" "${YELLOW}$1${NC}"
}

log_error() {
  log "ERROR" "${RED}$1${NC}"
}

log_success() {
  log "SUCCESS" "${GREEN}$1${NC}"
}

log_step() {
  log "STEP" "${BOLD}${BLUE}$1${NC}"
}

log_debug() {
  if [[ "${DEBUG:-false}" == "true" ]]; then
    log "DEBUG" "$1"
  fi
}

# Error handler
handle_error() {
  local exit_code=$?
  log_error "An error occurred (exit code: $exit_code)"
  log_error "Check the log file for details: $LOG_FILE"
  exit $exit_code
}

# Set up trap for error handling
trap handle_error ERR

# Load environment variables
load_env_variables() {
  log_step "LOADING ENVIRONMENT VARIABLES"
  
  local env_file="$PROJECT_ROOT/.env"
  if [ -f "$env_file" ]; then
    log_info "Found .env file at: $env_file"
    set -a
    source "$env_file"
    set +a
    log_success "Environment variables loaded successfully"
    
    # Set default values for any missing variables
    export NETWORK_NAME=${NETWORK_NAME:-hiresync-network}
    export POSTGRES_VOLUME=${POSTGRES_VOLUME:-hiresync-postgres-data}
    export MAVEN_REPO_VOLUME=${MAVEN_REPO_VOLUME:-hiresync-maven-repo}
  else
    log_warning "No .env file found at: $env_file"
    log_info "Using default values"
    
    # Set default values
    export NETWORK_NAME="hiresync-network"
    export POSTGRES_VOLUME="hiresync-postgres-data"
    export MAVEN_REPO_VOLUME="hiresync-maven-repo"
  fi
}

# Check if Docker is running
check_docker() {
  log_step "CHECKING DOCKER STATUS"
  
  if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed or not in PATH"
    exit 1
  fi
  
  log_info "Testing Docker connection..."
  if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running. Cannot stop containers."
    exit 1
  fi
  
  log_success "Docker is running and accessible"
}

# Stop Docker containers
stop_docker_containers() {
  log_step "STOPPING DOCKER CONTAINERS"
  
  local compose_file="$PROJECT_ROOT/docker/docker-compose.local.yaml"
  if [ -f "$compose_file" ]; then
    log_info "Using docker-compose file: $compose_file"
    
    # Check if there are any running hiresync containers
    if docker ps -q --filter "name=hiresync" | grep -q .; then
      log_info "Stopping containers with docker-compose..."
      
      # Check for temporary Docker environment file
      local docker_env_file="$PROJECT_ROOT/docker/.env.docker"
      if [ -f "$docker_env_file" ]; then
        docker-compose -f "$compose_file" --env-file "$docker_env_file" down
      else
        docker-compose -f "$compose_file" down
      fi
      
      if [ $? -eq 0 ]; then
        log_success "Containers stopped successfully with docker-compose"
      else
        log_warning "Failed to stop containers with docker-compose, trying direct Docker commands..."
        stop_containers_directly
      fi
    else
      log_info "No running hiresync containers found"
    fi
  else
    log_warning "Docker Compose file not found: $compose_file"
    log_info "Trying to stop containers directly with Docker commands..."
    stop_containers_directly
  fi
  
  # Clean up temporary files
  cleanup_temp_files
}

# Stop containers directly with Docker commands if docker-compose fails
stop_containers_directly() {
  log_info "Stopping containers directly with Docker commands..."
  
  # Get all containers that contain "hiresync" in their name
  local containers=$(docker ps -a -q --filter "name=hiresync")
  
  if [ -n "$containers" ]; then
    log_info "Found hiresync containers. Stopping them..."
    
    for container in $containers; do
      local container_name=$(docker inspect --format='{{.Name}}' "$container" | sed 's/^\///')
      
      # Check if container is running
      if docker ps -q --filter "id=$container" | grep -q .; then
        log_info "Stopping container: $container_name"
        docker stop "$container" > /dev/null
      else
        log_info "Container already stopped: $container_name"
      fi
      
      # Remove container
      log_info "Removing container: $container_name"
      docker rm "$container" > /dev/null
    done
    
    log_success "Containers stopped and removed successfully"
  else
    log_info "No hiresync containers found"
  fi
}

# Clean up temporary files created during development
cleanup_temp_files() {
  log_step "CLEANING UP TEMPORARY FILES"
  
  # Remove generated application-env.yaml
  local env_yaml="$PROJECT_ROOT/src/main/resources/application-env.yaml"
  if [ -f "$env_yaml" ]; then
    log_info "Removing generated application-env.yaml..."
    rm -f "$env_yaml"
  fi
  
  # Remove temporary Docker environment file
  local docker_env_file="$PROJECT_ROOT/docker/.env.docker"
  if [ -f "$docker_env_file" ]; then
    log_info "Removing temporary Docker environment file..."
    rm -f "$docker_env_file"
  fi
  
  log_success "Temporary files cleaned up successfully"
}

# Setup logging
setup_logging() {
  log_info "============================================================"
  log_info "${BOLD}STOPPING HIRESYNC LOCAL DEVELOPMENT ENVIRONMENT${NC}"
  log_info "============================================================"
  log_info "Date/Time: $(date)"
  log_info "User: $(whoami)"
  log_info "System: $(uname -a)"
  log_info "Project Root: $PROJECT_ROOT"
}

# Main function
main() {
  setup_logging
  
  # Load environment variables
  load_env_variables
  
  # Check Docker
  check_docker
  
  # Stop Docker containers
  stop_docker_containers
  
  log_success "All containers stopped successfully!"
}

# Run the main function
main

# Calculate total execution time - this will only execute if the script completes successfully
END_TIME=$(date +%s)
TOTAL_TIME=$((END_TIME - START_TIME))
log_success "Script execution completed successfully in ${TOTAL_TIME} seconds" 