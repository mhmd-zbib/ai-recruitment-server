#!/bin/bash

# HireSync Development Environment Manager
# Unified script for managing development environments
# Combines functionality from local-start.sh, dev-start.sh, and local-dev.sh

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source the database utility functions
source "$PROJECT_ROOT/scripts/utils/db-utils.sh"

# Set colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
MODE="local"
USE_DOCKER=true
VERBOSE=false
DEBUG=false
DRY_RUN=false
SKIP_DB_WAIT=false
USE_EXISTING_DB=false

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Development Environment Manager${NC}"
echo -e "${BLUE}========================================${NC}"

# Print usage information
show_help() {
  echo -e "Usage: $0 [options]"
  echo -e ""
  echo -e "Options:"
  echo -e "  --mode MODE       Environment mode: local, dev, or test (default: $MODE)"
  echo -e "  --no-docker       Skip Docker PostgreSQL setup"
  echo -e "  --use-existing-db Use existing PostgreSQL database without checking"
  echo -e "  --skip-db-wait    Skip waiting for PostgreSQL to be ready"
  echo -e "  --verbose         Enable verbose output"
  echo -e "  --debug           Enable debug mode"
  echo -e "  --dry-run         Show what would be done without making changes"
  echo -e "  --help            Display this help message"
  echo -e ""
  echo -e "Examples:"
  echo -e "  $0                            # Start local development with Docker"
  echo -e "  $0 --mode dev                 # Start development environment"
  echo -e "  $0 --mode test --no-docker    # Start test environment without Docker"
  echo -e "  $0 --use-existing-db          # Use existing PostgreSQL database"
  exit 0
}

# Process command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="$2"
      shift 2
      ;;
    --no-docker)
      USE_DOCKER=false
      shift
      ;;
    --use-existing-db)
      USE_EXISTING_DB=true
      shift
      ;;
    --skip-db-wait)
      SKIP_DB_WAIT=true
      shift
      ;;
    --verbose)
      VERBOSE=true
      shift
      ;;
    --debug)
      DEBUG=true
      shift
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --help)
      show_help
      ;;
    *)
      echo -e "${RED}Error: Unknown option: $1${NC}"
      show_help
      ;;
  esac
done

# Validate mode
if [[ ! "$MODE" =~ ^(local|dev|test)$ ]]; then
  echo -e "${RED}Error: Invalid mode '$MODE'. Must be one of: local, dev, test${NC}"
  exit 1
fi

# Load environment variables
load_env_file

# Change to project root
cd "$PROJECT_ROOT" || exit 1

# Get Docker directory
if [ -z "$DOCKER_DIR" ]; then
  DOCKER_DIR="$PROJECT_ROOT/docker"
fi

# Print configuration
if [ "$VERBOSE" = true ] || [ "$DRY_RUN" = true ]; then
  echo -e "${CYAN}Configuration:${NC}"
  echo -e "  ${CYAN}Mode:${NC} $MODE"
  echo -e "  ${CYAN}Use Docker:${NC} $USE_DOCKER"
  echo -e "  ${CYAN}Use Existing DB:${NC} $USE_EXISTING_DB"
  echo -e "  ${CYAN}Skip DB Wait:${NC} $SKIP_DB_WAIT"
  echo -e "  ${CYAN}Verbose:${NC} $VERBOSE"
  echo -e "  ${CYAN}Debug:${NC} $DEBUG"
  echo -e "  ${CYAN}Dry Run:${NC} $DRY_RUN"
  echo -e "  ${CYAN}Project Root:${NC} $PROJECT_ROOT"
  echo -e "  ${CYAN}Docker Directory:${NC} $DOCKER_DIR"
  echo -e ""
fi

# Function to set up local development with Docker PostgreSQL
setup_docker_postgres() {
  if [ "$USE_DOCKER" = false ]; then
    echo -e "${YELLOW}Skipping Docker PostgreSQL setup as requested.${NC}"
    return 0
  fi

  # Check Docker status
  if ! check_docker; then
    echo -e "${RED}Error: Docker is required for local development.${NC}"
    echo -e "${YELLOW}Please start Docker and try again, or use --no-docker option.${NC}"
    exit 1
  fi

  # Configure database for PostgreSQL
  configure_local_db true

  # Handle existing database option
  if [ "$USE_EXISTING_DB" = true ]; then
    if is_postgres_running; then
      echo -e "${GREEN}Using existing PostgreSQL database as requested.${NC}"
      return 0
    else
      echo -e "${YELLOW}Warning: --use-existing-db option specified but no running PostgreSQL found.${NC}"
      echo -e "${YELLOW}Will attempt to start PostgreSQL.${NC}"
    fi
  fi

  # Start PostgreSQL
  if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN: Would start PostgreSQL with Docker Compose${NC}"
  else
    # Skip waiting if requested
    if [ "$SKIP_DB_WAIT" = true ]; then
      echo -e "${YELLOW}Starting PostgreSQL without waiting (as requested)...${NC}"
      docker-compose -f "$DOCKER_DIR/docker-compose.yaml" up -d postgres 2>/dev/null || true
      
      # If we have an existing container but it's not running, try to start it
      if ! is_postgres_running; then
        container_id=$(docker ps -a --filter "name=hiresync-postgres" --format "{{.ID}}")
        if [ -n "$container_id" ]; then
          echo -e "${YELLOW}Found existing container. Starting it...${NC}"
          docker start $container_id
        fi
      fi
      
      echo -e "${YELLOW}Skipping database readiness check. Application may fail if database is not ready.${NC}"
    else
      if ! start_postgres; then
        echo -e "${RED}Failed to start PostgreSQL. Cannot proceed with local development.${NC}"
        echo -e "${YELLOW}You can try using the --use-existing-db option if the database is already running,${NC}"
        echo -e "${YELLOW}or --skip-db-wait to proceed without waiting for the database.${NC}"
        exit 1
      fi
    fi
  fi
}

# Function to create application configuration files
create_application_config() {
  local profile=$1
  local config_dir="src/main/resources"
  local config_file="$config_dir/application-$profile.yaml"

  mkdir -p "$config_dir"

  if [ -f "$config_file" ]; then
    echo -e "${GREEN}Using existing configuration: $config_file${NC}"
  else
    echo -e "${YELLOW}Creating $profile configuration file: $config_file${NC}"
    
    if [ "$DRY_RUN" = true ]; then
      echo -e "${YELLOW}DRY RUN: Would create $config_file${NC}"
    else
      case "$profile" in
        "local")
          create_local_config "$config_file"
          ;;
        "dev")
          create_dev_config "$config_file"
          ;;
        "test")
          create_test_config "$config_file"
          ;;
      esac
    fi
  fi
}

# Function to create local configuration
create_local_config() {
  local config_file="$1"
  cat > "$config_file" << 'EOF'
# Configuration for local development with PostgreSQL in Docker
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5544}/${DB_NAME:hiresync_db}
    username: ${DB_USERNAME:hiresync_user}
    password: ${DB_PASSWORD:hiresync_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        connection:
          provider_disables_autocommit: false

# Configure logging for local development
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
    com.zbib.hiresync: DEBUG

# Enable Swagger UI for local development
springdoc:
  swagger-ui:
    enabled: true
EOF
}

# Function to create dev configuration
create_dev_config() {
  local config_file="$1"
  cat > "$config_file" << 'EOF'
# Configuration for development environment
spring:
  datasource:
    url: ${DEV_JDBC_DATABASE_URL:jdbc:postgresql://localhost:5544/hiresync_db}
    username: ${DEV_JDBC_DATABASE_USERNAME:hiresync_user}
    password: ${DEV_JDBC_DATABASE_PASSWORD:hiresync_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        connection:
          provider_disables_autocommit: false

# Configure logging for development
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
    com.zbib.hiresync: DEBUG

# Enable Swagger UI for development
springdoc:
  swagger-ui:
    enabled: true
EOF
}

# Function to create test configuration
create_test_config() {
  local config_file="$1"
  cat > "$config_file" << 'EOF'
# H2 in-memory database configuration for testing
spring:
  datasource:
    url: jdbc:h2:mem:hiresync_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
      settings:
        web-allow-others: false

# Configure logging for testing
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
    com.zbib.hiresync: DEBUG

# Enable Swagger UI for testing
springdoc:
  swagger-ui:
    enabled: true
EOF
}

# Function to start the application
start_application() {
  local profile=$1
  local debug_opts=""

  # Set Spring active profile
  export SPRING_PROFILES_ACTIVE="$profile"

  # Set debug options if requested
  if [ "$DEBUG" = true ]; then
    debug_opts="-Dspring-boot.run.jvmArguments=\"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005\""
  fi

  # Print application access information
  echo -e "${GREEN}Starting application with $profile profile...${NC}"
  echo -e "${YELLOW}API will be available at: http://localhost:8080/api${NC}"
  echo -e "${YELLOW}Swagger UI will be available at: http://localhost:8080/api/swagger-ui.html${NC}"
  
  if [ "$DEBUG" = true ]; then
    echo -e "${YELLOW}Debug port available at: 5005${NC}"
  fi
  
  echo -e "${YELLOW}Press Ctrl+C to stop the application${NC}"

  # Execute the run command
  if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN: Would execute: ./mvnw spring-boot:run -Dspring-boot.run.profiles=$profile $debug_opts${NC}"
  else
    # Check for Maven wrapper in both current directory and project root
    # Try relative path first (for when executed from a subdirectory)
    local mvnw_path=""
    local mvn_command="mvn"
    
    if [ -f "./mvnw" ]; then
      mvnw_path="./mvnw"
    elif [ -f "$PROJECT_ROOT/mvnw" ]; then
      mvnw_path="$PROJECT_ROOT/mvnw"
    fi
    
    # If mvnw exists, make it executable and use it
    if [ -n "$mvnw_path" ]; then
      # Make it executable - handle different OS cases
      case "$(uname -s)" in
        Linux*|Darwin*|CYGWIN*|MINGW*|MSYS*)
          chmod +x "$mvnw_path" 2>/dev/null || true
          ;;
      esac
      
      # Use the detected mvnw path
      mvn_command="$mvnw_path"
      echo -e "${GREEN}Using Maven wrapper at $mvnw_path${NC}"
    else
      echo -e "${YELLOW}Maven wrapper (mvnw) not found. Using 'mvn' command instead...${NC}"
      # Check if mvn is available
      if ! command -v mvn >/dev/null 2>&1; then
        echo -e "${RED}Error: Neither Maven wrapper nor 'mvn' command is available.${NC}"
        echo -e "${RED}Please install Maven or generate a Maven wrapper using:${NC}"
        echo -e "${YELLOW}  mvn -N io.takari:maven:wrapper${NC}"
        exit 1
      fi
    fi
    
    # Run with appropriate command and options
    if [ "$DEBUG" = true ]; then
      $mvn_command spring-boot:run -Dspring-boot.run.profiles="$profile" -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
    else
      $mvn_command spring-boot:run -Dspring-boot.run.profiles="$profile"
    fi
  fi
}

# Main execution flow
echo -e "${GREEN}Starting HireSync in $MODE mode...${NC}"

# Set up Docker PostgreSQL for local mode
if [ "$MODE" = "local" ]; then
  setup_docker_postgres
fi

# Create or verify configuration files
create_application_config "$MODE"

# Start the application
start_application "$MODE"

# Exit with the status of the last command
exit $? 