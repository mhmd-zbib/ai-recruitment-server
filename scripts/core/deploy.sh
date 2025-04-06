#!/usr/bin/env bash

# Description: Deploys the application to staging or production environments.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"
source "$SCRIPT_DIR/../utils/env.sh"

# Default values
ENVIRONMENT="staging"
SKIP_TESTS=false
SKIP_BUILD=false
SKIP_MIGRATIONS=false

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
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync deploy [--env=staging|production] [--skip-tests] [--skip-build] [--skip-migrations]"
      exit 1
      ;;
  esac
done

# Container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Validate environment
case "$ENVIRONMENT" in
  staging|production)
    ;;
  *)
    log_error "Invalid environment: $ENVIRONMENT"
    echo "Valid environments: staging, production"
    exit 1
    ;;
esac

log_section "Deploying to $ENVIRONMENT"

# Load environment variables
ENV_FILE="$PROJECT_ROOT/.env.$ENVIRONMENT"
if [ -f "$ENV_FILE" ]; then
  log_info "Loading environment variables from $ENV_FILE"
  load_env "$ENV_FILE"
else
  log_error "Environment file not found: $ENV_FILE"
  exit 1
fi

# Check if deployment credentials are available
if [ -z "$DEPLOY_USER" ] || [ -z "$DEPLOY_HOST" ]; then
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

# Package the deployment
log_info "Packaging deployment"
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