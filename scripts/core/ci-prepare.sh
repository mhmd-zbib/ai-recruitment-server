#!/bin/bash

# Description: Prepares the CI environment for running tests and builds without Docker.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities if available
if [ -f "$SCRIPT_DIR/../utils/logging.sh" ]; then
  source "$SCRIPT_DIR/../utils/logging.sh"
else
  # Simple logging functions if the utils aren't available
  log_info() { echo "[INFO] $1"; }
  log_error() { echo "[ERROR] $1"; }
  log_success() { echo "[SUCCESS] $1"; }
  log_section() { echo "===== $1 ====="; }
fi

# Default values
CACHE_DEPS=${CACHE_DEPS:-true}
MVN_ARGS=${MAVEN_ARGS:-"-B -ntp"}

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-cache)
      CACHE_DEPS=false
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync ci-prepare [--no-cache]"
      exit 1
      ;;
  esac
done

log_section "Preparing CI Environment"

# Check Java version
log_info "Java version:"
java -version

# Check Maven version
log_info "Maven version:"
mvn --version

# Setup application properties for CI
if [ ! -f "$PROJECT_ROOT/src/main/resources/application-ci.yaml" ]; then
  log_info "Creating CI-specific application properties..."
  
  cat > "$PROJECT_ROOT/src/main/resources/application-ci.yaml" << EOF
# CI Environment specific properties
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb
    username: hiresync
    password: hiresync
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

logging:
  level:
    root: WARN
    com.zbib.hiresync: INFO

# Actuator settings for CI
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never

# JWT configuration for CI
jwt:
  secret: test-secret-key-with-minimum-length-of-32-characters
  issuer: hiresync-ci
  expiration: 3600000
EOF

  log_success "CI application properties created"
fi

# Create necessary directories for reports and configs
log_info "Creating directories for reports and configs"
mkdir -p "$PROJECT_ROOT/config"
mkdir -p "$PROJECT_ROOT/target/reports"
mkdir -p "$PROJECT_ROOT/target/surefire-reports"
mkdir -p "$PROJECT_ROOT/target/failsafe-reports"

# Check if running in CI environment
if [ -n "$CI" ]; then
  log_info "Running in CI environment"
  
  # Cache Maven dependencies if running in CI
  if [ "$CACHE_DEPS" = true ]; then
    log_info "Caching Maven dependencies..."
    mvn $MVN_ARGS dependency:go-offline
  fi
  
  # Prepare test directories and ensure permissions
  log_info "Setting up test environment..."
  chmod -R 755 "$PROJECT_ROOT/target"
fi

# Format code automatically before validation
log_info "Formatting code with Spotless..."
mvn spotless:apply $MVN_ARGS || log_error "Spotless formatting failed, continuing anyway..."

log_section "CI Environment Ready"
exit 0 