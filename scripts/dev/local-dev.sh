#!/bin/bash

# HireSync Local Development Script for Git Bash
# This is a simplified script for local development

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Get Docker directory
if [ -z "$DOCKER_DIR" ]; then
  DOCKER_DIR="$PROJECT_ROOT/docker"
fi

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Local Development${NC}"
echo -e "${BLUE}========================================${NC}"

# Utility function to check Docker
check_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not installed or not in PATH.${NC}"
    return 1
  fi

  if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running.${NC}"
    return 1
  fi

  echo -e "${GREEN}Docker is running.${NC}"
  return 0
}

# Function to load environment variables
load_env() {
  if [ -f "$PROJECT_ROOT/.env" ]; then
    echo -e "${GREEN}Loading environment variables from .env file...${NC}"
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
  else
    echo -e "${YELLOW}Warning: .env file not found, creating from example...${NC}"
    cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
  fi
}

# Main function to start local development
start_local_dev() {
  echo -e "${GREEN}Starting HireSync in local development mode...${NC}"
  
  # Change to project root
  cd "$PROJECT_ROOT" || exit 1
  
  # Check if Docker is running
  if ! check_docker; then
    echo -e "${RED}Error: Docker is required for local development.${NC}"
    echo -e "${YELLOW}Please start Docker Desktop and try again.${NC}"
    exit 1
  fi

  # Load environment variables
  load_env

  # Set active profile
  export SPRING_PROFILES_ACTIVE=local
  
  # Start PostgreSQL with Docker Compose
  echo -e "${BLUE}Starting PostgreSQL with Docker Compose...${NC}"
  docker-compose -f "$DOCKER_DIR/docker-compose.yaml" up -d postgres
  
  # Wait for PostgreSQL to be ready
  echo -e "${YELLOW}Waiting for PostgreSQL to be ready...${NC}"
  RETRIES=30
  until docker-compose -f "$DOCKER_DIR/docker-compose.yaml" exec postgres pg_isready -U hiresync_user -d hiresync_db > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -e "${YELLOW}Waiting for PostgreSQL to be ready... $(( 30 - RETRIES ))/30${NC}"
    RETRIES=$((RETRIES-1))
    sleep 2
  done
  
  if [ $RETRIES -eq 0 ]; then
    echo -e "${RED}Error: PostgreSQL failed to start.${NC}"
    echo -e "${YELLOW}Check docker-compose logs with: docker-compose -f $DOCKER_DIR/docker-compose.yaml logs postgres${NC}"
    exit 1
  fi
  
  echo -e "${GREEN}PostgreSQL is ready!${NC}"
  
  # Create application-local.yaml if it doesn't exist
  mkdir -p src/main/resources
  if [ ! -f src/main/resources/application-local.yaml ]; then
    echo -e "${YELLOW}Creating application-local.yaml configuration...${NC}"
    cat > src/main/resources/application-local.yaml << 'EOF'
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
  fi
  
  # Start the application
  echo -e "${GREEN}Starting Spring Boot application...${NC}"
  echo -e "${YELLOW}API will be available at: http://localhost:8080/api${NC}"
  echo -e "${YELLOW}Swagger UI will be available at: http://localhost:8080/api/swagger-ui.html${NC}"
  echo -e "${YELLOW}Press Ctrl+C to stop the application${NC}"
  
  # Run the application
  if [ -f mvnw ]; then
    chmod +x mvnw
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
  else
    echo -e "${RED}Error: Maven wrapper (mvnw) not found.${NC}"
    echo -e "${YELLOW}Running with 'mvn' instead...${NC}"
    mvn spring-boot:run -Dspring-boot.run.profiles=local
  fi
}

# Call the main function
start_local_dev 