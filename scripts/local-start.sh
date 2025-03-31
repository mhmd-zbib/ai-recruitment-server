#!/bin/bash

# Script to start HireSync application in local development mode
# Automatically detects if Docker is running and configures accordingly

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
NC='\033[0m' # No Color

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Local Development Starter${NC}"
echo -e "${BLUE}========================================${NC}"

# Process command-line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --h2)
      FORCE_H2=true
      shift
      ;;
    --help)
      echo -e "Usage: ./scripts/local-start.sh [options]"
      echo -e "Options:"
      echo -e "  --h2                  Force using H2 in-memory database, even if Docker is available"
      echo -e "  --help                Display this help message"
      echo -e ""
      echo -e "Examples:"
      echo -e "  ./scripts/local-start.sh           # Auto-detect: use PostgreSQL if Docker is running, otherwise H2"
      echo -e "  ./scripts/local-start.sh --h2      # Force using H2 database regardless of Docker status"
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
docker_available=false
if check_docker; then
  docker_available=true
fi

# Configure database based on Docker availability and user preference
if [ "$FORCE_H2" = true ]; then
  echo -e "${YELLOW}Forcing use of H2 in-memory database as requested.${NC}"
  configure_local_db false
elif [ "$docker_available" = true ]; then
  configure_local_db true
  
  # Start PostgreSQL if Docker is available
  if ! start_postgres; then
    echo -e "${RED}Failed to start PostgreSQL. Falling back to H2 database.${NC}"
    configure_local_db false
  fi
else
  echo -e "${YELLOW}Docker is not running. Using H2 in-memory database instead.${NC}"
  configure_local_db false
fi

# Export Spring active profile
export SPRING_PROFILES_ACTIVE=local

# Print application access information
echo -e "${GREEN}Starting application with local profile...${NC}"
echo -e "${YELLOW}API will be available at: http://localhost:8080/api${NC}"

if [ "$FORCE_H2" = true ] || [ "$docker_available" = false ]; then
  echo -e "${YELLOW}H2 Console will be available at: http://localhost:8080/api/h2-console${NC}"
  echo -e "${YELLOW}JDBC URL: jdbc:h2:mem:hiresync_db${NC}"
  echo -e "${YELLOW}Username: sa${NC}"
  echo -e "${YELLOW}Password: (leave empty)${NC}"
fi

echo -e "${YELLOW}Swagger UI will be available at: http://localhost:8080/api/swagger-ui.html${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop the application${NC}"

# Run the application with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Exit with the status of the last command
exit $? 