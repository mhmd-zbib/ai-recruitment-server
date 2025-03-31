#!/bin/bash

# Helper script to run various commands for the HireSync application

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Get script directory for reliable sourcing
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Application Helper${NC}"
echo -e "${BLUE}========================================${NC}"

# Show the menu if no arguments are given
if [ $# -eq 0 ]; then
  echo -e "${YELLOW}Available commands:${NC}"
  echo -e "  ${GREEN}local${NC}    - Start application in local development mode"
  echo -e "  ${GREEN}dev${NC}      - Start application in development mode"
  echo -e "  ${GREEN}deploy${NC}   - Deploy application in production mode"
  echo -e "  ${GREEN}verify${NC}   - Run code verification checks"
  echo -e "  ${GREEN}docker${NC}   - Build Docker image"
  echo -e "  ${GREEN}help${NC}     - Show this help message"
  echo
  echo -e "${YELLOW}Examples:${NC}"
  echo -e "  ./run.sh local        # Start in local mode"
  echo -e "  ./run.sh dev --clean  # Start in dev mode with clean build"
  echo -e "  ./run.sh deploy --docker  # Deploy with Docker"
  echo -e "  ./run.sh docker --version=1.0.0  # Build Docker image with version tag"
  exit 0
fi

# Process command
COMMAND=$1
shift # Remove the first argument, leaving the rest for the script

case $COMMAND in
  local)
    echo -e "${PURPLE}Starting in local development mode...${NC}"
    $SCRIPT_DIR/scripts/local-start.sh "$@"
    ;;
  dev)
    echo -e "${PURPLE}Starting in development mode...${NC}"
    $SCRIPT_DIR/scripts/dev-start.sh "$@"
    ;;
  deploy)
    echo -e "${PURPLE}Deploying in production mode...${NC}"
    $SCRIPT_DIR/scripts/prod-deploy.sh "$@"
    ;;
  verify)
    echo -e "${PURPLE}Running code verification...${NC}"
    $SCRIPT_DIR/scripts/verify.sh "$@"
    ;;
  docker)
    echo -e "${PURPLE}Building Docker image...${NC}"
    $SCRIPT_DIR/scripts/docker-build.sh "$@"
    ;;
  help)
    echo -e "${YELLOW}Available commands:${NC}"
    echo -e "  ${GREEN}local${NC}    - Start application in local development mode"
    echo -e "  ${GREEN}dev${NC}      - Start application in development mode"
    echo -e "  ${GREEN}deploy${NC}   - Deploy application in production mode"
    echo -e "  ${GREEN}verify${NC}   - Run code verification checks"
    echo -e "  ${GREEN}docker${NC}   - Build Docker image"
    echo -e "  ${GREEN}help${NC}     - Show this help message"
    echo
    echo -e "${YELLOW}Examples:${NC}"
    echo -e "  ./run.sh local        # Start in local mode"
    echo -e "  ./run.sh dev --clean  # Start in dev mode with clean build"
    echo -e "  ./run.sh deploy --docker  # Deploy with Docker"
    echo -e "  ./run.sh docker --version=1.0.0  # Build Docker image with version tag"
    ;;
  *)
    echo -e "${RED}Unknown command: ${COMMAND}${NC}"
    echo -e "${YELLOW}Run ./run.sh help for available commands${NC}"
    exit 1
    ;;
esac

exit $? 