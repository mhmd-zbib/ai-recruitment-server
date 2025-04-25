#!/usr/bin/env bash
set -e

# Production deployment script for HireSync API
# This script is designed to be run on the production server

# Get the absolute path of the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.prod.yaml"

# Echo with timestamp
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Check if running as root (which we don't want)
if [ "$(id -u)" = "0" ]; then
  log "Warning: Running as root. Consider using a dedicated service user instead."
fi

# Load environment variables from .env file or use provided env file
ENV_FILE="$PROJECT_ROOT/.env"
if [ -n "$1" ] && [ -f "$1" ]; then
  ENV_FILE="$1"
  log "Using provided environment file: $ENV_FILE"
fi

if [ -f "$ENV_FILE" ]; then
  log "Loading environment variables from $ENV_FILE"
  export $(grep -v '^#' "$ENV_FILE" | xargs)
else
  log "Error: Environment file not found at $ENV_FILE"
  log "Please create an environment file or provide one as an argument."
  exit 1
fi

# Verify environment
if [ "$SPRING_PROFILES_ACTIVE" != "prod" ]; then
  log "Error: Not using production profile. Set SPRING_PROFILES_ACTIVE=prod in your environment file."
  exit 1
fi

# Check Docker is running
if ! docker info &>/dev/null; then
  log "Error: Docker is not running. Please start Docker and try again."
  exit 1
fi

# Verify Docker Compose file exists
if [ ! -f "$COMPOSE_FILE" ]; then
  log "Error: Production Docker Compose file not found at $COMPOSE_FILE"
  exit 1
fi

# Pull latest Docker image
log "Pulling latest Docker image from Docker Hub..."
if [ -z "$DOCKER_USERNAME" ] || [ -z "$APP_NAME" ]; then
  log "Error: DOCKER_USERNAME or APP_NAME not set in environment file."
  exit 1
fi

docker pull "$DOCKER_USERNAME/$APP_NAME:latest"
if [ $? -ne 0 ]; then
  log "Error: Failed to pull Docker image. Check credentials and internet connection."
  exit 1
fi

# Use the appropriate docker-compose command based on Docker version
if docker compose version &>/dev/null; then
  DOCKER_COMPOSE_CMD="docker compose"
else
  DOCKER_COMPOSE_CMD="docker-compose"
fi

# Start application with Docker Compose
log "Starting application in production mode..."
$DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" down
$DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" up -d

# Verify application is running
log "Verifying application startup..."
sleep 10

if ! $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" ps | grep -q "Up"; then
  log "Error: Application failed to start. Check logs:"
  $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" logs
  exit 1
fi

# Application is running successfully
log "✅ Application successfully deployed and running in production mode"
log "🚀 API should be available at http://localhost:$PORT/api"
log "   Health check endpoint: http://localhost:$PORT/api/actuator/health"

# Check for database migrations flag
if [ "$1" = "--migrate" ] || [ "$2" = "--migrate" ]; then
  log "Running database migrations..."
  # Add your migration commands here
  # e.g., docker exec hiresync-app ./mvnw flyway:migrate
  log "Database migrations completed."
fi

log "Deployment completed successfully." 