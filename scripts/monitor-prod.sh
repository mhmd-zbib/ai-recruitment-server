#!/usr/bin/env bash
set -e

# Production monitoring script for HireSync API
# Checks application health and sends alerts if needed

# Get the absolute path of the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Echo with timestamp
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Load environment variables
ENV_FILE="$PROJECT_ROOT/.env"
if [ -f "$ENV_FILE" ]; then
  export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

# Set default values if not in environment
PORT=${PORT:-8080}
APP_CONTEXT=${APP_CONTEXT:-api}
HEALTH_ENDPOINT=${HEALTH_ENDPOINT:-actuator/health}
ALERT_EMAIL=${ALERT_EMAIL:-"admin@example.com"}

# Full health check URL
HEALTH_URL="http://localhost:$PORT/$APP_CONTEXT/$HEALTH_ENDPOINT"

# Check if the application is running
check_app_running() {
  log "Checking if application containers are running..."
  
  if ! docker ps | grep -q "hiresync"; then
    log "⚠️ Warning: No HireSync containers found running"
    return 1
  else
    log "✅ Application containers are running"
    docker ps | grep "hiresync"
    return 0
  fi
}

# Check application health endpoint
check_health_endpoint() {
  log "Checking application health endpoint: $HEALTH_URL"
  
  # Use curl with timeout to prevent hanging
  HTTP_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$HEALTH_URL")
  
  if [ "$HTTP_RESPONSE" = "200" ]; then
    log "✅ Health check passed: HTTP $HTTP_RESPONSE"
    return 0
  else
    log "⚠️ Warning: Health check failed with HTTP $HTTP_RESPONSE"
    return 1
  fi
}

# Check database connection
check_database() {
  log "Checking database connection..."
  
  # Find the database container
  DB_CONTAINER=$(docker ps | grep postgres | awk '{print $1}')
  
  if [ -z "$DB_CONTAINER" ]; then
    log "⚠️ Warning: Database container not found"
    return 1
  fi
  
  # Check PostgreSQL is accepting connections
  if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
    log "✅ Database is accepting connections"
    return 0
  else
    log "⚠️ Warning: Database is not accepting connections"
    return 1
  fi
}

# Check disk space
check_disk_space() {
  log "Checking disk space..."
  
  # Get disk usage percentage
  DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
  
  if [ "$DISK_USAGE" -gt 90 ]; then
    log "⚠️ Warning: Disk usage is critical: $DISK_USAGE%"
    return 1
  elif [ "$DISK_USAGE" -gt 80 ]; then
    log "⚠️ Warning: Disk usage is high: $DISK_USAGE%"
    return 1
  else
    log "✅ Disk space is sufficient: $DISK_USAGE% used"
    return 0
  fi
}

# Check memory usage
check_memory() {
  log "Checking memory usage..."
  
  # Get memory usage percentage
  MEM_USAGE=$(free | awk '/Mem/{printf("%.0f"), $3/$2*100}')
  
  if [ "$MEM_USAGE" -gt 90 ]; then
    log "⚠️ Warning: Memory usage is critical: $MEM_USAGE%"
    return 1
  elif [ "$MEM_USAGE" -gt 80 ]; then
    log "⚠️ Warning: Memory usage is high: $MEM_USAGE%"
    return 1
  else
    log "✅ Memory usage is normal: $MEM_USAGE%"
    return 0
  fi
}

# Send an alert email (placeholder - implement with your preferred method)
send_alert() {
  ALERT_MESSAGE="$1"
  log "🚨 ALERT: $ALERT_MESSAGE"
  log "Would send email to $ALERT_EMAIL"
  
  # Uncomment and customize to actually send alerts
  # echo "$ALERT_MESSAGE" | mail -s "HireSync Production Alert" "$ALERT_EMAIL"
}

# Run all health checks
run_health_checks() {
  log "Running health checks..."
  FAILED=0
  
  # Run each check and capture results
  check_app_running || FAILED=1
  check_health_endpoint || FAILED=1
  check_database || FAILED=1
  check_disk_space || FAILED=1
  check_memory || FAILED=1
  
  # If any checks failed, send an alert
  if [ "$FAILED" -eq 1 ]; then
    send_alert "One or more health checks failed. Please check the application logs."
  else
    log "✅ All health checks passed"
  fi
}

# Main execution
log "Starting production monitoring for HireSync API"
run_health_checks
log "Monitoring complete"

# Exit with appropriate status code
if [ "$FAILED" -eq 1 ]; then
  exit 1
else
  exit 0
fi 