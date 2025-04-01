#!/bin/bash
#==============================================================================
# HireSync Application Manager
# Version: 1.0.0
#
# Description:
#   A unified interface for all operations related to the HireSync application.
#   Manages build, deployment, local development, testing, and utilities.
#
# Author: HireSync Team
# License: Proprietary
#==============================================================================

# Exit on any error, undefined variable, and prevent accidental pipe errors
set -euo pipefail

# Get script directory for reliable sourcing
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "$SCRIPT_DIR" && pwd)"

#==============================================================================
# Color definitions
#==============================================================================
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[0;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

#==============================================================================
# Print header
#==============================================================================
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Application Manager${NC}"
echo -e "${BLUE}========================================${NC}"

#==============================================================================
# Utility functions
#==============================================================================

# Verify Docker is installed and running
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

# Locate and verify Maven installation
check_maven() {
  # Check in sequence: project wrapper, local wrapper, system Maven
  if [ -f "$PROJECT_ROOT/mvnw" ]; then
    chmod +x "$PROJECT_ROOT/mvnw" 2>/dev/null || true
    echo "$PROJECT_ROOT/mvnw"
    return 0
  elif [ -f "./mvnw" ]; then
    chmod +x "./mvnw" 2>/dev/null || true
    echo "./mvnw"
    return 0
  elif command -v mvn >/dev/null 2>&1; then
    echo "mvn"
    return 0
  else
    echo -e "${RED}Error: Neither Maven wrapper nor 'mvn' command is available.${NC}"
    echo -e "${RED}Please install Maven or generate a Maven wrapper using:${NC}"
    echo -e "${YELLOW}  mvn -N io.takari:maven:wrapper${NC}"
    return 1
  fi
}

# Check if PostgreSQL container is running
is_postgres_running() {
  docker ps --format '{{.Names}}' | grep -q 'hiresync-postgres'
}

# Load environment variables from .env file
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

#==============================================================================
# Command handlers
#==============================================================================

# Handle build command
handle_build() {
  shift
  load_env
  
  if [[ "$*" == *"--docker"* ]] && ! check_docker; then
    echo -e "${YELLOW}Warning: Docker is required for Docker builds.${NC}"
    echo -e "${YELLOW}Proceeding with standard build...${NC}"
  fi
  
  export DOCKER_DIR="$PROJECT_ROOT/docker"
  bash "$SCRIPT_DIR/scripts/build/docker-build.sh" "$@"
}

# Handle clean command - removes build artifacts
handle_clean() {
  shift
  echo -e "${GREEN}Cleaning build artifacts...${NC}"
  
  local mvn_cmd
  mvn_cmd=$(check_maven)
  if [ $? -ne 0 ]; then
    return 1
  fi
  
  $mvn_cmd clean "$@"
  
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

# Handle deploy command
handle_deploy() {
  shift
  load_env
  
  if [[ "$*" == *"--docker"* ]] && ! check_docker; then
    echo -e "${RED}Error: Docker is required for Docker deployments.${NC}"
    exit 1
  fi
  
  export DOCKER_DIR="$PROJECT_ROOT/docker"
  bash "$SCRIPT_DIR/scripts/deploy/prod-deploy.sh" "$@"
}

# Handle development environment command
handle_dev() {
  shift
  load_env
  export DOCKER_DIR="$PROJECT_ROOT/docker"
  
  if is_postgres_running; then
    echo -e "${GREEN}Detected PostgreSQL container is already running.${NC}"
    echo -e "${GREEN}Auto-adding --use-existing-db flag.${NC}"
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode dev --use-existing-db "$@"
  else
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode dev "$@"
  fi
}

# Handle local development command
handle_local() {
  shift
  load_env
  
  if ! check_docker; then
    echo -e "${RED}Error: Docker is required for local development.${NC}"
    exit 1
  fi
  
  export DOCKER_DIR="$PROJECT_ROOT/docker"
  
  if is_postgres_running; then
    echo -e "${GREEN}Detected PostgreSQL container is already running.${NC}"
    echo -e "${GREEN}Auto-adding --use-existing-db flag.${NC}"
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode local --use-existing-db "$@"
  else
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode local "$@"
  fi
}

# Handle quality check command
handle_quality() {
  shift
  bash "$SCRIPT_DIR/scripts/quality/quality-check.sh" "$@"
}

# Handle linting command - autofix code style issues
handle_lint() {
  shift
  echo -e "${GREEN}Running auto-fix linting...${NC}"
  
  local mvn_cmd
  mvn_cmd=$(check_maven)
  if [ $? -ne 0 ]; then
    return 1
  fi
  
  # Always apply auto-formatting first
  $mvn_cmd spotless:apply -q
  
  # Run the lint-minimal script which now auto-fixes issues
  bash "$SCRIPT_DIR/scripts/quality/quality-check.sh" --quick --auto-fix "$@"
  
  echo -e "${GREEN}Linting and auto-fixing completed!${NC}"
}

# Handle verification command
handle_verify() {
  shift
  bash "$SCRIPT_DIR/scripts/build/verify.sh" "$@"
}

# Handle test command
handle_test() {
  shift
  echo -e "${GREEN}Running tests with test profile...${NC}"
  
  local mvn_cmd
  mvn_cmd=$(check_maven)
  if [ $? -ne 0 ]; then
    return 1
  fi
  
  $mvn_cmd test -Dspring.profiles.active=test "$@"
}

# Handle test environment command
handle_test_env() {
  shift
  load_env
  export DOCKER_DIR="$PROJECT_ROOT/docker"
  
  # Check if PostgreSQL is needed and already running for test env
  if is_postgres_running && [[ ! "$*" == *"--no-docker"* ]]; then
    echo -e "${GREEN}Detected PostgreSQL container is already running.${NC}"
    echo -e "${GREEN}Auto-adding --use-existing-db flag.${NC}"
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode test --use-existing-db "$@"
  else
    bash "$SCRIPT_DIR/scripts/dev/dev-environment.sh" --mode test "$@"
  fi
}

# Handle health check command
handle_health() {
  shift
  echo -e "${GREEN}Running health check...${NC}"
  bash "$SCRIPT_DIR/scripts/utils/health-check.sh" "$@"
}

# Handle git hooks installation
handle_githooks() {
  shift
  echo -e "${GREEN}Installing Git hooks...${NC}"
  
  if [ -d "$SCRIPT_DIR/.git-hooks" ]; then
    if [[ "$*" == *"--windows"* ]]; then
      # Use cmd.exe to run the Windows batch script on Windows
      cmd.exe /c "$SCRIPT_DIR/.git-hooks/auto-install.bat"
    else
      # Use bash to run the shell script
      bash "$SCRIPT_DIR/.git-hooks/auto-install.sh"
    fi
    
    if [ $? -eq 0 ]; then
      echo -e "${GREEN}Git hooks installed successfully.${NC}"
    else
      echo -e "${RED}Error: Failed to install Git hooks.${NC}"
      exit 1
    fi
  else
    echo -e "${RED}Error: Git hooks directory not found.${NC}"
    echo -e "${YELLOW}Expected at: $SCRIPT_DIR/.git-hooks${NC}"
    exit 1
  fi
}

# Handle database management command
handle_db() {
  shift
  case "$1" in
    start)
      shift
      echo -e "${GREEN}Starting PostgreSQL database...${NC}"
      export DOCKER_DIR="$PROJECT_ROOT/docker"
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

# Display help information
show_help() {
  cat << EOF
Usage: ./run.sh [command] [options]

Commands:
  build     Build the application
  clean     Clean build artifacts
  db        Manage the database container (start|stop|restart|status)
  deploy    Deploy the application
  dev       Start development environment
  local     Start local development
  quality   Run quality checks
  lint      Run auto-fix linting (corrects issues automatically)
  verify    Verify code and build
  test      Run tests with test profile
  test-env  Start application with test environment
  health    Check application health status
  githooks  Install Git hooks for development workflow

Options:
  --help                 Show this help message
  --verbose              Enable verbose output
  --debug                Enable debug mode
  --docker               Use Docker for the operation
  --no-docker            Skip using Docker (for dev/local/test-env)
  --use-existing-db      Use existing PostgreSQL database
  --skip-db-wait         Skip waiting for PostgreSQL to be ready
  --version=X            Specify version for builds
  --windows              Use Windows-specific scripts (for githooks)

Examples:
  ./run.sh build --version=1.0.0
  ./run.sh deploy --docker
  ./run.sh dev
  ./run.sh local
  ./run.sh db start
  ./run.sh quality
  ./run.sh lint
  ./run.sh verify
  ./run.sh githooks
EOF
}

#==============================================================================
# Main command handler
#==============================================================================
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
  githooks) handle_githooks "$@" ;;
  --help|-h|help) show_help ;;
  *)
    echo -e "${RED}Error: Unknown command '$1'${NC}"
    echo -e "Run './run.sh --help' for usage information"
    exit 1
    ;;
esac

exit $? 