#!/bin/bash

# HireSync Application Manager
# A unified interface for all operations related to the HireSync application

# Get script directory for reliable sourcing
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR" && pwd)"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Application Manager${NC}"
echo -e "${BLUE}========================================${NC}"

# Utility functions
check_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not installed.${NC}"
    return 1
  fi

  if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running.${NC}"
    return 1
  fi

  return 0
}

load_env() {
  if [ -f .env ]; then
    echo -e "${GREEN}Loading environment variables from .env file...${NC}"
    set -a
    source ./.env
    set +a
  else
    echo -e "${YELLOW}Warning: .env file not found, using default values.${NC}"
    echo -e "${YELLOW}Consider creating an .env file from .env.example${NC}"
  fi
}

# Command handlers
handle_build() {
  shift
  load_env
  
  if [[ "$*" == *"--docker"* ]] && ! check_docker; then
    echo -e "${YELLOW}Warning: Docker is required for Docker builds.${NC}"
    echo -e "${YELLOW}Proceeding with standard build...${NC}"
  fi
  
  # Pass the Docker directory to the build script
  DOCKER_DIR="$PROJECT_ROOT/docker"
  export DOCKER_DIR
  bash "$SCRIPT_DIR/scripts/build/docker-build.sh" "$@"
}

handle_clean() {
  shift
  echo -e "${GREEN}Cleaning build artifacts...${NC}"
  ./mvnw clean "$@"
  
  if [[ "$*" == *"--docker"* ]]; then
    if check_docker; then
      echo -e "${GREEN}Cleaning Docker artifacts...${NC}"
      docker system prune -f
    else
      echo -e "${YELLOW}Warning: Docker is not available, skipping Docker cleanup.${NC}"
    fi
  fi
  
  if [ -d "$SCRIPT_DIR/logs" ]; then
    echo -e "${GREEN}Cleaning logs directory...${NC}"
    rm -rf "$SCRIPT_DIR/logs"/*
  fi
  
  echo -e "${GREEN}Clean completed successfully.${NC}"
}

handle_deploy() {
  shift
  load_env
  
  if [[ "$*" == *"--docker"* ]] && ! check_docker; then
    echo -e "${RED}Error: Docker is required for Docker deployments.${NC}"
    exit 1
  fi
  
  # Pass the Docker directory to the deploy script
  DOCKER_DIR="$PROJECT_ROOT/docker"
  export DOCKER_DIR
  bash "$SCRIPT_DIR/scripts/deploy/prod-deploy.sh" "$@"
}

handle_dev() {
  shift
  load_env
  
  # Pass the Docker directory to the dev script
  DOCKER_DIR="$PROJECT_ROOT/docker"
  export DOCKER_DIR
  bash "$SCRIPT_DIR/scripts/dev/dev-start.sh" "$@"
}

handle_local() {
  shift
  load_env
  
  if ! check_docker; then
    echo -e "${RED}Error: Docker is required for local development.${NC}"
    exit 1
  fi
  
  # Pass the Docker directory to the local script
  DOCKER_DIR="$PROJECT_ROOT/docker"
  export DOCKER_DIR
  bash "$SCRIPT_DIR/scripts/dev/local-start.sh" "$@"
}

handle_quality() {
  shift
  bash "$SCRIPT_DIR/scripts/quality/quality-check.sh" "$@"
}

handle_verify() {
  shift
  bash "$SCRIPT_DIR/scripts/build/verify.sh" "$@"
}

handle_test() {
  shift
  echo -e "${GREEN}Running tests with test profile...${NC}"
  ./mvnw test -Dspring.profiles.active=test "$@"
}

handle_health() {
  shift
  echo -e "${GREEN}Running health check...${NC}"
  bash "$SCRIPT_DIR/scripts/utils/health-check.sh" "$@"
}

show_help() {
  echo -e "Usage: ./run.sh [command] [options]"
  echo -e ""
  echo -e "Commands:"
  echo -e "  build     Build the application"
  echo -e "  clean     Clean build artifacts"
  echo -e "  deploy    Deploy the application"
  echo -e "  dev       Start development environment"
  echo -e "  local     Start local development"
  echo -e "  quality   Run quality checks"
  echo -e "  verify    Verify code and build"
  echo -e "  test      Run tests with test profile"
  echo -e "  health    Check application health status"
  echo -e ""
  echo -e "Options:"
  echo -e "  --help       Show this help message"
  echo -e "  --verbose    Enable verbose output"
  echo -e "  --debug      Enable debug mode"
  echo -e "  --docker     Use Docker for the operation"
  echo -e "  --version=X  Specify version for builds"
  echo -e ""
  echo -e "Examples:"
  echo -e "  ./run.sh build --version=1.0.0"
  echo -e "  ./run.sh deploy --docker"
  echo -e "  ./run.sh dev"
  echo -e "  ./run.sh local"
  echo -e "  ./run.sh quality"
  echo -e "  ./run.sh verify"
}

# Main command handler
case "$1" in
  build)    handle_build "$@" ;;
  clean)    handle_clean "$@" ;;
  deploy)   handle_deploy "$@" ;;
  dev)      handle_dev "$@" ;;
  local)    handle_local "$@" ;;
  quality)  handle_quality "$@" ;;
  verify)   handle_verify "$@" ;;
  test)     handle_test "$@" ;;
  health)   handle_health "$@" ;;
  --help|-h|help) show_help ;;
  *)
    echo -e "${RED}Error: Unknown command '$1'${NC}"
    echo -e "Run './run.sh --help' for usage information"
    exit 1
    ;;
esac

exit $? 