#!/bin/bash
# HireSync Core Utilities

# Set project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Color definitions
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Define docker compose command once
DOCKER_COMPOSE_CMD=""

# Error handling wrapper function
run_command() {
  local cmd="$1"
  local error_msg="${2:-Command failed}"
  
  if ! eval "$cmd"; then
    log_error "$error_msg"
    return 1
  fi
  return 0
}

# Simplified logging
log() { echo -e "[$(date "+%H:%M:%S")] $1"; }
log_info() { log "${CYAN}$1${NC}"; }
log_warning() { log "${YELLOW}$1${NC}"; }
log_error() { log "${RED}$1${NC}"; }
log_success() { log "${GREEN}$1${NC}"; }
log_header() { log "${BOLD}${BLUE}$1${NC}"; }

# Load environment variables
load_env() {
  local env_file="$PROJECT_ROOT/.env"
  local env_example="$PROJECT_ROOT/.env.example"
  
  # Create .env from example if needed
  if [[ ! -f "$env_file" && -f "$env_example" ]]; then
    if ! run_command "cp \"$env_example\" \"$env_file\"" "Failed to create .env from example"; then
      log_error "Please create .env file manually"
      exit 1
    fi
    log_warning "Created default .env file from example. Please review it."
  fi
  
  # Check for and remove BOM character if present
  if [[ -f "$env_file" ]] && head -c 3 "$env_file" | grep -q $'\xEF\xBB\xBF'; then
    log_warning "BOM character detected in .env file, removing it"
    # Create temporary file without BOM
    local temp_file=$(mktemp)
    tail -c +4 "$env_file" > "$temp_file"
    # Replace original file
    mv "$temp_file" "$env_file"
    log_info "BOM character removed from .env file"
  fi
  
  # Load variables from .env file if it exists
  if [[ -f "$env_file" ]]; then
    source "$env_file" || {
      log_error "Failed to load .env file"
      exit 1
    }
  else
    log_error "No .env file found. Please create one."
    exit 1
  fi
  
  # Set essential defaults if not already set
  export DB_HOST=${DB_HOST:-localhost}
  export DB_PORT=${DB_PORT:-5433}
  export DB_NAME=${DB_NAME:-hiresync}
  export DB_USER=${DB_USER:-hiresync}
  export DB_PASSWORD=${DB_PASSWORD:-hiresync}
  export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-local}
  export NETWORK_NAME=${NETWORK_NAME:-hiresync-network}
  export POSTGRES_VOLUME=${POSTGRES_VOLUME:-hiresync-postgres-data}
  export MAVEN_REPO_VOLUME=${MAVEN_REPO_VOLUME:-hiresync-maven-repo}
  
  # Generate JWT secret if needed
  if [ -z "$JWT_SECRET" ]; then
    local new_jwt_secret=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | xxd -p)
    if [ -z "$new_jwt_secret" ]; then
      new_jwt_secret="REPLACE_WITH_SECURE_KEY_IN_PRODUCTION"
    else
      new_jwt_secret="dev_only_${new_jwt_secret}"
    fi
    export JWT_SECRET="$new_jwt_secret"
    
    # Update the .env file with the new JWT secret
    if [ -f "$env_file" ]; then
      if grep -q "^JWT_SECRET=" "$env_file"; then
        sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=$new_jwt_secret|" "$env_file" && rm -f "${env_file}.bak"
      else
        echo "JWT_SECRET=$new_jwt_secret" >> "$env_file"
      fi
    fi
  fi
  
  # Set JWT expiration if not already set
  export JWT_EXPIRATION=${JWT_EXPIRATION:-86400000}
}

# Set up docker compose command
setup_docker_compose_cmd() {
  if [ -z "$DOCKER_COMPOSE_CMD" ]; then
    if docker compose version &> /dev/null; then
      DOCKER_COMPOSE_CMD="docker compose"
    elif docker-compose version &> /dev/null; then
      DOCKER_COMPOSE_CMD="docker-compose"
    else
      log_error "Neither 'docker compose' nor 'docker-compose' command found"
      exit 1
    fi
  fi
  
  return 0
}

# Check if Docker is running
check_docker() {
  log_info "Checking Docker..."
  if ! docker info &> /dev/null; then
    log_error "Docker is not running. Please start Docker first."
    return 1
  fi
  
  setup_docker_compose_cmd
  
  log_success "Docker is running"
  return 0
}

# Print usage information
usage() {
  echo -e "${BOLD}HireSync Development Environment Manager${NC}"
  echo
  echo -e "Usage: ${CYAN}./hiresync${NC} ${BOLD}COMMAND${NC}"
  echo
  echo -e "Commands:"
  echo -e "  ${BOLD}start${NC}        Start all services and the application (production mode)"
  echo -e "  ${BOLD}start-local${NC}  Start local development environment with hot-reloading"
  echo -e "  ${BOLD}stop${NC}         Stop all services"
  echo -e "  ${BOLD}restart${NC}      Restart all services and the application"
  echo -e "  ${BOLD}status${NC}       Show status of services"
  echo -e "  ${BOLD}app${NC}          Start only the Spring Boot application"
  echo -e "  ${BOLD}services${NC}     Start only the supporting services (PostgreSQL, etc.)"
  echo -e "  ${BOLD}clean${NC}        Stop services and remove volumes (data reset)"
  echo -e "  ${BOLD}checkstyle${NC}   Run code style checks (use -h for options)"
  echo -e "  ${BOLD}lint${NC}         Run comprehensive code quality and linting checks"
  echo -e "  ${BOLD}quality${NC}      Run all quality checks (checkstyle, PMD, SpotBugs)"
  echo -e "  ${BOLD}help${NC}         Show this help message"
  echo
}

# Initialize core utilities
setup_docker_compose_cmd
load_env 