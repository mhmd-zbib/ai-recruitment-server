#!/bin/bash
# HireSync Database Management
# Handles database setup, initialization, and management operations

# Exit on error, treat unset variables as errors, and handle pipefail
set -euo pipefail

# Constants for database connection
readonly DB_HOST="${DB_HOST:-localhost}"
readonly DB_PORT="${DB_PORT:-5432}"
readonly DB_NAME="${DB_NAME:-hiresync_db}"
readonly DB_USERNAME="${DB_USERNAME:-hiresync_user}"
readonly DB_PASSWORD="${DB_PASSWORD:-hiresync_password}"
readonly READ_PASSWORD="${READ_PASSWORD:-read_password}"
readonly WRITE_PASSWORD="${WRITE_PASSWORD:-write_password}"

# Container name and volume
readonly POSTGRES_CONTAINER_NAME="hiresync-postgres"
readonly POSTGRES_VOLUME_NAME="hiresync-postgres-data"
readonly POSTGRES_IMAGE="postgres:16-alpine"

# Status codes for database container
readonly DB_STATUS_RUNNING=0
readonly DB_STATUS_STOPPED=1
readonly DB_STATUS_NOT_FOUND=2

# Import core utilities if not already available
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${SCRIPT_DIR}/core/colors.sh"
source "${SCRIPT_DIR}/core/logging.sh"

# Check if Docker is available
check_docker() {
  if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed or not in PATH"
    return 1
  fi
  
  if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running"
    return 1
  fi
  
  return 0
}

# Check if container exists
container_exists() {
  docker container inspect "$POSTGRES_CONTAINER_NAME" &> /dev/null
}

# Check if container is running
container_running() {
  if ! container_exists; then
    return 1
  fi
  
  local status
  status=$(docker container inspect -f '{{.State.Running}}' "$POSTGRES_CONTAINER_NAME" 2>/dev/null)
  [[ "$status" == "true" ]]
}

# Get database container status
db_status() {
  if ! check_docker; then
    return 3
  fi
  
  if ! container_exists; then
    return $DB_STATUS_NOT_FOUND
  fi
  
  if container_running; then
    return $DB_STATUS_RUNNING
  else
    return $DB_STATUS_STOPPED
  fi
}

# Start database container
db_start() {
  log_step "Starting PostgreSQL database"
  
  # Check if container exists
  if container_exists; then
    # If container exists, just make sure it's running
    if container_running; then
      log_info "PostgreSQL container is already running"
      return 0
    else
      log_info "Starting existing PostgreSQL container"
      if ! docker start "$POSTGRES_CONTAINER_NAME" > /dev/null; then
        log_error "Failed to start PostgreSQL container"
        return 1
      fi
      
      # Wait for database to be ready
      wait_for_db_ready
      return $?
    fi
  fi
  
  # Create container if it doesn't exist
  log_info "Creating new PostgreSQL container"
  
  # Create a Docker volume for persistent data if it doesn't exist
  if ! docker volume inspect "$POSTGRES_VOLUME_NAME" > /dev/null 2>&1; then
    log_debug "Creating PostgreSQL data volume"
    docker volume create "$POSTGRES_VOLUME_NAME" > /dev/null
  fi
  
  # Build run command with proper environment variables
  local docker_run_cmd=(
    "docker" "run" "--name" "$POSTGRES_CONTAINER_NAME"
    "-e" "POSTGRES_USER=$DB_USERNAME"
    "-e" "POSTGRES_PASSWORD=$DB_PASSWORD"
    "-e" "POSTGRES_DB=$DB_NAME"
    "-e" "PGSQL_APP_READ_PASSWORD=$READ_PASSWORD"
    "-e" "PGSQL_APP_WRITE_PASSWORD=$WRITE_PASSWORD"
    "-v" "${POSTGRES_VOLUME_NAME}:/var/lib/postgresql/data"
    "-v" "${SCRIPT_DIR}/../db:/docker-entrypoint-initdb.d"
    "-p" "${DB_PORT}:5432"
    "--restart" "unless-stopped"
    "-d" "$POSTGRES_IMAGE"
  )
  
  log_debug "Running: ${docker_run_cmd[*]}"
  
  if ! "${docker_run_cmd[@]}" > /dev/null; then
    log_error "Failed to create PostgreSQL container"
    return 1
  fi
  
  # Wait for database to be ready
  wait_for_db_ready
  return $?
}

# Wait for database to be ready
wait_for_db_ready() {
  local max_wait=30
  local wait_interval=1
  local waited=0
  
  log_info "Waiting for PostgreSQL to be ready"
  start_spinner "Waiting for database to start"
  
  while [[ $waited -lt $max_wait ]]; do
    if docker exec "$POSTGRES_CONTAINER_NAME" pg_isready -q -U "$DB_USERNAME"; then
      stop_spinner "true"
      log_info "PostgreSQL is ready"
      return 0
    fi
    
    sleep $wait_interval
    waited=$((waited + wait_interval))
  done
  
  stop_spinner "false"
  log_error "Timed out waiting for PostgreSQL to be ready"
  return 1
}

# Stop the database container
db_stop() {
  log_step "Stopping PostgreSQL database"
  
  # Check if container exists and is running
  if ! container_exists; then
    log_info "PostgreSQL container does not exist"
    return 0
  fi
  
  if ! container_running; then
    log_info "PostgreSQL container is already stopped"
    return 0
  fi
  
  # Stop the container
  log_info "Stopping PostgreSQL container"
  if ! docker stop "$POSTGRES_CONTAINER_NAME" > /dev/null; then
    log_error "Failed to stop PostgreSQL container"
    return 1
  fi
  
  log_info "PostgreSQL container stopped"
  return 0
}

# Restart the database container
db_restart() {
  log_step "Restarting PostgreSQL database"
  
  # Stop the container if it exists
  if container_exists; then
    db_stop || return 1
  fi
  
  # Start the container
  db_start
  return $?
}

# Initialize database schema
db_init() {
  log_step "Initializing database schema"
  
  # Ensure database is running
  db_start || return 1
  
  # Check if init script exists
  local init_script="${SCRIPT_DIR}/../db/init.sql"
  if [[ ! -f "$init_script" ]]; then
    log_error "Database initialization script not found: $init_script"
    return 1
  fi
  
  log_info "Applying database initialization script"
  
  # Set application parameters in PostgreSQL
  docker exec -i "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -c "SELECT set_config('app.read_password', '$READ_PASSWORD', false);" > /dev/null
  
  docker exec -i "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -c "SELECT set_config('app.write_password', '$WRITE_PASSWORD', false);" > /dev/null
  
  # Execute the script
  if ! cat "$init_script" | docker exec -i "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -f -; then
    log_error "Failed to execute database initialization script"
    return 1
  fi
  
  log_info "Database initialized successfully"
  return 0
}

# Execute SQL command in database
db_exec() {
  local sql="$1"
  local database="${2:-$DB_NAME}"
  local user="${3:-$DB_USERNAME}"
  
  log_debug "Executing SQL in PostgreSQL"
  
  # Check if database is running
  if ! container_running; then
    log_error "PostgreSQL container is not running"
    return 1
  fi
  
  # Execute the SQL
  echo "$sql" | docker exec -i "$POSTGRES_CONTAINER_NAME" \
    psql -U "$user" -d "$database" -t
}

# Create database backup
db_backup() {
  log_step "Backing up PostgreSQL database"
  
  local backup_dir="${1:-${DB_BACKUP_DIR:-./backups}}"
  local timestamp=$(date +"%Y%m%d_%H%M%S")
  local backup_file="${backup_dir}/${DB_NAME}_${timestamp}.sql"
  
  # Create backup directory if it doesn't exist
  mkdir -p "$backup_dir"
  
  # Check if container is running
  if ! container_running; then
    log_error "PostgreSQL container is not running"
    return 1
  fi
  
  log_info "Creating backup at: $backup_file"
  
  # Perform the backup
  start_spinner "Creating database backup"
  
  if ! docker exec "$POSTGRES_CONTAINER_NAME" \
    pg_dump -U "$DB_USERNAME" -d "$DB_NAME" > "$backup_file"; then
    stop_spinner "false"
    log_error "Failed to create database backup"
    return 1
  fi
  
  stop_spinner "true"
  
  # Set proper permissions on backup file
  chmod 600 "$backup_file"
  
  log_info "Database backup created successfully: $backup_file"
  return 0
}

# Restore database from backup
db_restore() {
  log_step "Restoring PostgreSQL database"
  
  local backup_file="$1"
  
  # Check if backup file exists
  if [[ -z "$backup_file" ]]; then
    log_error "No backup file specified"
    return 1
  fi
  
  if [[ ! -f "$backup_file" ]]; then
    log_error "Backup file not found: $backup_file"
    return 1
  fi
  
  # Ensure database is running
  db_start || return 1
  
  # Warn user about data loss
  log_warn "This will overwrite the current database with backup data"
  if [[ -t 0 ]] && ! read -p "Are you sure you want to continue? (y/n): " -n 1 -r && echo && [[ ! $REPLY =~ ^[Yy]$ ]]; then
    log_info "Restore cancelled by user"
    return 0
  fi
  
  log_info "Restoring from backup: $backup_file"
  
  # Restore the backup
  start_spinner "Restoring database from backup"
  
  # Create temporary database if it doesn't exist
  if ! docker exec "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -c "SELECT 1 FROM pg_database WHERE datname = '${DB_NAME}_temp'" | \
    grep -q 1; then
    
    docker exec "$POSTGRES_CONTAINER_NAME" \
      psql -U "$DB_USERNAME" -c "CREATE DATABASE ${DB_NAME}_temp"
  fi
  
  # Restore to temporary database first
  if ! cat "$backup_file" | docker exec -i "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -d "${DB_NAME}_temp"; then
    stop_spinner "false"
    log_error "Failed to restore database backup"
    return 1
  fi
  
  # Now switch databases
  docker exec "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME'"
  
  docker exec "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -c "DROP DATABASE IF EXISTS ${DB_NAME}_old"
  
  docker exec "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -c "ALTER DATABASE $DB_NAME RENAME TO ${DB_NAME}_old"
  
  docker exec "$POSTGRES_CONTAINER_NAME" \
    psql -U "$DB_USERNAME" -c "ALTER DATABASE ${DB_NAME}_temp RENAME TO $DB_NAME"
  
  stop_spinner "true"
  
  log_info "Database restored successfully"
  
  # Ask if user wants to drop the old database
  if [[ -t 0 ]] && read -p "Do you want to drop the old database backup? (y/n): " -n 1 -r && echo && [[ $REPLY =~ ^[Yy]$ ]]; then
    docker exec "$POSTGRES_CONTAINER_NAME" \
      psql -U "$DB_USERNAME" -c "DROP DATABASE IF EXISTS ${DB_NAME}_old"
    log_info "Old database dropped"
  else
    log_info "Old database preserved as ${DB_NAME}_old"
  fi
  
  return 0
}

# Print connection information
print_db_info() {
  log_info "PostgreSQL Connection Information"
  echo -e "${CYAN}Host:${NC}     $DB_HOST"
  echo -e "${CYAN}Port:${NC}     $DB_PORT"
  echo -e "${CYAN}Database:${NC} $DB_NAME"
  echo -e "${CYAN}Username:${NC} $DB_USERNAME"
  echo -e "${CYAN}Password:${NC} ********"
  echo -e "${CYAN}JDBC URL:${NC} jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
}

# Clean up database resources
db_clean() {
  log_step "Cleaning up database resources"
  
  db_stop || true
  
  # Remove container
  if container_exists; then
    log_info "Removing PostgreSQL container"
    if ! docker rm "$POSTGRES_CONTAINER_NAME" > /dev/null; then
      log_error "Failed to remove PostgreSQL container"
      return 1
    fi
  fi
  
  # Ask if user wants to remove volume
  if docker volume inspect "$POSTGRES_VOLUME_NAME" &> /dev/null; then
    if [[ -t 0 ]] && read -p "Do you want to remove the database volume? All data will be lost! (y/n): " -n 1 -r && echo && [[ $REPLY =~ ^[Yy]$ ]]; then
      log_info "Removing PostgreSQL volume"
      if ! docker volume rm "$POSTGRES_VOLUME_NAME" > /dev/null; then
        log_error "Failed to remove PostgreSQL volume"
        return 1
      fi
    fi
  fi
  
  log_info "Database resources cleaned up successfully"
  return 0
}

# Export functions for use by other scripts
export -f db_start
export -f db_stop
export -f db_restart
export -f db_status
export -f db_init
export -f db_backup
export -f db_restore
export -f db_exec
export -f print_db_info
export -f db_clean 