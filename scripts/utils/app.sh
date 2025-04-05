#!/bin/bash
# HireSync Application Utilities
# Common functions for managing the Spring Boot application

# Load dependencies
source "$(dirname "$0")/logging.sh"

# Default app configuration
export JAVA_OPTS="${JAVA_OPTS:-}"
export DEBUG_PORT="${DEBUG_PORT:-5005}"

# Set Maven command with appropriate args
set_maven_cmd() {
  if command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
  elif [ -f "./mvnw" ]; then
    MVN_CMD="./mvnw"
  else
    log_error "Maven not found - cannot run the application"
    return 1
  fi
  
  log_debug "Using Maven command: $MVN_CMD"
  return 0
}

# Run the application using Maven
run_app() {
  local profile="${1:-local}"
  local dev_mode="${2:-false}"
  
  set_maven_cmd || return 1
  
  # Set Java options for debugging if needed
  if [ "${DEBUG_MODE:-false}" = "true" ]; then
    log_info "Enabling remote debugging on port $DEBUG_PORT"
    export JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
  fi
  
  # Set Maven goals based on profile
  local maven_goals="spring-boot:run"
  local maven_args="-Dspring-boot.run.profiles=$profile"
  
  # Use spring-boot:run for development mode
  if [ "$dev_mode" = "true" ]; then
    log_info "Starting application in development mode with hot reload"
    maven_args="$maven_args -Dspring-boot.run.jvmArguments=\"$JAVA_OPTS\""
  else
    log_info "Starting application with profile: $profile"
    maven_args="$maven_args -Dspring-boot.run.jvmArguments=\"$JAVA_OPTS\""
  fi
  
  # Run the application
  log_debug "Running: $MVN_CMD $maven_goals $maven_args"
  $MVN_CMD $maven_goals $maven_args
  
  local status=$?
  if [ $status -ne 0 ]; then
    log_error "Application failed with exit code $status"
    return $status
  fi
  
  return 0
}

# Run database migrations
run_migrations() {
  local profile="${1:-local}"
  
  set_maven_cmd || return 1
  
  log_info "Running database migrations with profile: $profile"
  $MVN_CMD flyway:migrate -P$profile
  
  local status=$?
  if [ $status -ne 0 ]; then
    log_error "Migrations failed with exit code $status"
    return $status
  fi
  
  log_success "Database migrations completed successfully"
  return 0
}

# Package the application
package_app() {
  local profile="${1:-prod}"
  
  set_maven_cmd || return 1
  
  log_info "Packaging application with profile: $profile"
  $MVN_CMD clean package -P$profile -DskipTests
  
  local status=$?
  if [ $status -ne 0 ]; then
    log_error "Packaging failed with exit code $status"
    return $status
  fi
  
  log_success "Application packaged successfully"
  return 0
}

# Run the compiled application
run_jar() {
  local profile="${1:-prod}"
  local jar_file=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -1)
  
  if [ ! -f "$jar_file" ]; then
    log_error "Application JAR file not found. Run 'package_app' first."
    return 1
  fi
  
  log_info "Running application JAR: $jar_file"
  java $JAVA_OPTS -jar "$jar_file" --spring.profiles.active="$profile"
  
  local status=$?
  if [ $status -ne 0 ]; then
    log_error "Application failed with exit code $status"
    return $status
  fi
  
  return 0
}

# Run tests
run_tests() {
  local profile="${1:-test}"
  local test_scope="${2:-}"
  
  set_maven_cmd || return 1
  
  local test_args="-P$profile"
  if [ -n "$test_scope" ]; then
    test_args="$test_args -Dtest=$test_scope"
  fi
  
  log_info "Running tests with profile: $profile"
  log_debug "Test arguments: $test_args"
  
  $MVN_CMD test $test_args
  
  local status=$?
  if [ $status -ne 0 ]; then
    log_error "Tests failed with exit code $status"
    return $status
  fi
  
  log_success "Tests completed successfully"
  return 0
}

# Check application health
check_app_health() {
  local host="${1:-localhost}"
  local port="${2:-8080}"
  local endpoint="${3:-/actuator/health}"
  local max_attempts="${4:-10}"
  local wait_time="${5:-2}"
  
  log_info "Checking application health at http://$host:$port$endpoint"
  
  for i in $(seq 1 $max_attempts); do
    if curl -s "http://$host:$port$endpoint" | grep -q "UP"; then
      log_success "Application is healthy"
      return 0
    fi
    
    log_warning "Attempt $i/$max_attempts: Application not healthy, waiting ${wait_time}s..."
    sleep "$wait_time"
  done
  
  log_error "Application is not healthy after $max_attempts attempts"
  return 1
} 