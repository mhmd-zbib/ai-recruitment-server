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

is_postgres_running() {
  if docker ps --format '{{.Names}}' | grep -q 'hiresync-postgres'; then
    return 0
  else
    return 1
  fi
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
  
  # Add auto detection for existing db
  if is_postgres_running; then
    echo -e "${GREEN}Detected PostgreSQL container is already running.${NC}"
    echo -e "${GREEN}Auto-adding --use-existing-db flag.${NC}"
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode dev --use-existing-db "$@"
  else
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode dev "$@"
  fi
}

handle_local() {
  shift
  load_env
  
  if ! check_docker; then
    echo -e "${RED}Error: Docker is required for local development.${NC}"
    exit 1
  fi
  
  # Pass the Docker directory to the dev environment script
  DOCKER_DIR="$PROJECT_ROOT/docker"
  export DOCKER_DIR
  
  # Auto-detect if PostgreSQL is already running
  if is_postgres_running; then
    echo -e "${GREEN}Detected PostgreSQL container is already running.${NC}"
    echo -e "${GREEN}Auto-adding --use-existing-db flag.${NC}"
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode local --use-existing-db "$@"
  else
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode local "$@"
  fi
}

handle_quality() {
  shift
  bash "$SCRIPT_DIR/scripts/quality/quality-check.sh" "$@"
}

handle_lint() {
  shift
  echo -e "${GREEN}Running auto-fix linting...${NC}"
  
  # Always apply auto-formatting first
  if [ -f ./mvnw ]; then
    ./mvnw spotless:apply -q
  else
    mvn spotless:apply -q
  fi
  
  # Run the lint-minimal script which now auto-fixes issues
  bash "$SCRIPT_DIR/scripts/quality/lint-minimal.sh" "$@"
  
  echo -e "${GREEN}Linting and auto-fixing completed!${NC}"
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

handle_test_env() {
  shift
  load_env
  
  # Pass the Docker directory to the dev environment script
  DOCKER_DIR="$PROJECT_ROOT/docker"
  export DOCKER_DIR
  
  # Check if PostgreSQL is needed and already running for test env
  if is_postgres_running && [[ ! "$*" == *"--no-docker"* ]]; then
    echo -e "${GREEN}Detected PostgreSQL container is already running.${NC}"
    echo -e "${GREEN}Auto-adding --use-existing-db flag.${NC}"
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode test --use-existing-db "$@"
  else
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode test "$@"
  fi
}

handle_health() {
  shift
  echo -e "${GREEN}Running health check...${NC}"
  bash "$SCRIPT_DIR/scripts/utils/health-check.sh" "$@"
}

handle_db() {
  shift
  case "$1" in
    start)
      shift
      echo -e "${GREEN}Starting PostgreSQL database...${NC}"
      DOCKER_DIR="$PROJECT_ROOT/docker"
      export DOCKER_DIR
      source "$SCRIPT_DIR/scripts/utils/db-utils.sh"
      start_postgres
      ;;
    stop)
      shift
      echo -e "${GREEN}Stopping PostgreSQL database...${NC}"
      if check_docker && docker ps --format '{{.Names}}' | grep -q 'hiresync-postgres'; then
        docker stop hiresync-postgres
        echo -e "${GREEN}PostgreSQL database stopped.${NC}"
      else
        echo -e "${YELLOW}No running PostgreSQL container found.${NC}"
      fi
      ;;
    restart)
      shift
      echo -e "${GREEN}Restarting PostgreSQL database...${NC}"
      if check_docker && docker ps -a --format '{{.Names}}' | grep -q 'hiresync-postgres'; then
        docker restart hiresync-postgres
        echo -e "${GREEN}PostgreSQL database restarted.${NC}"
      else
        echo -e "${YELLOW}No PostgreSQL container found to restart.${NC}"
        echo -e "${YELLOW}Use 'run.sh db start' to create and start a new container.${NC}"
      fi
      ;;
    status)
      shift
      if check_docker && docker ps --format '{{.Names}}' | grep -q 'hiresync-postgres'; then
        echo -e "${GREEN}PostgreSQL database is running.${NC}"
        docker ps --filter "name=hiresync-postgres" --format "table {{.ID}}\t{{.Names}}\t{{.Status}}\t{{.Ports}}"
      else
        if docker ps -a --format '{{.Names}}' | grep -q 'hiresync-postgres'; then
          echo -e "${YELLOW}PostgreSQL container exists but is not running.${NC}"
          docker ps -a --filter "name=hiresync-postgres" --format "table {{.ID}}\t{{.Names}}\t{{.Status}}"
          echo -e "${YELLOW}Use 'run.sh db start' to start the container.${NC}"
        else
          echo -e "${RED}No PostgreSQL container found.${NC}"
          echo -e "${YELLOW}Use 'run.sh db start' to create and start a new container.${NC}"
        fi
      fi
      ;;
    *)
      echo -e "${YELLOW}Usage: run.sh db [start|stop|restart|status]${NC}"
      echo -e "${YELLOW}  start    - Start the PostgreSQL database container${NC}"
      echo -e "${YELLOW}  stop     - Stop the PostgreSQL database container${NC}"
      echo -e "${YELLOW}  restart  - Restart the PostgreSQL database container${NC}"
      echo -e "${YELLOW}  status   - Check if the PostgreSQL database is running${NC}"
      ;;
  esac
}

show_help() {
  echo -e "Usage: ./run.sh [command] [options]"
  echo -e ""
  echo -e "Commands:"
  echo -e "  build     Build the application"
  echo -e "  clean     Clean build artifacts"
  echo -e "  db        Manage the database container (start|stop|restart|status)"
  echo -e "  deploy    Deploy the application"
  echo -e "  dev       Start development environment"
  echo -e "  local     Start local development"
  echo -e "  quality   Run quality checks"
  echo -e "  lint      Run auto-fix linting (corrects issues automatically)"
  echo -e "  verify    Verify code and build"
  echo -e "  test      Run tests with test profile"
  echo -e "  test-env  Start application with test environment"
  echo -e "  health    Check application health status"
  echo -e ""
  echo -e "Options:"
  echo -e "  --help                 Show this help message"
  echo -e "  --verbose              Enable verbose output"
  echo -e "  --debug                Enable debug mode"
  echo -e "  --docker               Use Docker for the operation"
  echo -e "  --no-docker            Skip using Docker (for dev/local/test-env)"
  echo -e "  --use-existing-db      Use existing PostgreSQL database"
  echo -e "  --skip-db-wait         Skip waiting for PostgreSQL to be ready"
  echo -e "  --version=X            Specify version for builds"
  echo -e ""
  echo -e "Examples:"
  echo -e "  ./run.sh build --version=1.0.0"
  echo -e "  ./run.sh deploy --docker"
  echo -e "  ./run.sh dev"
  echo -e "  ./run.sh local"
  echo -e "  ./run.sh db start"
  echo -e "  ./run.sh quality"
  echo -e "  ./run.sh lint"
  echo -e "  ./run.sh verify"
}

# Main command handler
case "$1" in
  build)    handle_build "$@" ;;
  clean)    handle_clean "$@" ;;
  db)       handle_db "$@" ;;
  deploy)   handle_deploy "$@" ;;
  dev)      handle_dev "$@" ;;
  local)    handle_local "$@" ;;
  quality)  handle_quality "$@" ;;
  lint)     handle_lint "$@" ;;
  verify)   handle_verify "$@" ;;
  test)     handle_test "$@" ;;
  test-env) handle_test_env "$@" ;;
  health)   handle_health "$@" ;;
  --help|-h|help) show_help ;;
  *)
    echo -e "${RED}Error: Unknown command '$1'${NC}"
    echo -e "Run './run.sh --help' for usage information"
    exit 1
    ;;
esac

exit $? 