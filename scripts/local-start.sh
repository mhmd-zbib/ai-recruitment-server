#!/bin/bash

# Script to start HireSync application in local development mode
# Always uses PostgreSQL in Docker for consistency

# Get script directory for reliable sourcing
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Source the database utility functions
source "$SCRIPT_DIR/db-utils.sh"

# Set colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Local Development Starter${NC}"
echo -e "${BLUE}========================================${NC}"

# Process command-line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --help)
      echo -e "Usage: ./scripts/local-start.sh [options]"
      echo -e "Options:"
      echo -e "  --help                Display this help message"
      echo -e ""
      echo -e "Examples:"
      echo -e "  ./scripts/local-start.sh           # Start with PostgreSQL in Docker"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Load environment variables from .env file
load_env_file

# Change to project root
cd "$PROJECT_ROOT" || exit 1

# Check Docker status
if ! check_docker; then
  echo -e "${RED}Error: Docker is required for local development.${NC}"
  echo -e "${YELLOW}Please start Docker and try again.${NC}"
  exit 1
fi

# Configure database for PostgreSQL
configure_local_db true

# Start PostgreSQL
if ! start_postgres; then
  echo -e "${RED}Failed to start PostgreSQL. Cannot proceed with local development.${NC}"
  exit 1
fi

# Export Spring active profile
export SPRING_PROFILES_ACTIVE=local

# Print application access information
echo -e "${GREEN}Starting application with local profile...${NC}"
echo -e "${YELLOW}API will be available at: http://localhost:8080/api${NC}"
echo -e "${YELLOW}Swagger UI will be available at: http://localhost:8080/api/swagger-ui.html${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop the application${NC}"

# Run the application with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Exit with the status of the last command
exit $? 