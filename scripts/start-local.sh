#!/bin/bash
# HireSync Local Development Start Command
# Comprehensive local development environment setup and orchestration

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/docker-utils.sh"
source "$(dirname "$0")/app-utils.sh"

# Handle cleanup on exit (Ctrl+C)
cleanup() {
  log_info "Received termination signal"
  log_success "Development server stopped. Services remain running."
  exit 0
}

# Register cleanup function for SIGINT and SIGTERM
trap cleanup SIGINT SIGTERM

# Initialize development directories
init_dev_directories() {
  log_info "Setting up development directories..."
  
  # Create logs directory if it doesn't exist
  mkdir -p "../logs"
  mkdir -p "../target/temp"
  
  log_success "Development directories initialized"
  return 0
}

# Load environment variables for local development
configure_environment() {
  log_info "Configuring local development environment..."
  
  # Create .env file from example if not exists
  if [ ! -f "../.env" ]; then
    if [ -f "../.env.example" ]; then
      cp "../.env.example" "../.env"
      log_warning "Created .env from .env.example - you may need to customize it"
    else
      log_error "No .env or .env.example file found"
      return 1
    fi
  fi
  
  # Load environment variables
  set -a
  source "../.env"
  set +a
  
  # Set development-specific environment variables
  export SPRING_PROFILES_ACTIVE="dev,local"
  export LOGGING_LEVEL_ROOT="INFO"
  export LOGGING_LEVEL_COM_HIRESYNC="DEBUG"
  export SPRING_JPA_SHOW_SQL="true"
  export SPRING_DEVTOOLS_RESTART_ENABLED="true"
  export SPRING_DEVTOOLS_LIVERELOAD_ENABLED="true"
  
  log_success "Environment configured for local development"
  return 0
}

# Check all prerequisites
check_prerequisites() {
  log_info "Checking prerequisites..."
  
  # Check Docker
  if ! check_docker; then
    log_error "Docker must be running to start services"
    return 1
  fi
  
  # Check Maven
  if ! command -v mvn &> /dev/null; then
    log_error "Maven is required for development mode"
    return 1
  fi
  
  # Check Java version
  if ! command -v java &> /dev/null; then
    log_error "Java is required but not found"
    return 1
  fi
  
  java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
  log_info "Found Java version: $java_version"
  
  log_success "All prerequisites satisfied"
  return 0
}

# Set up network and volumes for development
setup_dev_infrastructure() {
  log_info "Setting up development infrastructure..."
  
  if ! setup_docker_infrastructure; then
    log_error "Failed to set up Docker infrastructure"
    return 1
  fi
  
  # Create development-specific volume mount points
  if ! docker volume inspect hiresync_dev_data &> /dev/null; then
    docker volume create hiresync_dev_data
    log_info "Created development data volume"
  fi
  
  log_success "Development infrastructure ready"
  return 0
}

# Start all required services in the correct order
start_dev_services() {
  log_info "Starting development services..."
  
  # Pull latest images if needed
  if [ "$PULL_LATEST" = "true" ]; then
    log_info "Pulling latest service images..."
    docker-compose -f ../docker/docker-compose.dev.yml pull
  fi
  
  # Start services with local development overrides
  if ! docker-compose -f ../docker/docker-compose.yml -f ../docker/docker-compose.dev.yml up -d; then
    log_error "Failed to start services with docker-compose"
    return 1
  fi
  
  # Wait for services to be healthy
  local max_attempts=30
  local attempt=0
  log_info "Waiting for services to be ready..."
  
  # Wait for database
  while ! docker exec $(get_container_id "postgres") pg_isready -U postgres &> /dev/null; do
    attempt=$((attempt+1))
    if [ $attempt -ge $max_attempts ]; then
      log_error "Database failed to become ready in time"
      return 1
    fi
    log_info "Waiting for database to be ready... ($attempt/$max_attempts)"
    sleep 2
  done
  
  log_success "All services started and healthy"
  return 0
}

# Run database migrations if needed
run_migrations() {
  log_info "Checking for pending database migrations..."
  
  # Using Flyway directly through Maven
  if ! mvn -f ../pom.xml flyway:info -Dflyway.configFiles=../config/flyway.conf -q; then
    log_warning "Could not check migration status"
  fi
  
  if [ "$AUTO_MIGRATE" = "true" ]; then
    log_info "Running database migrations..."
    if ! mvn -f ../pom.xml flyway:migrate -Dflyway.configFiles=../config/flyway.conf; then
      log_warning "Database migration failed"
      return 1
    fi
    log_success "Database migrations completed"
  else
    log_info "Skipping auto-migration (AUTO_MIGRATE not set to true)"
  fi
  
  return 0
}

# Seed development data if needed
seed_dev_data() {
  if [ "$SEED_DEV_DATA" = "true" ]; then
    log_info "Seeding development data..."
    if ! mvn -f ../pom.xml exec:java -Dexec.mainClass="com.hiresync.tools.DevDataSeeder" -Dexec.classpathScope=test; then
      log_warning "Development data seeding failed"
      return 1
    fi
    log_success "Development data seeded successfully"
  else
    log_info "Skipping development data seeding (SEED_DEV_DATA not set to true)"
  fi
  
  return 0
}

# Configure development-specific logging
configure_logging() {
  log_info "Configuring development logging..."
  
  # Ensure log directory exists
  mkdir -p "../logs"
  
  # Symlink logs for easy access
  if [ -d "/var/log/hiresync" ]; then
    if [ ! -L "../logs/service-logs" ]; then
      ln -sf "/var/log/hiresync" "../logs/service-logs"
    fi
  fi
  
  log_success "Logging configured for development"
  return 0
}

# Setup port forwarding and host entries if needed
setup_networking() {
  log_info "Setting up local networking..."
  
  # Check if ports are already in use
  local required_ports=(8080 5432 27017 6379)
  local port_conflicts=0
  
  for port in "${required_ports[@]}"; do
    if lsof -i:"$port" > /dev/null; then
      log_warning "Port $port is already in use! This may cause conflicts."
      port_conflicts=$((port_conflicts+1))
    fi
  done
  
  if [ $port_conflicts -gt 0 ]; then
    log_warning "$port_conflicts port conflicts detected. You may need to stop other services."
  else
    log_success "No port conflicts detected"
  fi
  
  return 0
}

# Start the application with hot-reloading for local development
start_dev_application() {
  log_info "Starting application in development mode..."
  
  # Set development JVM arguments
  local dev_jvm_args=(
    "-Dspring.profiles.active=dev,local"
    "-Dspring.devtools.restart.enabled=true"
    "-Dspring.devtools.livereload.enabled=true"
    "-Dserver.port=8080"
    "-Xms256m"
    "-Xmx1g"
    "-XX:+UseG1GC"
    "-Dcom.sun.management.jmxremote"
    "-Dspring.jmx.enabled=true"
    "-XX:+HeapDumpOnOutOfMemoryError"
    "-XX:HeapDumpPath=../logs/"
  )
  
  local jvm_args_string=$(IFS=" "; echo "${dev_jvm_args[*]}")
  
  # Change to project root directory
  cd ..
  
  # Start with Spring Boot DevTools for hot reloading
  log_warning "Hot reloading is active - Java file changes will trigger automatic restart"
  
  mvn spring-boot:run -Dspring-boot.run.jvmArguments="$jvm_args_string"
  
  return $?
}

# Setup file watchers for non-Java files (if available)
setup_file_watchers() {
  if command -v watchman &> /dev/null; then
    log_info "Setting up file watchers for non-Java files..."
    
    # Add your watchman config here for non-Java files
    # This is just a placeholder as the specific implementation depends on the project
    watchman watch-del-all &> /dev/null || true
    watchman watch-project ../src &> /dev/null
    
    log_success "File watchers configured"
  else
    log_info "Watchman not found, skipping file watcher setup"
  fi
  
  return 0
}

# Monitor running services health
setup_health_monitoring() {
  log_info "Setting up health monitoring..."
  
  # Start a background process to monitor service health
  (
    while true; do
      sleep 30
      if ! docker ps | grep -q "postgres"; then
        echo "$(date) - WARNING: Database container is not running!" >> ../logs/monitor.log
      fi
      
      # Add more health checks as needed
      
    done
  ) &
  
  # Save the PID to kill it during cleanup
  MONITOR_PID=$!
  
  log_success "Health monitoring started"
  return 0
}

# Main execution flow
main() {
  log_section "HireSync Local Development Environment"
  
  # Set default values for configuration flags
  export AUTO_MIGRATE=${AUTO_MIGRATE:-"false"}
  export SEED_DEV_DATA=${SEED_DEV_DATA:-"false"}
  export PULL_LATEST=${PULL_LATEST:-"false"}
  
  # Execute all setup steps
  check_prerequisites || exit 1
  init_dev_directories || log_warning "Failed to initialize some directories"
  configure_environment || exit 1
  setup_dev_infrastructure || exit 1
  setup_networking || log_warning "Networking setup had warnings"
  start_dev_services || exit 1
  run_migrations || log_warning "Database migrations had issues"
  seed_dev_data || log_warning "Development data seeding had issues"
  configure_logging || log_warning "Logging configuration had issues"
  setup_file_watchers || log_warning "File watcher setup had issues"
  setup_health_monitoring || log_warning "Health monitoring setup had issues"
  
  log_section "Starting HireSync in development mode"
  
  # Start the application (this will block until the app exits)
  start_dev_application
  
  # Clean up monitoring processes
  if [ -n "$MONITOR_PID" ]; then
    kill $MONITOR_PID &> /dev/null || true
  fi
  
  log_section "Development environment shutdown complete"
  exit 0
}

# Execute main function
main 