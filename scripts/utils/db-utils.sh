#!/bin/bash

# Database utility functions for HireSync application
# This script provides common database utilities used by other scripts

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the Docker directory
if [ -z "$DOCKER_DIR" ]; then
  DOCKER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/docker"
fi

# Check if Docker is running
check_docker() {
  if docker info > /dev/null 2>&1; then
    echo -e "${GREEN}Docker is running.${NC}"
    return 0
  else
    echo -e "${RED}Docker is not running.${NC}"
    return 1
  fi
}

# Check if PostgreSQL container is already running
is_postgres_running() {
  if docker ps --format '{{.Names}}' | grep -q 'hiresync-postgres'; then
    echo -e "${GREEN}PostgreSQL container is already running.${NC}"
    return 0
  else
    return 1
  fi
}

# Start the PostgreSQL container
start_postgres() {
  # First check if container is already running
  if is_postgres_running; then
    echo -e "${GREEN}Using existing PostgreSQL container.${NC}"
    
    # Verify the database is accessible
    attempt=0
    max_attempts=5
    echo -e "${YELLOW}Verifying database connection...${NC}"
    until docker exec hiresync-postgres pg_isready -U hiresync_user -d hiresync_db > /dev/null 2>&1 || [ $attempt -eq $max_attempts ]; do
      attempt=$((attempt+1))
      echo -e "${YELLOW}Checking database connection... ($attempt/$max_attempts)${NC}"
      sleep 1
    done
    
    if [ $attempt -lt $max_attempts ]; then
      echo -e "${GREEN}Connection to PostgreSQL verified!${NC}"
      return 0
    else
      echo -e "${YELLOW}Warning: Existing PostgreSQL container doesn't seem responsive.${NC}"
      echo -e "${YELLOW}Attempting to restart the container...${NC}"
      docker restart hiresync-postgres
    fi
  fi

  echo -e "${BLUE}Starting PostgreSQL container...${NC}"
  
  # Try to start the container, but handle error if it already exists
  docker-compose -f "$DOCKER_DIR/docker-compose.yaml" up -d postgres 2>/dev/null || true
  
  # If the container exists but is not running, try to start it directly
  if ! is_postgres_running; then
    container_id=$(docker ps -a --filter "name=hiresync-postgres" --format "{{.ID}}")
    if [ -n "$container_id" ]; then
      echo -e "${YELLOW}Found existing container. Starting it...${NC}"
      docker start $container_id
    fi
  fi
  
  # Wait for the database to be ready
  echo -e "${YELLOW}Waiting for PostgreSQL to be ready...${NC}"
  attempt=0
  max_attempts=30
  until docker exec hiresync-postgres pg_isready -U hiresync_user -d hiresync_db > /dev/null 2>&1 || [ $attempt -eq $max_attempts ]; do
    # If container doesn't exist at all, try using docker-compose exec
    if ! docker ps -a --filter "name=hiresync-postgres" --format "{{.ID}}" | grep -q .; then
      docker-compose -f "$DOCKER_DIR/docker-compose.yaml" exec postgres pg_isready -U hiresync_user -d hiresync_db > /dev/null 2>&1 && break
    fi
    
    attempt=$((attempt+1))
    echo -e "${YELLOW}Waiting for database to be ready... ($attempt/$max_attempts)${NC}"
    sleep 2
  done

  if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}Error: PostgreSQL container did not become ready in time.${NC}"
    echo -e "${YELLOW}Check docker-compose logs with: docker-compose -f $DOCKER_DIR/docker-compose.yaml logs postgres${NC}"
    return 1
  fi

  echo -e "${GREEN}PostgreSQL database is ready!${NC}"
  return 0
}

# Configure database settings for local environment
# Parameters:
#   $1: true if Docker is available, false otherwise
configure_local_db() {
  local docker_available=$1
  local config_file="src/main/resources/application-local.yaml"
  
  if [ "$docker_available" = true ]; then
    echo -e "${BLUE}Configuring for local environment with PostgreSQL...${NC}"
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
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

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
  else
    echo -e "${BLUE}Configuring for local environment with H2 database...${NC}"
    cat > "$config_file" << 'EOF'
# H2 in-memory database configuration for local development
spring:
  datasource:
    url: jdbc:h2:mem:hiresync_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
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
  fi

  echo -e "${GREEN}Database configuration for local environment is set.${NC}"
  return 0
}

# Load environment variables from .env file
load_env_file() {
  local env_file=".env"
  if [ -f "$env_file" ]; then
    echo -e "${YELLOW}Loading environment variables from $env_file${NC}"
    # Export all variables from .env file
    set -a
    source "$env_file"
    set +a
    return 0
  else
    echo -e "${YELLOW}No .env file found. Using default environment variables.${NC}"
    return 1
  fi
}

# Check if required environment variables for production are set
check_prod_env_vars() {
  local required_vars=("JDBC_DATABASE_URL" "JDBC_DATABASE_USERNAME" "JDBC_DATABASE_PASSWORD")
  local missing_vars=()

  for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
      missing_vars+=("$var")
    fi
  done

  if [ ${#missing_vars[@]} -ne 0 ]; then
    echo -e "${RED}Error: The following required environment variables are not set:${NC}"
    for var in "${missing_vars[@]}"; do
      echo -e "  - ${YELLOW}$var${NC}"
    done
    
    echo -e "\n${YELLOW}Set these variables in your environment before running this script.${NC}"
    echo -e "Example:"
    echo -e "  export JDBC_DATABASE_URL=jdbc:postgresql://hostname:port/database"
    echo -e "  export JDBC_DATABASE_USERNAME=username"
    echo -e "  export JDBC_DATABASE_PASSWORD=password"
    
    return 1
  fi

  echo -e "${GREEN}All required environment variables are set.${NC}"
  return 0
} 