#!/bin/bash
# HireSync Application Management Utilities
# Contains functions for managing the Spring Boot application

# Load common utilities
source "$(dirname "$0")/common.sh"

# Start the Spring Boot application
start_app() {
  log_header "Starting Spring Boot application..."
  
  # Check if Maven container is running
  if ! docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
    log_error "DevTools container is not running. Please start services first."
    return 1
  fi
  
  # Wait for PostgreSQL to be ready
  log_info "Waiting for PostgreSQL to be ready..."
  local postgres_ready=false
  for i in {1..30}; do
    if docker exec hiresync-postgres pg_isready -h localhost -U "${DB_USER}" -d "${DB_NAME}" > /dev/null 2>&1; then
      log_success "PostgreSQL is ready"
      postgres_ready=true
      break
    fi
    log_info "Waiting for PostgreSQL to start... (${i}/30)"
    sleep 2
  done
  
  if [ "$postgres_ready" != "true" ]; then
    log_error "PostgreSQL did not start in time. Please check the logs."
    return 1
  fi
  
  # Define Maven command for running the app with proper environment variables
  local MVN_CMD="mvn spring-boot:run -Dmaven.main.skip=false"
  MVN_CMD+=" -Dspring-boot.run.profiles=local"
  MVN_CMD+=" -Dspring.profiles.active=local"
  
  # Add explicit datasource properties as JVM arguments
  MVN_CMD+=" -Dspring-boot.run.jvmArguments="
  MVN_CMD+="\"-Dspring.datasource.url=jdbc:postgresql://postgres:5432/${DB_NAME}"
  MVN_CMD+=" -Dspring.datasource.username=${DB_USER}"
  MVN_CMD+=" -Dspring.datasource.password=${DB_PASSWORD}"
  MVN_CMD+=" -Dspring.datasource.driver-class-name=org.postgresql.Driver"
  MVN_CMD+=" -DJWT_SECRET=${JWT_SECRET}"
  MVN_CMD+=" -DJWT_EXPIRATION=${JWT_EXPIRATION}\""
  
  # Skip tests for faster startup
  MVN_CMD+=" -DskipTests=true -Dmaven.test.skip=true -P local"
  
  # Build the environment variable string for docker exec
  local env_vars=""
  
  # Database config - Use 'postgres' as hostname when running in container
  env_vars+=" -e DB_HOST=postgres"
  env_vars+=" -e DB_PORT=5432"
  env_vars+=" -e DB_NAME=${DB_NAME}"
  env_vars+=" -e DB_USER=${DB_USER}"
  env_vars+=" -e DB_PASSWORD=${DB_PASSWORD}"
  
  # Add Spring-specific datasource properties
  env_vars+=" -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${DB_NAME}"
  env_vars+=" -e SPRING_DATASOURCE_USERNAME=${DB_USER}"
  env_vars+=" -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}"
  env_vars+=" -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver"
  
  # JWT config
  env_vars+=" -e JWT_SECRET=\"${JWT_SECRET}\""
  env_vars+=" -e JWT_EXPIRATION=${JWT_EXPIRATION}"
  
  # Application config
  env_vars+=" -e SPRING_PROFILES_ACTIVE=local"
  env_vars+=" -e APP_PORT=8080"
  
  # Logging
  env_vars+=" -e LOG_LEVEL_APP=DEBUG"
  env_vars+=" -e LOG_LEVEL_SQL=DEBUG"
  env_vars+=" -e LOG_LEVEL_SQL_PARAMS=TRACE"
  
  # Time zone
  env_vars+=" -e TZ=${TZ:-UTC}"
  
  # Run Maven inside Docker with environment variables
  if ! run_command "docker exec $env_vars -it hiresync-devtools bash -c \"cd /workspace && $MVN_CMD\"" "Failed to start Spring Boot application"; then
    log_error "Application failed to start. Check the logs for more details."
    return 1
  fi
  
  log_success "Application started successfully with local profile."
  return 0
} 