#!/bin/bash
# Server management utilities for HireSync

# Source database utilities if not already sourced
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="$(cd "${SCRIPT_DIR}/../core" && pwd)"
source "${CORE_DIR}/env.sh"
DB_DIR="$(cd "${SCRIPT_DIR}/../db" && pwd)"
source "${DB_DIR}/postgres.sh"

# Check Java installation
check_java() {
  log_debug "Checking Java installation"
  
  # Check if Java is installed
  if ! command -v java &> /dev/null; then
    log_error "Java is not installed or not in PATH"
    log_info "Please install Java from https://adoptium.net/ and try again"
    return 1
  fi
  
  # Check Java version
  local java_version
  java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
  log_debug "Java version: $java_version"
  
  # Look for JAVA_HOME
  if [ -z "$JAVA_HOME" ]; then
    log_warn "JAVA_HOME environment variable is not set"
    log_info "Setting JAVA_HOME may be required for some Maven operations"
  else
    log_debug "JAVA_HOME: $JAVA_HOME"
  fi
  
  return 0
}

# Check Maven installation
check_maven() {
  log_debug "Checking Maven installation"
  
  # Check for Maven wrapper in project root
  if [ -f "$PROJECT_ROOT/mvnw" ]; then
    # Make it executable - handle different OS cases
    chmod +x "$PROJECT_ROOT/mvnw" 2>/dev/null || true
    log_debug "Using Maven Wrapper at $PROJECT_ROOT/mvnw"
    return 0
  fi
  
  # Check for Maven command
  if command -v mvn &> /dev/null; then
    local mvn_version
    mvn_version=$(mvn --version | head -n 1)
    log_debug "Maven found: $mvn_version"
    return 0
  fi
  
  log_error "Maven is not installed and Maven Wrapper (mvnw) is not available"
  log_info "Please install Maven or generate a Maven wrapper in your project"
  return 1
}

# Create necessary application configuration
create_application_config() {
  local profile="$1"
  
  log_debug "Creating application configuration for profile: $profile"
  
  # Create config directory if it doesn't exist
  local config_dir="$PROJECT_ROOT/src/main/resources"
  mkdir -p "$config_dir"
  
  # Create main application.yaml if it doesn't exist
  local app_config="$config_dir/application.yaml"
  if [ ! -f "$app_config" ]; then
    log_info "Creating application.yaml with default profiles"
    cat > "$app_config" << EOF
spring:
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:dev}

# Common configuration shared by all profiles
server:
  port: \${SERVER_PORT:8080}
  servlet:
    context-path: /api
EOF
  fi
  
  # Create profile configuration if it doesn't exist
  local profile_config="$config_dir/application-$profile.yaml"
  if [ ! -f "$profile_config" ]; then
    log_info "Creating configuration for profile: $profile"
    
    case "$profile" in
      "dev"|"local")
        create_dev_config "$profile_config"
        ;;
      "prod")
        create_prod_config "$profile_config"
        ;;
      "test")
        create_test_config "$profile_config"
        ;;
      *)
        log_warn "Unknown profile: $profile. Using development configuration."
        create_dev_config "$profile_config"
        ;;
    esac
  fi
}

# Create development profile configuration
create_dev_config() {
  local config_file="$1"
  
  cat > "$config_file" << EOF
# Development Profile Configuration
spring:
  datasource:
    url: jdbc:postgresql://\${DB_HOST:localhost}:\${DB_PORT:5432}/\${DB_NAME:hiresync_db}
    username: \${DB_USERNAME:postgres}
    password: \${DB_PASSWORD:postgres}
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
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Development Logging Configuration
logging:
  level:
    root: INFO
    com.zbib.hiresync: DEBUG
    org.hibernate.SQL: DEBUG
    
# Enable Swagger UI for development
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
EOF
}

# Create production profile configuration
create_prod_config() {
  local config_file="$1"
  
  cat > "$config_file" << EOF
# Production Profile Configuration
spring:
  datasource:
    url: jdbc:postgresql://\${DB_HOST:localhost}:\${DB_PORT:5432}/\${DB_NAME:hiresync_db}
    username: \${DB_USERNAME:postgres}
    password: \${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Production Logging Configuration
logging:
  level:
    root: WARN
    com.zbib.hiresync: INFO
    
# Disable Swagger UI in production by default
springdoc:
  swagger-ui:
    enabled: \${ENABLE_SWAGGER:false}
    path: /swagger-ui.html
EOF
}

# Create test profile configuration
create_test_config() {
  local config_file="$1"
  
  cat > "$config_file" << EOF
# Test Profile Configuration
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

# Test Logging Configuration
logging:
  level:
    root: INFO
    com.zbib.hiresync: DEBUG
    org.hibernate.SQL: DEBUG
    
# Enable Swagger UI for testing
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
EOF
}

# Start Spring Boot application
start_server() {
  local profile="${1:-dev}"
  local debug="${2:-false}"
  local skip_checks="${3:-false}"
  local skip_db="${4:-false}"
  local skip_quality="${5:-true}"
  
  log_step "Starting Spring Boot application with $profile profile"
  
  # Export profile for Spring Boot
  export SPRING_PROFILES_ACTIVE="$profile"
  
  # Change to project root
  cd "$PROJECT_ROOT"
  
  # Check Java and Maven if not skipping checks
  if [ "$skip_checks" != "true" ]; then
    check_java || return 1
    check_maven || return 1
  fi
  
  # Start database if not skipping
  if [ "$skip_db" != "true" ]; then
    db_start || return 1
  fi
  
  # Create configuration files
  create_application_config "$profile"
  
  # Build command
  local mvn_cmd
  if [ -f "./mvnw" ]; then
    mvn_cmd="./mvnw"
  else
    mvn_cmd="mvn"
  fi
  
  # Add debug options if debugging
  local debug_opts=""
  if [ "$debug" = "true" ]; then
    debug_opts="-Dspring-boot.run.jvmArguments=\"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005\""
  fi
  
  # Add quality check skipping if requested
  local quality_opts=""
  if [ "$skip_quality" = "true" ]; then
    quality_opts="-Dcheckstyle.skip=true -Denforcer.skip=true -Dspotbugs.skip=true"
  fi
  
  # Print startup information
  log_info "Starting application with profile: $profile"
  log_info "API will be available at: http://localhost:${SERVER_PORT}/api"
  log_info "Swagger UI will be available at: http://localhost:${SERVER_PORT}/api/swagger-ui.html"
  
  if [ "$debug" = "true" ]; then
    log_info "Debug port will be available at: 5005"
  fi
  
  # Start Spring Boot
  log_info "Starting Spring Boot application..."
  if [ "$debug" = "true" ]; then
    $mvn_cmd spring-boot:run -Dspring-boot.run.profiles="$profile" \
      -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" $quality_opts
  else
    $mvn_cmd spring-boot:run -Dspring-boot.run.profiles="$profile" $quality_opts
  fi
  
  local exit_code=$?
  
  if [ $exit_code -ne 0 ]; then
    log_error "Spring Boot application failed to start (exit code: $exit_code)"
    return 1
  else
    log_info "Spring Boot application stopped gracefully"
    return 0
  fi
}

# HireSync Development Server
# Runs the application in development mode with proper configuration

# Source core utilities
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="$(cd "${SCRIPT_DIR}/../core" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

source "${CORE_DIR}/logging.sh"
source "${CORE_DIR}/env.sh"
source "${SCRIPT_DIR}/../utils/db.sh"

# Application settings
readonly APP_JAR="${PROJECT_ROOT}/target/hiresync.jar"
readonly APP_MAIN_CLASS="com.zbib.hiresync.HireSyncApplication"

# Start the development server
start_dev_server() {
  log_step "Starting development server"
  
  # Load environment
  load_env "${PROJECT_ROOT}/.env"
  
  # Check database and start if needed
  if ask_yes_no "Do you want to ensure the database is running?" true; then
    log_info "Checking database status"
    db_status > /dev/null
    local db_status=$?
    
    if [[ $db_status -ne 0 ]]; then
      log_info "Starting database"
      db_start || return 1
    else
      log_info "Database is already running"
    fi
  fi
  
  # Check if we need to build the application
  local build_app=false
  if [[ ! -f "$APP_JAR" ]]; then
    log_warn "Application JAR not found: $APP_JAR"
    if ask_yes_no "Do you want to build the application?" true; then
      build_app=true
    else
      log_error "Cannot start without application JAR"
      return 1
    fi
  elif ask_yes_no "Do you want to rebuild the application?" false; then
    build_app=true
  fi
  
  # Build the application if needed
  if [[ "$build_app" == "true" ]]; then
    log_info "Building application"
    cd "$PROJECT_ROOT"
    
    if [[ ! -f "./mvnw" ]]; then
      log_error "Maven wrapper not found"
      return 1
    fi
    
    chmod +x ./mvnw
    
    if ask_yes_no "Skip tests during build?" true; then
      ./mvnw clean package -DskipTests
    else
      ./mvnw clean package
    fi
    
    if [[ $? -ne 0 ]]; then
      log_error "Build failed"
      return 1
    fi
    
    log_info "Build completed successfully"
  fi
  
  # Check for debugging
  local debug_opts=""
  if ask_yes_no "Do you want to enable remote debugging?" false; then
    local debug_port="${DEBUG_PORT:-5005}"
    debug_opts="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$debug_port"
    log_info "Remote debugging enabled on port $debug_port"
  fi
  
  # Determine JVM options
  local profile="${SPRING_PROFILES_ACTIVE:-dev}"
  local java_opts="${JAVA_OPTS:-}"
  
  # Add default JVM options
  java_opts="$java_opts -XX:+UseG1GC -Xmx512m"
  
  # Start the application
  log_info "Starting application with profile: $profile"
  
  if [[ -f "$APP_JAR" ]]; then
    # Run with JAR file if it exists
    java $debug_opts $java_opts -jar "$APP_JAR" --spring.profiles.active="$profile"
  else
    # Fall back to Maven if JAR doesn't exist
    cd "$PROJECT_ROOT"
    ./mvnw spring-boot:run -Dspring-boot.run.profiles="$profile" \
      -Dspring-boot.run.jvmArguments="$debug_opts $java_opts"
  fi
}

# Show help
show_help() {
  cat << EOF
HireSync Development Server

Usage: ./run.sh dev [options]

Options:
  --debug               Enable remote debugging
  --profile=<profile>   Set active profile (default: dev)
  --build               Force rebuild before starting
  --no-db               Skip database check/start
  --help, -h            Show this help message

Examples:
  ./run.sh dev
  ./run.sh dev --debug --profile=test
EOF
}

# Parse command line arguments
parse_args() {
  # Set defaults
  local debug=false
  local profile="dev"
  local build=false
  local skip_db=false
  
  # Parse arguments
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --debug)
        export DEBUG_ENABLED=true
        export DEBUG_PORT="${2:-5005}"
        shift
        ;;
      --profile=*)
        export SPRING_PROFILES_ACTIVE="${1#*=}"
        shift
        ;;
      --build)
        build=true
        shift
        ;;
      --no-db)
        skip_db=true
        shift
        ;;
      --help|-h)
        show_help
        exit 0
        ;;
      *)
        log_error "Unknown option: $1"
        show_help
        exit 1
        ;;
    esac
  done
  
  # Set environment variables
  export FORCE_BUILD="$build"
  export SKIP_DB="$skip_db"
}

# Main function
main() {
  # Parse command line arguments
  parse_args "$@"
  
  # Start development server
  start_dev_server
  
  return $?
}

# Run main function if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi 