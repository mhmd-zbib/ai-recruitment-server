#!/bin/bash
set -e

# HireSync Production Run Script
# This script runs the HireSync application in production mode
# Usage: ./run-prod.sh [--local] [--tag VERSION]

# Colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color
BLUE='\033[0;34m'

# Default values
DOCKER_IMAGE="hiresync"
DOCKER_TAG="latest"
USE_LOCAL=false
ENV_FILE=".env.prod"
DOCKER_COMPOSE_FILE="docker/docker-compose.prod.yaml"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --local)
      USE_LOCAL=true
      shift
      ;;
    --tag)
      DOCKER_TAG="$2"
      shift 2
      ;;
    --env)
      ENV_FILE="$2"
      shift 2
      ;;
    --help)
      echo "Usage: ./run-prod.sh [OPTIONS]"
      echo
      echo "Options:"
      echo "  --local            Use locally built image instead of pulling from registry"
      echo "  --tag VERSION      Specify Docker image tag to use (default: latest)"
      echo "  --env FILE         Specify environment file to use (default: .env.prod)"
      echo "  --help             Display this help message"
      echo
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Run with --help for usage information."
      exit 1
      ;;
  esac
done

# Print colorful message
echo -e "${BLUE}===============================================${NC}"
echo -e "${BLUE}  HireSync Production Run Script${NC}"
echo -e "${BLUE}===============================================${NC}"
echo -e "Image: ${GREEN}$DOCKER_IMAGE:$DOCKER_TAG${NC}"
echo -e "Mode:  ${GREEN}$(if $USE_LOCAL; then echo "Local"; else echo "Registry"; fi)${NC}"
echo -e "Env:   ${GREEN}$ENV_FILE${NC}"
echo

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
  echo -e "${RED}Error: Docker is not installed${NC}"
  exit 1
fi

# Check if Docker Compose is available
if ! docker compose version &> /dev/null; then
  echo -e "${RED}Error: Docker Compose is not available${NC}"
  exit 1
fi

# Create environment file if it doesn't exist
if [ ! -f "$ENV_FILE" ]; then
  echo -e "${YELLOW}Environment file $ENV_FILE doesn't exist. Creating a default one...${NC}"
  cat > "$ENV_FILE" << EOF
# HireSync Production Environment Variables
DOCKER_IMAGE=$DOCKER_IMAGE
DOCKER_TAG=$DOCKER_TAG
POSTGRES_DB=hiresync
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
JWT_SECRET=replace-this-with-your-secure-secret-key-min-32-chars
JWT_ISSUER=hiresync
JWT_AUDIENCE=hiresync-app
JWT_EXPIRATION=86400000
CORS_ORIGINS=http://localhost:3000
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=INFO
# Set to true to apply the index fix for job created_by column
APPLY_INDEX_FIX=false
EOF
  echo -e "${YELLOW}Please edit $ENV_FILE with your production settings before continuing.${NC}"
  exit 1
fi

# Load environment variables
echo -e "${GREEN}Loading environment variables from $ENV_FILE${NC}"
export $(grep -v '^#' "$ENV_FILE" | xargs)

# Validate required variables
if [ -z "$POSTGRES_USER" ] || [ -z "$POSTGRES_PASSWORD" ]; then
  echo -e "${RED}Error: Database credentials are not set in $ENV_FILE${NC}"
  exit 1
fi

if [ -z "$JWT_SECRET" ] || [ ${#JWT_SECRET} -lt 32 ]; then
  echo -e "${RED}Error: JWT_SECRET is missing or too short (must be at least 32 characters)${NC}"
  exit 1
fi

# Pull Docker image if not using local
if [ "$USE_LOCAL" = false ]; then
  echo -e "${GREEN}Pulling Docker image $DOCKER_IMAGE:$DOCKER_TAG...${NC}"
  docker pull "$DOCKER_IMAGE:$DOCKER_TAG" || {
    echo -e "${RED}Failed to pull Docker image. Make sure it exists and you have access.${NC}"
    echo -e "${YELLOW}Try running with --local flag to use a locally built image.${NC}"
    exit 1
  }
else
  echo -e "${GREEN}Using local Docker image $DOCKER_IMAGE:$DOCKER_TAG${NC}"
  # Check if local image exists
  if ! docker image inspect "$DOCKER_IMAGE:$DOCKER_TAG" &> /dev/null; then
    echo -e "${RED}Error: Local image $DOCKER_IMAGE:$DOCKER_TAG does not exist${NC}"
    echo -e "${YELLOW}Build the image first with: docker build -t $DOCKER_IMAGE:$DOCKER_TAG -f docker/Dockerfile .${NC}"
    exit 1
  fi
fi

# Stop any running instances
echo -e "${GREEN}Stopping any running containers...${NC}"
docker compose -f "$DOCKER_COMPOSE_FILE" down --remove-orphans || true

# Set environment variables for Docker Compose
export DOCKER_IMAGE
export DOCKER_TAG

# Start the application
echo -e "${GREEN}Starting HireSync in production mode...${NC}"
docker compose -f "$DOCKER_COMPOSE_FILE" up -d

# Wait for application to start
echo -e "${GREEN}Waiting for application to start...${NC}"
attempt=1
max_attempts=30
until curl -s http://localhost:8080/actuator/health | grep -q "UP"; do
  if [ $attempt -gt $max_attempts ]; then
    echo -e "${RED}Application failed to start in time.${NC}"
    echo -e "${YELLOW}Checking logs:${NC}"
    docker compose -f "$DOCKER_COMPOSE_FILE" logs
    exit 1
  fi
  echo -e "${YELLOW}Waiting for application to start (attempt $attempt/$max_attempts)...${NC}"
  sleep 5
  attempt=$((attempt+1))
done

# Print success message
echo -e "\n${GREEN}✅ HireSync is now running in production mode!${NC}"
echo -e "${BLUE}===============================================${NC}"
echo -e "${BLUE}  HireSync Production Status${NC}"
echo -e "${BLUE}===============================================${NC}"
echo -e "API URL:          ${GREEN}http://localhost:8080/api${NC}"
echo -e "Health check:     ${GREEN}http://localhost:8080/actuator/health${NC}"
echo -e "Database:         ${GREEN}PostgreSQL on port 5432${NC}"
echo -e "Environment:      ${GREEN}${SPRING_PROFILES_ACTIVE:-production}${NC}"
echo -e "\n${YELLOW}Useful commands:${NC}"
echo -e "View logs:        ${GREEN}docker compose -f $DOCKER_COMPOSE_FILE logs -f${NC}"
echo -e "Stop application: ${GREEN}docker compose -f $DOCKER_COMPOSE_FILE down${NC}"
echo -e "Restart:          ${GREEN}docker compose -f $DOCKER_COMPOSE_FILE restart app${NC}"
echo -e "${BLUE}===============================================${NC}"

exit 0 