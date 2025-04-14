#!/usr/bin/env bash
set -e

# Get the absolute path of the script directory using Windows paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/scripts" && pwd)"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.local.yaml"
DEVTOOLS_CONTAINER="hiresync-devtools"

# Define colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Load environment variables
if [ -f "$PROJECT_ROOT/.env" ]; then
  export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Function to display help message
show_help() {
  echo -e "${BLUE}${BOLD}HireSync - Developer Toolkit${NC}"
  echo -e "A toolkit for managing the HireSync application development lifecycle\n"
  echo -e "Usage: ./hiresync COMMAND [OPTIONS]"
  echo -e "Example: ./hiresync start\n"
  echo -e "${BOLD}Available commands:${NC}"
  echo -e "  ${BOLD}start${NC}       - Start the application in Docker"
  echo -e "  ${BOLD}stop${NC}        - Stop Docker containers"
  echo -e "  ${BOLD}restart${NC}     - Restart Docker containers and application"
  echo -e "  ${BOLD}logs${NC}        - View application logs"
  echo -e "  ${BOLD}shell${NC}       - Open a shell in the dev container"
  echo -e "  ${BOLD}init${NC}        - Initialize project structure"
  echo -e "  ${BOLD}quality${NC}     - Run code quality checks"
  echo -e "  ${BOLD}test${NC}        - Run tests (unit, integration)"
  echo -e "  ${BOLD}build${NC}       - Build the application"
  echo -e "  ${BOLD}deploy${NC}      - Deploy the application"
  echo -e "  ${BOLD}db${NC}          - Database operations (migrate, reset, seed)"
  echo -e "  ${BOLD}version${NC}     - Display version information"
  echo -e "  ${BOLD}help${NC}        - Display this help message\n"
  echo -e "For command-specific help, use: ./hiresync COMMAND --help"
}

# Function to check if a script exists
script_exists() {
  [ -f "$SCRIPT_DIR/$1.sh" ]
}

# Function to check if Docker is running
check_docker() {
  if ! docker info &>/dev/null; then
    echo -e "${RED}${BOLD}Error: Docker is not running.${NC}"
    echo -e "Please start Docker Desktop and try again."
    exit 1
  fi
}

# Function to check if container exists
container_exists() {
  docker ps -a | grep -q "$DEVTOOLS_CONTAINER"
}

# Function to check if container is running
container_running() {
  docker ps | grep -q "$DEVTOOLS_CONTAINER"
}

# Function to start containers
start_containers() {
  echo -e "${BLUE}Starting Docker containers...${NC}"
  docker compose -f "$COMPOSE_FILE" up -d --quiet-pull
  
  # Wait for containers to be ready
  echo -e "${BLUE}Waiting for containers to be ready...${NC}"
  sleep 3
  
  if ! container_running; then
    echo -e "${RED}${BOLD}Error: Container $DEVTOOLS_CONTAINER failed to start.${NC}"
    docker compose -f "$COMPOSE_FILE" logs
    exit 1
  fi
  
  echo -e "${GREEN}Docker containers are ready.${NC}"
}

# Function to stop containers
stop_containers() {
  echo -e "${BLUE}Stopping Docker containers...${NC}"
  docker compose -f "$COMPOSE_FILE" down
  echo -e "${GREEN}Docker containers stopped.${NC}"
}

# Function to initialize project structure
initialize_project() {
  echo -e "${BLUE}Initializing project structure...${NC}"
  
  # Create basic project structure if it doesn't exist
  mkdir -p src/main/java/com/zbib/hiresync
  mkdir -p src/main/resources
  
  # Create pom.xml if it doesn't exist
  if [ ! -f "pom.xml" ] && [ -f "docker/pom.xml.template" ]; then
    echo -e "${YELLOW}Creating pom.xml from template...${NC}"
    cp docker/pom.xml.template pom.xml
  fi
  
  # Create main application class if it doesn't exist
  MAIN_CLASS="src/main/java/com/zbib/hiresync/HireSyncApplication.java"
  if [ ! -f "$MAIN_CLASS" ]; then
    echo -e "${YELLOW}Creating main application class...${NC}"
    mkdir -p "$(dirname "$MAIN_CLASS")"
    cat > "$MAIN_CLASS" << 'EOL'
package com.zbib.hiresync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HireSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(HireSyncApplication.class, args);
    }
}
EOL
  fi
  
  # Create application.properties if it doesn't exist
  APP_PROPERTIES="src/main/resources/application.properties"
  if [ ! -f "$APP_PROPERTIES" ]; then
    echo -e "${YELLOW}Creating application.properties...${NC}"
    mkdir -p "$(dirname "$APP_PROPERTIES")"
    cat > "$APP_PROPERTIES" << 'EOL'
# Database Configuration
spring.datasource.url=jdbc:postgresql://postgres:5432/hiresync
spring.datasource.username=hiresync
spring.datasource.password=hiresync
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Server Configuration
server.port=8080
server.servlet.context-path=/api

# Logging Configuration
logging.level.root=INFO
logging.level.com.zbib.hiresync=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate=ERROR
EOL
  fi
  
  echo -e "${GREEN}Project structure initialized.${NC}"
}

# Get command from first argument
COMMAND="$1"
shift || true

# Show usage if no command provided
if [ -z "$COMMAND" ] || [ "$COMMAND" == "help" ] || [ "$COMMAND" == "--help" ]; then
  show_help
  exit 0
fi

# Version command
if [ "$COMMAND" == "version" ]; then
  if [ -f "$PROJECT_ROOT/pom.xml" ]; then
    VERSION=$(grep -m 1 "<version>" "$PROJECT_ROOT/pom.xml" 2>/dev/null | sed 's/<[^>]*>//g' | tr -d ' \t\n\r' || echo "Unknown")
  else
    VERSION="0.0.1-SNAPSHOT (from template)"
  fi
  echo -e "${BLUE}${BOLD}HireSync${NC} version ${GREEN}${VERSION}${NC}"
  exit 0
fi

# Route to appropriate script or command
case "$COMMAND" in
  init)
    initialize_project
    ;;
    
  start)
    check_docker
    
    # Initialize project if needed
    if [ ! -f "pom.xml" ]; then
      echo -e "${YELLOW}Project not initialized. Initializing...${NC}"
      initialize_project
    fi
    
    # Start containers if not already running
    if ! container_running; then
      start_containers
    fi
    
    # Execute the start script
    if script_exists "start"; then
      bash "$SCRIPT_DIR/start.sh" "$@"
    else
      echo -e "${RED}Error: Start script not found at $SCRIPT_DIR/start.sh${NC}"
      exit 1
    fi
    ;;
    
  stop)
    check_docker
    if container_exists; then
      stop_containers
    else
      echo -e "${YELLOW}No containers running.${NC}"
    fi
    ;;
    
  restart)
    check_docker
    if container_exists; then
      stop_containers
    fi
    start_containers
    if script_exists "start"; then
      bash "$SCRIPT_DIR/start.sh" "$@"
    else
      echo -e "${RED}Error: Start script not found at $SCRIPT_DIR/start.sh${NC}"
      exit 1
    fi
    ;;
    
  logs)
    check_docker
    if ! container_running; then
      echo -e "${RED}Error: Containers are not running.${NC}"
      exit 1
    fi
    
    echo -e "${BLUE}Showing logs (Ctrl+C to exit)...${NC}"
    docker compose -f "$COMPOSE_FILE" logs -f
    ;;
    
  shell)
    check_docker
    if ! container_running; then
      echo -e "${YELLOW}Containers are not running. Starting them...${NC}"
      start_containers
    fi
    
    echo -e "${BLUE}Opening shell in $DEVTOOLS_CONTAINER...${NC}"
    docker exec -it "$DEVTOOLS_CONTAINER" bash
    ;;
    
  quality)
    check_docker
    if ! container_running; then
      echo -e "${YELLOW}Containers are not running. Starting them...${NC}"
      start_containers
    fi
    
    if script_exists "quality-check"; then
      bash "$SCRIPT_DIR/quality-check.sh" "$@"
    else
      echo -e "${YELLOW}Running quality checks in container...${NC}"
      docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /app && mvn verify -Dskip.tests=true"
    fi
    ;;
    
  test)
    check_docker
    if ! container_running; then
      echo -e "${YELLOW}Containers are not running. Starting them...${NC}"
      start_containers
    fi
    
    if script_exists "test"; then
      bash "$SCRIPT_DIR/test.sh" "$@"
    else
      echo -e "${YELLOW}Running tests in container...${NC}"
      docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /app && mvn test"
    fi
    ;;
    
  build)
    check_docker
    if ! container_running; then
      echo -e "${YELLOW}Containers are not running. Starting them...${NC}"
      start_containers
    fi
    
    if script_exists "build"; then
      bash "$SCRIPT_DIR/build.sh" "$@"
    else
      echo -e "${YELLOW}Building application in container...${NC}"
      docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /app && mvn clean package -DskipTests"
    fi
    ;;
    
  db)
    if [ -z "$1" ]; then
      echo -e "${RED}Error: Missing database operation${NC}"
      echo -e "Available operations: migrate, reset, seed"
      echo -e "Usage: ./hiresync db OPERATION"
      exit 1
    fi
    
    DB_OPERATION="$1"
    shift
    
    if script_exists "db-$DB_OPERATION"; then
      bash "$SCRIPT_DIR/db-$DB_OPERATION.sh" "$@"
    else
      echo -e "${RED}Error: Unknown database operation: $DB_OPERATION${NC}"
      echo -e "Available operations: migrate, reset, seed"
      exit 1
    fi
    ;;
    
  *)
    if script_exists "$COMMAND"; then
      bash "$SCRIPT_DIR/${COMMAND}.sh" "$@"
    else
      echo -e "${RED}${BOLD}Error: Unknown command: $COMMAND${NC}"
      echo -e "Run ${YELLOW}./hiresync help${NC} to see available commands."
      exit 1
    fi
    ;;
esac