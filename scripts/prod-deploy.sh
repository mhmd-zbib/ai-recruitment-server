#!/bin/bash

# Script to deploy and run HireSync application in production mode

# Get script directory for reliable sourcing
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Source the database utility functions
source "$SCRIPT_DIR/db-utils.sh"

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Production Deployment${NC}"
echo -e "${BLUE}========================================${NC}"

# Process command-line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --docker)
      USE_DOCKER=true
      shift
      ;;
    --jar)
      USE_JAR=true
      shift
      ;;
    --help)
      echo -e "Usage: ./scripts/prod-deploy.sh [options]"
      echo -e "Options:"
      echo -e "  --docker              Deploy using Docker Compose"
      echo -e "  --jar                 Deploy as a standalone JAR"
      echo -e "  --help                Display this help message"
      echo -e ""
      echo -e "Examples:"
      echo -e "  ./scripts/prod-deploy.sh --docker   # Deploy with Docker Compose"
      echo -e "  ./scripts/prod-deploy.sh --jar      # Deploy as JAR (requires env vars)"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Change to project root
cd "$PROJECT_ROOT" || exit 1

# If no deployment method specified, show help
if [ -z "$USE_DOCKER" ] && [ -z "$USE_JAR" ]; then
  echo -e "${YELLOW}Please specify a deployment method: --docker or --jar${NC}"
  echo -e "Run with --help for more information."
  exit 1
fi

# Function to deploy with Docker Compose
deploy_with_docker() {
  echo -e "${BLUE}Deploying with Docker Compose...${NC}"
  
  # Check Docker status
  if ! check_docker; then
    echo -e "${RED}Error: Docker is required for Docker deployment.${NC}"
    exit 1
  fi
  
  # Load environment variables if available
  load_env_file
  
  # Run Docker Compose
  echo -e "${YELLOW}Starting all services with docker-compose.prod.yaml...${NC}"
  docker-compose -f docker-compose.prod.yaml up -d
  
  # Check if services started successfully
  if docker-compose -f docker-compose.prod.yaml ps | grep -q "Exit"; then
    echo -e "${RED}Error: Some services failed to start. Check logs with:${NC}"
    echo -e "${YELLOW}docker-compose -f docker-compose.prod.yaml logs${NC}"
    exit 1
  fi
  
  echo -e "${GREEN}Deployment successful! Services are running.${NC}"
  echo -e "${YELLOW}View logs with: docker-compose -f docker-compose.prod.yaml logs -f${NC}"
}

# Function to deploy as JAR
deploy_as_jar() {
  echo -e "${BLUE}Deploying as standalone JAR...${NC}"
  
  # Check if required environment variables are set
  if ! check_prod_env_vars; then
    echo -e "${RED}Cannot proceed with deployment.${NC}"
    exit 1
  fi
  
  # Ensure we have the JAR file
  if [ ! -f "target/hiresync-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${YELLOW}Building application...${NC}"
    ./mvnw clean package -DskipTests
  fi
  
  # Export Spring active profile
  export SPRING_PROFILES_ACTIVE=prod
  
  # Run the application
  echo -e "${GREEN}Starting application...${NC}"
  java -jar target/hiresync-0.0.1-SNAPSHOT.jar
}

# Deploy based on selected method
if [ "$USE_DOCKER" = true ]; then
  deploy_with_docker
elif [ "$USE_JAR" = true ]; then
  deploy_as_jar
fi

exit $? 