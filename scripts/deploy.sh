#!/usr/bin/env bash
set -e

# Deploy script for HireSync application
# Usage: ./deploy.sh [dev|prod]

# Default to dev environment if not specified
ENVIRONMENT=${1:-dev}
COMPOSE_FILE="docker-compose.${ENVIRONMENT}.yaml"

echo "🚀 Deploying HireSync to ${ENVIRONMENT} environment"

# Validate environment
if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
  echo "⛔ Error: Invalid environment. Use 'dev' or 'prod'"
  exit 1
fi

# Load environment variables
if [ -f ".env.${ENVIRONMENT}" ]; then
  echo "📋 Loading environment variables from .env.${ENVIRONMENT}"
  source ".env.${ENVIRONMENT}"
else
  echo "⚠️ Warning: .env.${ENVIRONMENT} file not found, using default values"
fi

# Pull latest images
echo "📦 Pulling latest Docker images"
docker-compose -f "docker/${COMPOSE_FILE}" pull

# Start or restart services
echo "🔄 Starting/restarting services"
docker-compose -f "docker/${COMPOSE_FILE}" up -d

# Check service health
echo "🔍 Checking service health"
timeout 60s bash -c 'until docker-compose -f "docker/$0" ps app | grep -q "(healthy)"; do echo "Waiting for application to be healthy..."; sleep 5; done' "${COMPOSE_FILE}" || {
  echo "⚠️ Warning: Service health check timed out"
  docker-compose -f "docker/${COMPOSE_FILE}" ps
  docker-compose -f "docker/${COMPOSE_FILE}" logs app
}

# Cleanup old images
echo "🧹 Cleaning up old and dangling images"
docker image prune -f

echo "✅ Deployment completed successfully" 