#!/bin/bash
# HireSync Docker Utilities
# Provides functions for Docker operations in the project

# Exit on error, treat unset variables as errors, and handle pipefail
set -euo pipefail

# Constants
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Import core utilities if not already available
if [[ -z "${BOLD_BLUE:-}" ]]; then
  source "${SCRIPT_DIR}/core/colors.sh"
fi

if ! type log_info > /dev/null 2>&1; then
  source "${SCRIPT_DIR}/core/logging.sh"
fi

# Check if Docker is installed and running
check_docker() {
  log_debug "Checking Docker installation and daemon"
  
  if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed or not in PATH"
    return 1
  fi
  
  if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running"
    log_info "Please start Docker Desktop or the Docker service"
    return 1
  fi
  
  return 0
}

# Check if Docker Compose is installed
check_docker_compose() {
  log_debug "Checking Docker Compose availability"
  
  if command -v docker-compose &> /dev/null; then
    return 0
  elif docker compose version &> /dev/null; then
    return 0
  else
    log_error "Docker Compose not found (neither 'docker-compose' nor 'docker compose')"
    return 1
  fi
}

# Run docker-compose with the appropriate command based on what's available
docker_compose() {
  if command -v docker-compose &> /dev/null; then
    docker-compose "$@"
  else
    docker compose "$@"
  fi
}

# Build Docker image for the application
docker_build() {
  log_step "Building Docker image"
  
  local tag="${1:-latest}"
  local dockerfile="${2:-${PROJECT_ROOT}/docker/Dockerfile}"
  local build_args=()
  
  # Shift parameters for additional build args
  shift 2 || true
  
  # Add any additional build args
  for arg in "$@"; do
    build_args+=("--build-arg" "$arg")
  done
  
  # Check if Docker is available
  check_docker || return 1
  
  log_info "Building Docker image with tag: ${tag}"
  
  # Navigate to project root to ensure correct context
  cd "${PROJECT_ROOT}"
  
  # Start the build
  if docker build \
    -t "hiresync:${tag}" \
    -f "${dockerfile}" \
    "${build_args[@]}" \
    --no-cache \
    .; then
    log_success "Docker image hiresync:${tag} built successfully"
    return 0
  else
    log_error "Docker image build failed"
    return 1
  fi
}

# Start Docker Compose stack
docker_up() {
  log_step "Starting Docker Compose stack"
  
  local env_file="${1:-${PROJECT_ROOT}/.env}"
  local compose_file="${2:-${PROJECT_ROOT}/docker/docker-compose.yaml}"
  local extra_file="${3:-}"
  local detached="${4:-true}"
  
  # Check Docker and Docker Compose
  check_docker || return 1
  check_docker_compose || return 1
  
  # Prepare command arguments
  local cmd_args=("-f" "${compose_file}")
  
  # Add override file if specified
  if [[ -n "${extra_file}" && -f "${extra_file}" ]]; then
    cmd_args+=("-f" "${extra_file}")
  fi
  
  # Add detached mode flag
  if [[ "${detached}" == "true" ]]; then
    cmd_args+=("-d")
  fi
  
  # Load environment file
  if [[ -f "${env_file}" ]]; then
    log_info "Using environment file: ${env_file}"
    export $(grep -v '^#' "${env_file}" | xargs)
  else
    log_warn "Environment file not found: ${env_file}"
  fi
  
  # Start the containers
  log_info "Starting Docker Compose services"
  cd "${PROJECT_ROOT}"
  
  if docker_compose "${cmd_args[@]}" up; then
    log_success "Docker Compose stack started"
    return 0
  else
    log_error "Failed to start Docker Compose stack"
    return 1
  fi
}

# Stop Docker Compose stack
docker_down() {
  log_step "Stopping Docker Compose stack"
  
  local compose_file="${1:-${PROJECT_ROOT}/docker/docker-compose.yaml}"
  local extra_file="${2:-}"
  local remove_volumes="${3:-false}"
  
  # Check Docker and Docker Compose
  check_docker || return 1
  check_docker_compose || return 1
  
  # Prepare command arguments
  local cmd_args=("-f" "${compose_file}")
  
  # Add override file if specified
  if [[ -n "${extra_file}" && -f "${extra_file}" ]]; then
    cmd_args+=("-f" "${extra_file}")
  fi
  
  # Add volume removal flag if requested
  if [[ "${remove_volumes}" == "true" ]]; then
    cmd_args+=("-v")
  fi
  
  # Stop the containers
  log_info "Stopping Docker Compose services"
  cd "${PROJECT_ROOT}"
  
  if docker_compose "${cmd_args[@]}" down; then
    log_success "Docker Compose stack stopped"
    return 0
  else
    log_error "Failed to stop Docker Compose stack"
    return 1
  fi
}

# Show status of Docker Compose services
docker_status() {
  log_step "Docker Compose service status"
  
  local compose_file="${1:-${PROJECT_ROOT}/docker/docker-compose.yaml}"
  local extra_file="${2:-}"
  
  # Check Docker and Docker Compose
  check_docker || return 1
  check_docker_compose || return 1
  
  # Prepare command arguments
  local cmd_args=("-f" "${compose_file}")
  
  # Add override file if specified
  if [[ -n "${extra_file}" && -f "${extra_file}" ]]; then
    cmd_args+=("-f" "${extra_file}")
  fi
  
  # Get service status
  cd "${PROJECT_ROOT}"
  docker_compose "${cmd_args[@]}" ps
  
  return $?
}

# View logs from Docker Compose services
docker_logs() {
  local service="${1:-}"
  local compose_file="${2:-${PROJECT_ROOT}/docker/docker-compose.yaml}"
  local extra_file="${3:-}"
  local tail="${4:-100}"
  local follow="${5:-false}"
  
  # Check Docker and Docker Compose
  check_docker || return 1
  check_docker_compose || return 1
  
  # Prepare command arguments
  local cmd_args=("-f" "${compose_file}")
  
  # Add override file if specified
  if [[ -n "${extra_file}" && -f "${extra_file}" ]]; then
    cmd_args+=("-f" "${extra_file}")
  fi
  
  # Add log options
  cmd_args+=("logs" "--tail=${tail}")
  
  # Add follow flag if requested
  if [[ "${follow}" == "true" ]]; then
    cmd_args+=("-f")
  fi
  
  # Add service if specified
  if [[ -n "${service}" ]]; then
    cmd_args+=("${service}")
  fi
  
  # View logs
  cd "${PROJECT_ROOT}"
  docker_compose "${cmd_args[@]}"
  
  return $?
}

# Clean up Docker resources
docker_clean() {
  log_step "Cleaning Docker resources"
  
  # Check if Docker is available
  check_docker || return 1
  
  # Stop containers first
  log_info "Stopping containers"
  docker_down "" "" "true" || true
  
  # Remove dangling images
  log_info "Removing dangling images"
  docker image prune -f
  
  # Remove unused networks
  log_info "Removing unused networks"
  docker network prune -f
  
  # Ask if user wants to remove volumes
  if [[ -t 0 ]] && read -p "Do you want to remove all unused volumes? This will DELETE DATA! (y/n): " -n 1 -r && echo && [[ $REPLY =~ ^[Yy]$ ]]; then
    log_info "Removing unused volumes"
    docker volume prune -f
  fi
  
  log_success "Docker resources cleaned successfully"
  return 0
}

# Export functions
export -f check_docker
export -f check_docker_compose
export -f docker_compose
export -f docker_build
export -f docker_up
export -f docker_down
export -f docker_status
export -f docker_logs
export -f docker_clean 