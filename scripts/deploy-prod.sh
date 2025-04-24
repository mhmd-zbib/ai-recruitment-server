#!/bin/bash
set -e

# HireSync Production Deployment Script
# This script deploys the HireSync application to production or staging environments

# Colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color
BLUE='\033[0;34m'

# Print with timestamp
log() {
  local level=$1
  local message=$2
  local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
  
  case $level in
    INFO)
      echo -e "${GREEN}[$timestamp] [INFO] $message${NC}"
      ;;
    WARN)
      echo -e "${YELLOW}[$timestamp] [WARN] $message${NC}"
      ;;
    ERROR)
      echo -e "${RED}[$timestamp] [ERROR] $message${NC}"
      ;;
    *)
      echo -e "[$timestamp] [$level] $message"
      ;;
  esac
}

# Load environment variables
if [ -f .env ]; then
  log "INFO" "Loading environment variables from .env file"
  export $(grep -v '^#' .env | xargs)
else
  log "ERROR" "No .env file found. Please create one."
  exit 1
fi

# Check required variables
check_required_vars() {
  local missing=false
  for var in "$@"; do
    if [ -z "${!var}" ]; then
      log "ERROR" "Required variable $var is not set"
      missing=true
    fi
  done
  
  if [ "$missing" = true ]; then
    log "ERROR" "Missing required variables. Exiting."
    exit 1
  fi
}

# Check required variables
check_required_vars DOCKER_IMAGE DOCKER_TAG POSTGRES_USER POSTGRES_PASSWORD

log "INFO" "Starting deployment of HireSync version $DOCKER_TAG"
log "INFO" "Using Docker image: $DOCKER_IMAGE:$DOCKER_TAG"

# Pull latest Docker images
log "INFO" "Pulling latest Docker images"
docker pull $DOCKER_IMAGE:$DOCKER_TAG

# Check if containers are already running
if docker compose ps | grep -q "hiresync"; then
  log "INFO" "HireSync containers are already running. Taking backup before update..."
  
  # Backup database if PostgreSQL is running
  if docker compose ps | grep -q "postgres"; then
    BACKUP_DIR="./backups"
    mkdir -p $BACKUP_DIR
    BACKUP_FILE="$BACKUP_DIR/hiresync_backup_$(date +%Y%m%d_%H%M%S).sql"
    
    log "INFO" "Creating database backup to $BACKUP_FILE"
    docker compose exec -T postgres pg_dump -U $POSTGRES_USER hiresync > $BACKUP_FILE
    
    if [ $? -eq 0 ]; then
      log "INFO" "Database backup completed successfully"
    else
      log "WARN" "Database backup failed, proceeding anyway..."
    fi
  fi
  
  # Stop the running containers gracefully
  log "INFO" "Stopping running containers"
  docker compose down --remove-orphans
else
  log "INFO" "No existing containers found"
fi

# Fix for the idx_job_created_by index issue
if [ "$APPLY_INDEX_FIX" = "true" ]; then
  log "INFO" "Applying index fix for idx_job_created_by"
  cat > fix-index.sql << EOF
-- Drop potentially incorrectly created index
DROP INDEX IF EXISTS idx_job_created_by;

-- Drop the current index if it exists (for clean replacement)
DROP INDEX IF EXISTS idx_job_created_by_id;

-- Create the correctly named index on created_by_id
CREATE INDEX idx_job_created_by_id ON jobs (created_by_id);
EOF

  # We'll apply this after the application has initialized the schema
fi

# Start the application with the new version
log "INFO" "Starting HireSync application with Docker Compose"
docker compose up -d

# Wait for the database to be ready
log "INFO" "Waiting for database to be ready..."
attempt=1
max_attempts=30
until docker compose exec -T postgres pg_isready -U $POSTGRES_USER -d hiresync > /dev/null 2>&1; do
  if [ $attempt -gt $max_attempts ]; then
    log "ERROR" "Database failed to start in time"
    exit 1
  fi
  
  log "INFO" "Waiting for database to be ready (attempt $attempt of $max_attempts)..."
  sleep 2
  attempt=$((attempt+1))
done

# Apply the index fix if needed
if [ "$APPLY_INDEX_FIX" = "true" ]; then
  log "INFO" "Applying index fix to database"
  sleep 10 # Give the app time to initialize the schema
  docker compose exec -T postgres psql -U $POSTGRES_USER -d hiresync -f /tmp/fix-index.sql
  log "INFO" "Index fix applied successfully"
fi

# Wait for the application to be ready
log "INFO" "Waiting for application to be ready..."
attempt=1
max_attempts=30
until curl -s http://localhost:8080/actuator/health | grep -q "UP"; do
  if [ $attempt -gt $max_attempts ]; then
    log "ERROR" "Application failed to start in time. Checking logs:"
    docker compose logs app
    exit 1
  fi
  
  log "INFO" "Waiting for application to be ready (attempt $attempt of $max_attempts)..."
  sleep 5
  attempt=$((attempt+1))
done

# Run database migrations if needed (placeholder for future use)
if [ "$RUN_MIGRATIONS" = "true" ]; then
  log "INFO" "Running database migrations"
  # Here you would run your migration tool or script
  # Example: docker compose exec app ./run-migrations.sh
fi

# Verify deployment
log "INFO" "Verifying deployment..."
if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
  log "INFO" "✅ Deployment verified successfully! HireSync is running."
  
  # Print deployment info
  echo -e "\n${BLUE}===============================================${NC}"
  echo -e "${BLUE}  HireSync Deployment Summary${NC}"
  echo -e "${BLUE}===============================================${NC}"
  echo -e "Image:     ${GREEN}$DOCKER_IMAGE:$DOCKER_TAG${NC}"
  echo -e "Deployed:  ${GREEN}$(date)${NC}"
  echo -e "Profile:   ${GREEN}${SPRING_PROFILES_ACTIVE:-production}${NC}"
  echo -e "\nTo view logs: ${YELLOW}docker compose logs -f${NC}"
  echo -e "To stop:      ${YELLOW}docker compose down${NC}"
  echo -e "${BLUE}===============================================${NC}\n"
else
  log "ERROR" "Deployment verification failed. HireSync might not be running correctly."
  docker compose logs app
  exit 1
fi

exit 0 