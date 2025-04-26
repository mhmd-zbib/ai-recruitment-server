#!/usr/bin/env bash
set -e

# Parse arguments
DEBUG_MODE=false
RESTART=false

for arg in "$@"; do
  case $arg in
    --debug)
      DEBUG_MODE=true
      shift
      ;;
    --restart)
      RESTART=true
      shift
      ;;
    *)
      # Unknown option
      ;;
  esac
done

# Container names
DEVTOOLS_CONTAINER="hiresync-devtools"
POSTGRES_CONTAINER="hiresync-postgres"

# Get project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.local.yaml"

# Print banner
echo "===================================="
echo "     HireSync Development Tool      "
echo "===================================="

# Load environment variables
if [ -f "$PROJECT_ROOT/.env" ]; then
  export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
  echo "✅ Loaded environment from .env"
  
  # Display loaded environment variables
  echo "📋 Environment variables:"
  echo "   PORT=${PORT:-8080}"
  echo "   DB_PORT=${DB_PORT:-5432}"
  echo "   DB_NAME=${DB_NAME:-hiresync}"
  echo "   DB_USER=${DB_USER:-hiresync}"
  echo "   SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-local}"
  echo "   DEBUG_MODE=${DEBUG_MODE:-false}"
fi

# Check if Docker is running
if ! docker info &>/dev/null; then
  echo "❌ Docker is not running. Please start Docker and try again."
  exit 1
fi

# Determine docker-compose command
if docker compose version &>/dev/null; then
  DOCKER_COMPOSE_CMD="docker compose"
else
  DOCKER_COMPOSE_CMD="docker-compose"
fi

# Check if containers need to be restarted
if [ "$RESTART" = true ]; then
  echo "🔄 Restarting containers..."
  $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" down
fi

# Start containers if they are not running
if ! docker ps | grep -q "$DEVTOOLS_CONTAINER"; then
  echo "🚀 Starting Docker containers..."
  $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" up -d
  
  echo "⏳ Waiting for containers to be ready..."
  sleep 5
else
  echo "✅ Containers already running"
fi

# Show running containers
echo "🐳 Current containers:"
docker ps | grep "hiresync-" | awk '{print "   " $NF " (" $2 ")"}'

# Check the PORT variable
PORT=${PORT:-8080}
echo "🌐 Application will be available at: http://localhost:$PORT/api"
echo "📚 Swagger UI: http://localhost:$PORT/api/swagger-ui.html"

# Configure Maven logging
if [ "$DEBUG_MODE" = true ]; then
  LOG_LEVEL="DEBUG"
  MVN_LOG_LEVEL="-Dlogging.level.root=DEBUG -Dlogging.level.com.zbib.hiresync=DEBUG"
  echo "🐞 Debug mode enabled - verbose logging"
else
  LOG_LEVEL="INFO"
  MVN_LOG_LEVEL="-Dlogging.level.root=INFO"
  echo "ℹ️ Standard logging mode"
fi

# Run the application
echo "🏃 Starting Spring Boot application..."
echo "💡 Press Ctrl+C to stop the application (container will keep running)"

docker exec -it $DEVTOOLS_CONTAINER bash -c "cd /app && \
  mvn spring-boot:run \
  -Dspring-boot.run.profiles=local \
  $MVN_LOG_LEVEL \
  -Dspring-boot.run.jvmArguments=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005\"" 