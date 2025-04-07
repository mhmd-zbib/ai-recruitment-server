#!/usr/bin/env bash

# Description: Deploys the application to dev, staging or production environments.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"
source "$SCRIPT_DIR/../utils/env.sh"

# Default values
ENVIRONMENT="dev"
SKIP_TESTS=false
SKIP_BUILD=false
SKIP_MIGRATIONS=false
DOCKER_DEPLOYMENT=false
IMAGE_TAG=""
REGISTRY=""
ORG=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --env=*)
      ENVIRONMENT="${1#*=}"
      shift
      ;;
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    --skip-migrations)
      SKIP_MIGRATIONS=true
      shift
      ;;
    --docker)
      DOCKER_DEPLOYMENT=true
      shift
      ;;
    --image-tag=*)
      IMAGE_TAG="${1#*=}"
      shift
      ;;
    --registry=*)
      REGISTRY="${1#*=}"
      shift
      ;;
    --org=*)
      ORG="${1#*=}"
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync deploy [--env=dev|staging|production] [--skip-tests] [--skip-build] [--skip-migrations] [--docker] [--image-tag=TAG] [--registry=REGISTRY] [--org=ORGANIZATION]"
      exit 1
      ;;
  esac
done

# Container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Validate environment
case "$ENVIRONMENT" in
  dev|staging|production)
    ;;
  *)
    log_error "Invalid environment: $ENVIRONMENT"
    echo "Valid environments: dev, staging, production"
    exit 1
    ;;
esac

log_section "Deploying to $ENVIRONMENT"

# Load environment variables - support CI environment
ENV_FILE="$PROJECT_ROOT/.env.$ENVIRONMENT"
if [ -f "$ENV_FILE" ]; then
  log_info "Loading environment variables from $ENV_FILE"
  load_env "$ENV_FILE"
elif [ "$CI" == "true" ]; then
  log_info "Running in CI environment, using CI variables"
else
  log_error "Environment file not found: $ENV_FILE"
  exit 1
fi

# For Docker deployment, ensure we have required parameters
if [[ "$DOCKER_DEPLOYMENT" == true ]]; then
  # Set defaults for CI environment
  if [ -z "$IMAGE_TAG" ] && [ "$CI" == "true" ]; then
    if [ -n "$GITHUB_SHA" ]; then
      IMAGE_TAG="${GITHUB_SHA:0:8}"
      log_info "Using auto-generated image tag: $IMAGE_TAG"
    else
      log_error "No image tag provided for Docker deployment"
      exit 1
    fi
  fi
  
  if [ -z "$REGISTRY" ]; then
    REGISTRY="ghcr.io"
    log_info "Using default registry: $REGISTRY"
  fi
  
  if [ -z "$ORG" ] && [ -n "$GITHUB_REPOSITORY_OWNER" ]; then
    ORG="$GITHUB_REPOSITORY_OWNER"
    log_info "Using GitHub repository owner for organization: $ORG"
  elif [ -z "$ORG" ]; then
    ORG="zbib"
    log_info "Using default organization: $ORG"
  fi
fi

# Check if deployment credentials are available for traditional deployment
if [[ "$DOCKER_DEPLOYMENT" != true ]] && { [ -z "$DEPLOY_USER" ] || [ -z "$DEPLOY_HOST" ]; }; then
  log_error "Deployment credentials not found in environment variables"
  exit 1
fi

# Build the application if not skipped
if [[ "$SKIP_BUILD" != true ]]; then
  log_info "Building application"
  
  BUILD_ARGS="clean package"
  if [[ "$SKIP_TESTS" == true ]]; then
    BUILD_ARGS="$BUILD_ARGS -DskipTests"
    log_info "Skipping tests"
  fi
  
  # Check if container is running for build
  if docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
    docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && mvn $BUILD_ARGS"
  else
    # Build locally if container is not running
    mvn $BUILD_ARGS
  fi
else
  log_info "Skipping build"
fi

# Docker-based deployment 
if [[ "$DOCKER_DEPLOYMENT" == true ]]; then
  log_info "Preparing Docker deployment"
  
  DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.prod.yaml"
  if [ "$ENVIRONMENT" == "dev" ]; then
    # For dev, we might want to use a different compose file or the same prod one
    log_info "Using production docker-compose file for dev deployment"
  fi
  
  # Set environment variables for docker-compose
  export IMAGE_NAME="hiresync"
  export IMAGE_TAG="$IMAGE_TAG"
  export REGISTRY="$REGISTRY"
  export REPO_OWNER="$ORG"
  export SPRING_PROFILES_ACTIVE="$ENVIRONMENT"
  
  if [ -n "$DEPLOY_HOST" ]; then
    # Remote deployment
    log_info "Deploying Docker containers to $DEPLOY_HOST"
    
    # Create remote deployment script
    DEPLOY_SCRIPT=$(mktemp)
    cat > "$DEPLOY_SCRIPT" << EOF
#!/bin/bash
set -e

# Create deployment directory if it doesn't exist
mkdir -p ~/hiresync-deploy

# Copy files
cat > ~/hiresync-deploy/docker-compose.yaml << 'COMPOSE'
$(cat "$DOCKER_COMPOSE_FILE")
COMPOSE

# Set environment variables
export IMAGE_NAME="hiresync"
export IMAGE_TAG="$IMAGE_TAG"
export REGISTRY="$REGISTRY"
export REPO_OWNER="$ORG"
export SPRING_PROFILES_ACTIVE="$ENVIRONMENT"
export DB_USER="${DB_USER:-hiresync}"
export DB_PASSWORD="${DB_PASSWORD:-hiresync}"
export DB_NAME="${DB_NAME:-hiresync}"
export TZ="${TZ:-UTC}"
export APP_PORT="${APP_PORT:-8080}"
export ACTUATOR_PORT="${ACTUATOR_PORT:-8081}"

# Pull the images
cd ~/hiresync-deploy
docker pull $REGISTRY/$REPO_OWNER/$IMAGE_NAME:$IMAGE_TAG

# Stop and start containers
docker compose down || true
docker compose up -d

# Show status
docker compose ps
EOF
    
    chmod +x "$DEPLOY_SCRIPT"
    
    # Copy and execute the script on the remote host
    scp "$DEPLOY_SCRIPT" "$DEPLOY_USER@$DEPLOY_HOST:~/deploy-hiresync.sh"
    ssh "$DEPLOY_USER@$DEPLOY_HOST" "bash ~/deploy-hiresync.sh"
    
    # Cleanup temp file
    rm "$DEPLOY_SCRIPT"
  else
    # Local deployment for testing
    log_info "Deploying Docker containers locally"
    cd "$PROJECT_ROOT"
    docker compose -f "$DOCKER_COMPOSE_FILE" down || true
    docker compose -f "$DOCKER_COMPOSE_FILE" up -d
  fi
  
  log_success "Docker deployment to $ENVIRONMENT complete"
else
  # Traditional JAR deployment
  log_info "Preparing traditional JAR deployment"
  
  # Package the deployment
  JAR_FILE=$(find "$PROJECT_ROOT/target" -name "*.jar" | grep -v "sources" | grep -v "javadoc" | head -1)

  if [ -z "$JAR_FILE" ]; then
    log_error "No JAR file found in target directory"
    exit 1
  fi

  JAR_FILENAME=$(basename "$JAR_FILE")
  log_info "Using JAR file: $JAR_FILENAME"

  # Deploy to server
  log_info "Deploying to $DEPLOY_HOST"
  scp "$JAR_FILE" "$DEPLOY_USER@$DEPLOY_HOST:~/deploy/"

  # Run remote commands
  log_info "Executing deployment commands on remote server"
  ssh "$DEPLOY_USER@$DEPLOY_HOST" "bash -s" << EOF
    cd ~/deploy
    echo "Stopping existing application"
    sudo systemctl stop hiresync || true
    
    echo "Backing up existing JAR"
    mv /opt/hiresync/app.jar /opt/hiresync/app.jar.backup || true
    
    echo "Installing new JAR"
    sudo cp $JAR_FILENAME /opt/hiresync/app.jar
    
    echo "Setting permissions"
    sudo chown hiresync:hiresync /opt/hiresync/app.jar
    sudo chmod 750 /opt/hiresync/app.jar
    
    # Run migrations if not skipped
    if [[ "$SKIP_MIGRATIONS" != true ]]; then
      echo "Running database migrations"
      cd /opt/hiresync
      sudo -u hiresync java -jar app.jar --migrate-only
    fi
    
    echo "Starting application"
    sudo systemctl start hiresync
    
    echo "Checking application status"
    sudo systemctl status hiresync --no-pager
EOF

  log_success "Deployment to $ENVIRONMENT complete"
fi 