#!/bin/sh
# HireSync Application Docker Entrypoint
# Handles application startup with proper health checks and graceful shutdown

set -e

# Configuration with default fallbacks
readonly POSTGRES_HOST="${DB_HOST:-postgres}"
readonly POSTGRES_PORT="${DB_PORT:-5432}"
readonly POSTGRES_DB="${DB_NAME:-hiresync_db}"
readonly POSTGRES_USER="${DB_USERNAME:-hiresync_user}"
readonly POSTGRES_PASSWORD="${DB_PASSWORD:-hiresync_password}"
readonly POSTGRES_MAX_ATTEMPTS="${DB_MAX_ATTEMPTS:-30}"
readonly POSTGRES_ATTEMPT_INTERVAL="${DB_ATTEMPT_INTERVAL:-2}"
readonly DEBUG_ENABLED="${DEBUG_ENABLED:-false}"
readonly DEBUG_PORT="${DEBUG_PORT:-5005}"
readonly JVM_OPTS="${JAVA_OPTS:-}"
readonly APP_JAR="/app/app.jar"
readonly HEAP_DUMP_PATH="/app/logs/heapdump.hprof"
readonly GRACEFUL_SHUTDOWN_TIMEOUT=30

# Log with timestamp and level
log() {
  local level="INFO"
  if [ $# -gt 1 ]; then
    level="$1"
    shift
  fi
  echo "$(date -Iseconds) [ENTRYPOINT] [$level] $*"
}

# Error log shorthand
log_error() {
  log "ERROR" "$@"
}

# Warning log shorthand
log_warn() {
  log "WARN" "$@"
}

# Debug log shorthand
log_debug() {
  if [ "$DEBUG_ENABLED" = "true" ]; then
    log "DEBUG" "$@"
  fi
}

# Handle graceful exit
handle_exit() {
  log "Received termination signal, stopping application..."
  
  # Find the Java process
  local pid
  pid=$(pgrep -f "java.*app.jar" || echo "")
  
  if [ -n "$pid" ]; then
    log "Sending graceful termination signal to PID $pid..."
    kill -15 "$pid" 2>/dev/null || true
    
    # Wait for process to terminate gracefully
    local counter=0
    while [ $counter -lt $GRACEFUL_SHUTDOWN_TIMEOUT ]; do
      if ! ps -p "$pid" > /dev/null 2>&1; then
        log "Application terminated gracefully"
        break
      fi
      sleep 1
      counter=$((counter+1))
      
      # Log progress periodically
      if [ $((counter % 5)) -eq 0 ]; then
        log_debug "Still waiting for graceful shutdown... ($counter/$GRACEFUL_SHUTDOWN_TIMEOUT)"
      fi
    done
    
    # Force kill if still running
    if ps -p "$pid" > /dev/null 2>&1; then
      log_warn "Application did not terminate gracefully after ${GRACEFUL_SHUTDOWN_TIMEOUT}s, forcing exit..."
      kill -9 "$pid" 2>/dev/null || true
      sleep 1
    fi
  else
    log_debug "No Java process found to terminate"
  fi
  
  log "Exiting container"
  exit 0
}

# Set up signal handling
trap handle_exit INT TERM

# Check if the application JAR exists
check_jar() {
  if [ ! -f "$APP_JAR" ]; then
    log_error "Application JAR not found at $APP_JAR"
    return 1
  fi
  
  log_debug "Found application JAR: $APP_JAR"
  return 0
}

# Wait for database to be ready
wait_for_db() {
  log "Checking database connection to $POSTGRES_HOST:$POSTGRES_PORT..."
  
  local attempts=0
  local max_attempts=$POSTGRES_MAX_ATTEMPTS
  local interval=$POSTGRES_ATTEMPT_INTERVAL
  
  while [ $attempts -lt $max_attempts ]; do
    if nc -z "$POSTGRES_HOST" "$POSTGRES_PORT" >/dev/null 2>&1; then
      log "Database connection established"
      return 0
    fi
    
    attempts=$((attempts+1))
    
    if [ $attempts -eq $max_attempts ]; then
      log_error "Database connection failed after $attempts attempts"
      return 1
    fi
    
    log "Waiting for database to become available... ($attempts/$max_attempts)"
    sleep "$interval"
  done
  
  log_error "Timed out waiting for database"
  return 1
}

# Get Java runtime options
get_java_opts() {
  local opts=""
  
  # Container optimization
  opts="$opts -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
  
  # Memory management and crash handling
  opts="$opts -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${HEAP_DUMP_PATH}"
  
  # Security options
  opts="$opts -Djava.security.egd=file:/dev/./urandom -Dnetworkaddress.cache.ttl=60"
  
  # Add debug options if enabled
  if [ "$DEBUG_ENABLED" = "true" ]; then
    opts="$opts -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
    log "Remote debugging enabled on port $DEBUG_PORT"
  fi
  
  # Add additional JVM options
  if [ -n "$JVM_OPTS" ]; then
    opts="$opts $JVM_OPTS"
  fi
  
  echo "$opts"
}

# Start the application
start_application() {
  log "Starting HireSync application..."
  
  # Set database connection wait timeout
  if [ "${WAIT_FOR_DB:-true}" = "true" ]; then
    wait_for_db || {
      log_error "Failed to connect to database, exiting"
      exit 1
    }
  else
    log "Skipping database connection check"
  fi
  
  # Set Java options
  local java_opts
  java_opts=$(get_java_opts)
  
  # Start the application
  log "Executing: java $java_opts -jar $APP_JAR $*"
  exec java $java_opts -jar "$APP_JAR" "$@"
}

# Main execution
main() {
  log "HireSync container starting..."
  
  # Check if application JAR exists
  check_jar || {
    log_error "Application JAR check failed, exiting"
    exit 1
  }
  
  # Start the application with remaining arguments
  start_application "$@"
}

# Run the main function with all arguments
main "$@" 