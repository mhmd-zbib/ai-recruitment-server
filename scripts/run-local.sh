#!/bin/bash
# HireSync Local Development Environment
# This script starts the local development environment with PostgreSQL and Spring Boot

set -e

# Set script directory 
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOG_DIR="$PROJECT_ROOT/logs"
LOG_FILE="$LOG_DIR/hiresync-local-$(date +%Y%m%d-%H%M%S).log"
START_TIME=$(date +%s)

# Create logs directory if it doesn't exist
mkdir -p "$LOG_DIR"

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Logging functions
log() {
  local level=$1
  local message=$2
  local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
  local elapsed=$(( $(date +%s) - START_TIME ))
  echo -e "[$timestamp] [$level] [${elapsed}s] $message" | tee -a "$LOG_FILE"
}

log_info() {
  log "INFO" "${CYAN}$1${NC}"
}

log_warning() {
  log "WARNING" "${YELLOW}$1${NC}"
}

log_error() {
  log "ERROR" "${RED}$1${NC}"
}

log_success() {
  log "SUCCESS" "${GREEN}$1${NC}"
}

log_step() {
  log "STEP" "${BOLD}${BLUE}$1${NC}"
}

log_debug() {
  if [[ "${DEBUG:-false}" == "true" ]]; then
    log "DEBUG" "$1"
  fi
}

# Error handler
handle_error() {
  local exit_code=$?
  log_error "An error occurred (exit code: $exit_code)"
  log_error "Check the log file for details: $LOG_FILE"
  exit $exit_code
}

# Set up trap for error handling
trap handle_error ERR

# Verify environment with check-environment.sh
check_environment() {
  log_step "VERIFYING ENVIRONMENT"
  
  local check_script="$SCRIPT_DIR/check-environment.sh"
  if [ -f "$check_script" ]; then
    log_info "Running environment check script..."
    bash "$check_script"
    
    if [ $? -ne 0 ]; then
      log_warning "Environment check found some issues. Continuing anyway, but you may encounter problems."
    else
      log_success "Environment check passed successfully"
    fi
  else
    log_warning "Environment check script not found at: $check_script"
  fi
}

# Load environment variables
load_env_variables() {
  log_step "LOADING ENVIRONMENT VARIABLES"
  
  local env_file="$PROJECT_ROOT/.env"
  if [ -f "$env_file" ]; then
    log_info "Found .env file at: $env_file"
    # Export all variables from .env file (excluding comments)
    set -a
    # Use grep to filter out comments and empty lines before sourcing
    eval "$(grep -v '^#' "$env_file" | grep -v '^\s*$')"
    set +a
    log_success "Environment variables loaded successfully"
    
    # Set default values for any missing variables
    export DB_HOST=${DB_HOST:-postgres}
    export DB_PORT=${DB_PORT:-5432}
    export DB_NAME=${DB_NAME:-hiresync}
    export DB_USER=${DB_USER:-hiresync}
    export DB_USERNAME=${DB_USERNAME:-$DB_USER}  # Ensure both are set
    export DB_PASSWORD=${DB_PASSWORD:-hiresync}
    export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-local}
    
    # Set default values for port variables
    export APP_PORT=${APP_PORT:-8080}
    export DEBUG_PORT=${DEBUG_PORT:-5006}
    export ACTUATOR_PORT=${ACTUATOR_PORT:-8081}
    export PROMETHEUS_PORT=${PROMETHEUS_PORT:-9090}
    export MINIO_PORT=${MINIO_PORT:-9000}
    
    # JDBC URL construction if not explicitly set
    if [ -z "${JDBC_DATABASE_URL}" ]; then
      export JDBC_DATABASE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
    fi
    export JDBC_DATABASE_USERNAME=${JDBC_DATABASE_USERNAME:-$DB_USERNAME}
    export JDBC_DATABASE_PASSWORD=${JDBC_DATABASE_PASSWORD:-$DB_PASSWORD}
    
    log_debug "Database config: host=$DB_HOST, port=$DB_PORT, name=$DB_NAME, user=$DB_USER"
    log_debug "JDBC URL: $JDBC_DATABASE_URL"
    log_debug "Port configuration: app=$APP_PORT, debug=$DEBUG_PORT, actuator=$ACTUATOR_PORT"
  else
    log_error ".env file not found at: $env_file"
    log_info "Checking for .env.example to create a default .env file..."
    
    if [ -f "$PROJECT_ROOT/.env.example" ]; then
      log_info "Creating default .env file from example..."
      cp "$PROJECT_ROOT/.env.example" "$env_file"
      
      # Load again
      load_env_variables
    else
      log_error "No .env.example file found. Cannot continue."
      exit 1
    fi
  fi
  
  # Create an environment file for Docker Compose to use
  create_docker_env_file
}

# Create a temporary env file for Docker Compose
create_docker_env_file() {
  local docker_env_file="$PROJECT_ROOT/docker/.env.docker"
  log_info "Creating Docker environment file at: $docker_env_file"
  
  # Export all environment variables to the Docker env file
  env | grep -v '^_=' | grep -v '^BASH_' > "$docker_env_file"
  
  log_success "Docker environment file created successfully"
}

# Check if Docker is running
check_docker() {
  log_step "CHECKING DOCKER STATUS"
  
  if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed or not in PATH"
    exit 1
  fi
  
  log_info "Testing Docker connection..."
  if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running. Please start Docker and try again."
    exit 1
  fi
  
  # Get Docker version
  local docker_version=$(docker --version)
  log_success "Docker is running: $docker_version"
  
  # Check Docker Compose
  if ! command -v docker-compose &> /dev/null; then
    log_error "Docker Compose is not installed or not in PATH"
    exit 1
  fi
  
  # Get Docker Compose version
  local compose_version=$(docker-compose --version)
  log_success "Docker Compose is available: $compose_version"
}

# Manage Docker network
setup_docker_network() {
  log_step "SETTING UP DOCKER NETWORK"
  
  local network_name="${NETWORK_NAME:-hiresync-network}"
  
  # Check if network exists
  if docker network inspect "$network_name" &> /dev/null; then
    log_success "Docker network '$network_name' already exists"
  else
    log_info "Creating Docker network '$network_name'..."
    docker network create --driver bridge "$network_name"
    log_success "Docker network created successfully"
  fi
}

# Function to check and manage Docker volumes
setup_docker_volumes() {
  log_step "SETTING UP DOCKER VOLUMES"
  
  # Create postgres data volume if it doesn't exist
  local pg_volume="${POSTGRES_VOLUME:-hiresync-postgres-data}"
  if docker volume inspect "$pg_volume" &> /dev/null; then
    log_success "Docker volume '$pg_volume' already exists"
  else
    log_info "Creating Docker volume '$pg_volume'..."
    docker volume create "$pg_volume"
    log_success "Docker volume '$pg_volume' created successfully"
  fi
  
  # Create Maven repository volume if it doesn't exist
  local maven_volume="${MAVEN_REPO_VOLUME:-hiresync-maven-repo}"
  if docker volume inspect "$maven_volume" &> /dev/null; then
    log_success "Docker volume '$maven_volume' already exists"
  else
    log_info "Creating Docker volume '$maven_volume'..."
    docker volume create "$maven_volume"
    log_success "Docker volume '$maven_volume' created successfully"
  fi
}

# Pull Docker images
pull_docker_images() {
  log_step "PULLING REQUIRED DOCKER IMAGES"
  
  log_info "Pulling PostgreSQL image..."
  docker pull postgres:16-alpine
  log_success "PostgreSQL image pulled successfully"
}

# Start Docker containers
start_docker_containers() {
  log_step "STARTING DOCKER CONTAINERS"
  
  local compose_file="$PROJECT_ROOT/docker/docker-compose.local.yaml"
  log_info "Using docker-compose file: $compose_file"
  
  # Log restart policies
  log_info "Container restart policies:"
  log_info "  - PostgreSQL: ${DB_RESTART_POLICY:-unless-stopped}"
  log_info "  - Development tools: ${DEVTOOLS_RESTART_POLICY:-no}"
  
  # Validate docker-compose file
  log_info "Validating docker-compose file..."
  if ! docker-compose -f "$compose_file" config > /dev/null; then
    log_error "Invalid docker-compose file. Please check the syntax."
    exit 1
  fi
  
  # Force stop any existing containers with the same names to avoid conflicts
  log_info "Checking for existing containers..."
  
  for container in "hiresync-postgres" "hiresync-devtools"; do
    if docker ps -a -q --filter "name=$container" | grep -q .; then
      log_info "Found existing container: $container. Removing it..."
      
      # Stop container if it's running
      if docker ps -q --filter "name=$container" | grep -q .; then
        log_info "Stopping container: $container"
        docker stop "$container" > /dev/null
      fi
      
      # Remove container
      log_info "Removing container: $container"
      docker rm "$container" > /dev/null
    fi
  done
  
  # Start PostgreSQL container
  log_info "Starting PostgreSQL container..."
  if docker-compose -f "$compose_file" --env-file "$PROJECT_ROOT/docker/.env.docker" up -d postgres; then
    log_success "PostgreSQL container started successfully"
  else
    log_error "Failed to start PostgreSQL container. Check docker logs for details."
    docker-compose -f "$compose_file" logs postgres
    exit 1
  fi
  
  # Start Development Tools container
  log_info "Starting Development Tools container with JDK and Maven..."
  if docker-compose -f "$compose_file" --env-file "$PROJECT_ROOT/docker/.env.docker" up -d devtools; then
    log_success "Development Tools container started successfully"
  else
    log_error "Failed to start Development Tools container. Check docker logs for details."
    docker-compose -f "$compose_file" logs devtools
    exit 1
  fi
}

# Wait for PostgreSQL to be ready
wait_for_postgres() {
  log_step "WAITING FOR POSTGRESQL TO BE READY"
  
  local max_attempts=30
  local attempt=0
  local container_name="hiresync-postgres"
  
  log_info "Waiting for PostgreSQL container to start..."
  while [ $attempt -lt $max_attempts ]; do
    if ! docker ps --format '{{.Names}}' | grep -q "$container_name"; then
      log_error "PostgreSQL container is not running"
      docker logs "$container_name"
      exit 1
    fi
    
    log_info "Testing PostgreSQL connection (attempt $((attempt+1))/$max_attempts)..."
    if docker exec "$container_name" pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
      log_success "PostgreSQL is ready and accepting connections"
      
      # Display connection details
      log_info "PostgreSQL Connection Details:"
      log_info "  Host:     ${DB_HOST}"
      log_info "  Port:     ${DB_PORT}"
      log_info "  Database: ${DB_NAME}"
      log_info "  Username: ${DB_USER}"
      return 0
    fi
    
    attempt=$((attempt + 1))
    sleep 2
  done
  
  log_error "PostgreSQL did not become ready within the timeout period ($((max_attempts*2)) seconds)"
  log_error "PostgreSQL container logs:"
  docker logs "$container_name"
  exit 1
}

# Apply database migrations if needed
apply_db_migrations() {
  log_step "CHECKING DATABASE MIGRATIONS"
  
  local db_dir="${DB_SCRIPTS_DIR:-$PROJECT_ROOT/db}"
  
  if [ -d "$db_dir" ]; then
    local sql_count=$(find "$db_dir" -name "*.sql" | wc -l)
    
    if [ "$sql_count" -gt 0 ]; then
      log_info "Found $sql_count SQL scripts in $db_dir"
      log_info "These will be automatically applied by the PostgreSQL container on first startup"
      log_success "Database migrations are set up correctly"
    else
      log_warning "No SQL scripts found in $db_dir"
      log_info "This is normal if your application uses JPA schema generation"
    fi
  else
    log_warning "Database scripts directory not found at $db_dir"
    log_info "This is normal if your application uses JPA schema generation"
  fi
}

# Generate a Spring Boot application properties file with all environment variables
generate_application_properties() {
  log_step "GENERATING APPLICATION PROPERTIES FROM ENVIRONMENT VARIABLES"
  
  local props_file="$PROJECT_ROOT/src/main/resources/application-env.yaml"
  log_info "Generating application properties at: $props_file"
  
  # Start with a header
  echo "# AUTOMATICALLY GENERATED FROM .ENV FILE - DO NOT EDIT MANUALLY" > "$props_file"
  echo "# Generated on: $(date)" >> "$props_file"
  echo "" >> "$props_file"
  
  # Add database configuration
  cat >> "$props_file" << EOF
# Database Configuration
spring:
  datasource:
    url: ${JDBC_DATABASE_URL}
    username: ${JDBC_DATABASE_USERNAME}
    password: ${JDBC_DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_MAX_POOL_SIZE:-10}
      minimum-idle: ${DB_MIN_IDLE:-5}
      connection-timeout: ${DB_CONNECTION_TIMEOUT:-30000}
      idle-timeout: ${DB_IDLE_TIMEOUT:-600000}
      max-lifetime: ${DB_MAX_LIFETIME:-1800000}
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:-update}
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# Server Configuration
server:
  port: ${APP_PORT:-8080}

# Logging Configuration  
logging:
  level:
    root: ${LOG_LEVEL_ROOT:-INFO}
    com.zbib.hiresync: ${LOG_LEVEL_APP:-DEBUG}
    org.hibernate.SQL: ${LOG_LEVEL_SQL:-DEBUG}
    org.hibernate.type.descriptor.sql: ${LOG_LEVEL_SQL_PARAMS:-TRACE}

# Security Configuration
jwt:
  secret: ${JWT_SECRET:-LONG_RANDOM_SECRET_THAT_IS_AT_LEAST_32_CHARACTERS_A1B2C3D4E5F6G7H8}
  expiration: ${JWT_EXPIRATION:-86400000}

# Documentation Configuration
springdoc:
  swagger-ui:
    enabled: ${SWAGGER_UI_ENABLED:-true}
  api-docs:
    enabled: ${SPRINGDOC_ENABLED:-true}
EOF

  log_success "Application properties file generated successfully"
}

# Start the Spring Boot application
start_spring_boot() {
  log_step "STARTING SPRING BOOT APPLICATION"
  
  log_info "Starting the application with Maven from devtools container"
  
  # Check if dev container is running
  if ! docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
    log_error "Development tools container (hiresync-devtools) is not running"
    log_info "Starting Docker Compose stack again..."
    
    local compose_file="$PROJECT_ROOT/docker/docker-compose.local.yaml"
    docker-compose -f "$compose_file" --env-file "$PROJECT_ROOT/docker/.env.docker" up -d devtools
    
    if [ $? -ne 0 ]; then
      log_error "Failed to start development tools container"
      exit 1
    fi
    
    log_success "Development tools container started"
  else
    log_success "Development tools container is running"
  fi
  
  # Generate properties file from environment variables
  generate_application_properties
  
  log_info "Using containerized JDK and Maven to run the application"
  log_info "Profile: ${SPRING_PROFILES_ACTIVE:-local},env"
  
  # Pass all environment variables to the Spring Boot application
  local active_profiles="${SPRING_PROFILES_ACTIVE:-local},env"
  
  # Add Maven flags for better development experience
  local mvn_flags="clean spring-boot:run \
    -Dspring-boot.run.profiles=$active_profiles \
    -Dspring-boot.run.jvmArguments=\"-Xmx1g -XX:+UseG1GC -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT:-5006}\" \
    -Dcheckstyle.skip=true \
    -Dpmd.skip=true \
    -Dspotbugs.skip=true"
  
  # Run Maven command inside the container - detached so our script can exit
  log_info "Executing Maven command in development container (detached):"
  log_info "./mvnw $mvn_flags"
  
  # Prepare a script that passes all environment variables to the Java process
  local env_script="/tmp/hiresync-env-script.sh"
  echo '#!/bin/sh' > "$env_script"
  echo 'cd /workspace' >> "$env_script"
  echo 'chmod +x ./mvnw' >> "$env_script"
  
  # Add all environment variables to the script
  env | grep -v '^_=' | grep -v '^BASH_' | while read -r line; do
    echo "export $line" >> "$env_script"
  done
  
  # Add the Maven command
  echo "./mvnw $mvn_flags" >> "$env_script"
  
  # Copy script to container and execute it
  docker cp "$env_script" hiresync-devtools:/tmp/hiresync-env-script.sh
  docker exec -d hiresync-devtools sh -c "chmod +x /tmp/hiresync-env-script.sh && /tmp/hiresync-env-script.sh"
  
  local exit_code=$?
  if [ $exit_code -ne 0 ]; then
    log_error "Failed to start Spring Boot application (exit code: $exit_code)"
    return 1
  fi
  
  # Wait for application to start
  log_info "Waiting for Spring Boot application to start..."
  
  local max_attempts=30
  local attempt=0
  local app_port=${APP_PORT:-8080}
  
  while [ $attempt -lt $max_attempts ]; do
    log_info "Checking if application is running (attempt $((attempt+1))/$max_attempts)..."
    
    if curl -s http://localhost:$app_port/actuator/health > /dev/null 2>&1; then
      log_success "Application started successfully and is responding to health checks"
      log_info "Access the application at: http://localhost:$app_port/api"
      log_info "View application logs with: docker exec hiresync-devtools tail -f /workspace/logs/spring.log"
      return 0
    fi
    
    attempt=$((attempt + 1))
    sleep 3
  done
  
  log_warning "Application may still be starting. Check logs with: ./hiresync devlogs"
  log_info "Application URL: http://localhost:$app_port/api"
  
  return 0
}

# Parse command line arguments
parse_args() {
  # Process command line arguments
  DEV_ONLY=false
  
  while [[ "$#" -gt 0 ]]; do
    case $1 in
      --dev-only) DEV_ONLY=true; shift ;;
      *) log_error "Unknown parameter: $1"; exit 1 ;;
    esac
  done
}

# Setup logging
setup_logging() {
  log_info "============================================================"
  log_info "${BOLD}STARTING HIRESYNC LOCAL DEVELOPMENT ENVIRONMENT${NC}"
  log_info "============================================================"
  log_info "Date/Time: $(date)"
  log_info "User: $(whoami)"
  log_info "System: $(uname -a)"
  log_info "Project Root: $PROJECT_ROOT"
}

# Check prerequisites
check_prerequisites() {
  # Check environment
  check_environment
  
  # Load environment variables
  load_env_variables
  
  # Check Docker
  check_docker
}

# Main function
main() {
  parse_args "$@"
  setup_logging
  check_prerequisites
  pull_docker_images
  setup_docker_network  # Make sure network exists
  setup_docker_volumes  # Make sure volumes exist
  start_docker_containers
  
  # Wait for PostgreSQL to be ready
  wait_for_postgres
  
  # Apply database migrations if needed
  apply_db_migrations
  
  if [ "$DEV_ONLY" = false ]; then
    start_spring_boot
  else
    log_info "Development environment is ready."
    log_info "You can run Maven commands with: './hiresync maven <command>'"
    log_info "Or open a shell with: './hiresync bash'"
  fi

  log_success "Setup completed successfully!"
}

# Run the main function
main "$@"

# Calculate total execution time - this will only execute if the script completes successfully
END_TIME=$(date +%s)
TOTAL_TIME=$((END_TIME - START_TIME))
log_success "Script execution completed successfully in ${TOTAL_TIME} seconds" 