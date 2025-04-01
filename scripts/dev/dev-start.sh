#!/bin/bash

# Script to start HireSync application in development mode
# This assumes Docker is running and will start PostgreSQL

# Get script directory for reliable sourcing
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Source the database utility functions
source "$SCRIPT_DIR/db-utils.sh"

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Development Environment Starter${NC}"
echo -e "${BLUE}========================================${NC}"

# Process command-line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --clean)
      CLEAN_BUILD=true
      shift
      ;;
    --help)
      echo -e "Usage: ./scripts/dev-start.sh [options]"
      echo -e "Options:"
      echo -e "  --clean               Perform a clean Maven build before starting"
      echo -e "  --help                Display this help message"
      echo -e ""
      echo -e "Examples:"
      echo -e "  ./scripts/dev-start.sh           # Start in development mode with PostgreSQL"
      echo -e "  ./scripts/dev-start.sh --clean   # Clean build and start in development mode"
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

# Check Docker status and exit if not running
if ! check_docker; then
  echo -e "${RED}Error: Docker is required for development environment.${NC}"
  echo -e "${YELLOW}Please start Docker and try again.${NC}"
  exit 1
fi

# Check if Postgres container is already running
if docker ps | grep -q "hiresync-postgres"; then
  echo -e "${GREEN}PostgreSQL container is already running.${NC}"
else
  # Start PostgreSQL if Docker is available
  if ! start_postgres; then
    echo -e "${RED}Failed to start PostgreSQL database. Cannot proceed with development environment.${NC}"
    exit 1
  fi
fi

# Export Spring active profile
export SPRING_PROFILES_ACTIVE=dev

# Print application access information
echo -e "${GREEN}Starting application with development profile...${NC}"
echo -e "${YELLOW}API will be available at: http://localhost:8080/api${NC}"
echo -e "${YELLOW}Swagger UI will be available at: http://localhost:8080/api/swagger-ui.html${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop the application${NC}"

# Run the application with Maven
if [ "$CLEAN_BUILD" = true ]; then
  echo -e "${YELLOW}Performing clean Maven build...${NC}"
  ./mvnw clean spring-boot:run -Dspring-boot.run.profiles=dev
else
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
fi

# Exit with the status of the last command
exit $? 