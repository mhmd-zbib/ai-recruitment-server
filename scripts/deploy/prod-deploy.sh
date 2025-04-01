#!/bin/bash

# Script to deploy HireSync application to production

# Get script directory for reliable sourcing
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default values
USE_DOCKER=false
USE_JAR=false
SKIP_TESTS=false
SKIP_VERIFY=false
PERFORM_BACKUP=true
ROLLBACK_ON_FAILURE=true
DRY_RUN=false

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
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --skip-verify)
      SKIP_VERIFY=true
      shift
      ;;
    --no-backup)
      PERFORM_BACKUP=false
      shift
      ;;
    --no-rollback)
      ROLLBACK_ON_FAILURE=false
      shift
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --help)
      echo -e "Usage: ./scripts/deploy/prod-deploy.sh [options]"
      echo -e "Options:"
      echo -e "  --docker      Deploy using Docker (recommended)"
      echo -e "  --jar         Deploy using standalone JAR"
      echo -e "  --skip-tests  Skip running tests before deployment"
      echo -e "  --skip-verify Skip verification checks"
      echo -e "  --no-backup   Skip database backup before deployment"
      echo -e "  --no-rollback Do not automatically rollback on failure"
      echo -e "  --dry-run     Show what would be done without making changes"
      echo -e "  --help        Display this help message"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Check deployment method
if [ "$USE_DOCKER" = false ] && [ "$USE_JAR" = false ]; then
  echo -e "${RED}Error: No deployment method specified.${NC}"
  echo -e "Please use --docker or --jar option."
  exit 1
fi

if [ "$USE_DOCKER" = true ] && [ "$USE_JAR" = true ]; then
  echo -e "${RED}Error: Multiple deployment methods specified.${NC}"
  echo -e "Please use only one of --docker or --jar."
  exit 1
fi

# Change to project root
cd "$PROJECT_ROOT" || exit 1

# Load environment variables
if [ -f .env ]; then
  echo -e "${GREEN}Loading environment variables from .env file...${NC}"
  # Export variables for use in the script
  set -a
  . ./.env
  set +a
else
  echo -e "${YELLOW}Warning: .env file not found, using default values.${NC}"
fi

# Verify required environment variables
if [ "$USE_JAR" = true ]; then
  REQUIRED_VARS=("JDBC_DATABASE_URL" "JDBC_DATABASE_USERNAME" "JDBC_DATABASE_PASSWORD" "JWT_SECRET")
  MISSING_VARS=()
  
  for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
      MISSING_VARS+=("$var")
    fi
  done
  
  if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    echo -e "${RED}Error: Missing required environment variables:${NC}"
    for var in "${MISSING_VARS[@]}"; do
      echo -e "  - $var"
    done
    exit 1
  fi
fi

# Verify prerequisites
if [ "$USE_DOCKER" = true ]; then
  echo -e "${BLUE}Checking Docker availability...${NC}"
  if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker and try again.${NC}"
    exit 1
  fi
  
  echo -e "${BLUE}Checking Docker Compose availability...${NC}"
  if ! docker-compose --version > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker Compose is not installed. Please install Docker Compose and try again.${NC}"
    exit 1
  fi
fi

# Run verification if not skipped
if [ "$SKIP_VERIFY" = false ]; then
  echo -e "${BLUE}Running code verification...${NC}"
  
  if [ "$DRY_RUN" = true ]; then
    echo -e "${CYAN}[DRY RUN] Would run verification checks${NC}"
  else
    "$PROJECT_ROOT/scripts/build/verify.sh"
    
    if [ $? -ne 0 ]; then
      echo -e "${RED}Error: Verification failed. Fix issues before deploying.${NC}"
      exit 1
    fi
  fi
fi

# Run tests if not skipped
if [ "$SKIP_TESTS" = false ]; then
  echo -e "${BLUE}Running tests...${NC}"
  
  if [ "$DRY_RUN" = true ]; then
    echo -e "${CYAN}[DRY RUN] Would run tests${NC}"
  else
    mvn test -Dspring.profiles.active=test
    
    if [ $? -ne 0 ]; then
      echo -e "${RED}Error: Tests failed. Fix failing tests before deploying.${NC}"
      exit 1
    fi
  fi
fi

# Perform database backup if enabled
if [ "$PERFORM_BACKUP" = true ]; then
  echo -e "${BLUE}Creating database backup...${NC}"
  BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
  
  if [ "$DRY_RUN" = true ]; then
    echo -e "${CYAN}[DRY RUN] Would create database backup as $BACKUP_FILE${NC}"
  else
    if [ "$USE_DOCKER" = true ]; then
      # Try to backup using Docker if available
      if docker ps | grep -q "hiresync-postgres"; then
        docker exec hiresync-postgres pg_dump -U "${DB_USERNAME:-hiresync_user}" -d "${DB_NAME:-hiresync_db}" > "backups/$BACKUP_FILE"
        echo -e "${GREEN}Database backup created: backups/$BACKUP_FILE${NC}"
      else
        echo -e "${YELLOW}Warning: Docker PostgreSQL container not found. Skipping backup.${NC}"
      fi
    else
      # Use environment variables for external database
      if command -v pg_dump > /dev/null 2>&1; then
        mkdir -p backups
        PGPASSWORD="$JDBC_DATABASE_PASSWORD" pg_dump -h "${JDBC_DATABASE_URL#*//}" -U "$JDBC_DATABASE_USERNAME" > "backups/$BACKUP_FILE"
        echo -e "${GREEN}Database backup created: backups/$BACKUP_FILE${NC}"
      else
        echo -e "${YELLOW}Warning: pg_dump not found. Skipping backup.${NC}"
      fi
    fi
  fi
fi

# Build and deploy
if [ "$USE_DOCKER" = true ]; then
  echo -e "${BLUE}Deploying with Docker...${NC}"
  
  if [ "$DRY_RUN" = true ]; then
    echo -e "${CYAN}[DRY RUN] Would build Docker image and deploy${NC}"
  else
    # Build Docker image
    echo -e "${BLUE}Building Docker image...${NC}"
    VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    "$PROJECT_ROOT/scripts/build/docker-build.sh" --version="$VERSION"
    
    if [ $? -ne 0 ]; then
      echo -e "${RED}Error: Docker build failed.${NC}"
      exit 1
    fi
    
    # Deploy with Docker Compose
    echo -e "${BLUE}Starting Docker Compose services...${NC}"
    docker-compose -f docker-compose.prod.yaml up -d
    
    if [ $? -ne 0 ]; then
      echo -e "${RED}Error: Docker Compose startup failed.${NC}"
      
      if [ "$ROLLBACK_ON_FAILURE" = true ]; then
        echo -e "${YELLOW}Rolling back to previous state...${NC}"
        docker-compose -f docker-compose.prod.yaml down
        
        echo -e "${YELLOW}Deployment failed. Please check logs for more information.${NC}"
      fi
      
      exit 1
    fi
  fi
  
  echo -e "${GREEN}Docker deployment complete!${NC}"
  echo -e "${GREEN}The application is now running at http://localhost:${PORT:-8080}/api${NC}"
  
elif [ "$USE_JAR" = true ]; then
  echo -e "${BLUE}Deploying with standalone JAR...${NC}"
  
  if [ "$DRY_RUN" = true ]; then
    echo -e "${CYAN}[DRY RUN] Would build and deploy JAR${NC}"
  else
    # Build JAR
    echo -e "${BLUE}Building JAR...${NC}"
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
      echo -e "${RED}Error: JAR build failed.${NC}"
      exit 1
    fi
    
    # Find the JAR file
    JAR_FILE=$(find target -name "*.jar" -not -name "*sources.jar" -not -name "*javadoc.jar")
    
    if [ -z "$JAR_FILE" ]; then
      echo -e "${RED}Error: JAR file not found.${NC}"
      exit 1
    fi
    
    # Stop any running instance
    echo -e "${BLUE}Stopping any running instance...${NC}"
    if pgrep -f "$JAR_FILE" > /dev/null; then
      pkill -f "$JAR_FILE"
      sleep 5
    fi
    
    # Start the application
    echo -e "${BLUE}Starting application...${NC}"
    nohup java -jar "$JAR_FILE" --spring.profiles.active=prod > logs/application.log 2>&1 &
    
    # Check if the application started successfully
    sleep 10
    if ! pgrep -f "$JAR_FILE" > /dev/null; then
      echo -e "${RED}Error: Application failed to start.${NC}"
      echo -e "${YELLOW}Check logs/application.log for more information.${NC}"
      exit 1
    fi
  fi
  
  echo -e "${GREEN}JAR deployment complete!${NC}"
  echo -e "${GREEN}The application is now running at http://localhost:${PORT:-8080}/api${NC}"
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo -e "${BLUE}========================================${NC}"

exit 0 