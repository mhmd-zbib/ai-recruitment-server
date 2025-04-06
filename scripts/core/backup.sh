#!/usr/bin/env bash

# Description: Creates and downloads backups of production database and files.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"
source "$SCRIPT_DIR/../utils/env.sh"

# Default values
ENVIRONMENT="production"
BACKUP_TYPE="db"
BACKUP_DIR="$PROJECT_ROOT/backups"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --env=*)
      ENVIRONMENT="${1#*=}"
      shift
      ;;
    --type=*)
      BACKUP_TYPE="${1#*=}"
      shift
      ;;
    --dir=*)
      BACKUP_DIR="${1#*=}"
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync backup [--env=staging|production] [--type=db|files|all] [--dir=/path/to/backup]"
      exit 1
      ;;
  esac
done

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Load environment variables
ENV_FILE="$PROJECT_ROOT/.env.$ENVIRONMENT"
if [ -f "$ENV_FILE" ]; then
  log_info "Loading environment variables from $ENV_FILE"
  load_env "$ENV_FILE"
else
  log_error "Environment file not found: $ENV_FILE"
  exit 1
fi

# Check if deployment credentials are available
if [ -z "$DEPLOY_USER" ] || [ -z "$DEPLOY_HOST" ]; then
  log_error "Deployment credentials not found in environment variables"
  exit 1
fi

# Create timestamp for backup files
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")

log_section "Backing up $ENVIRONMENT"

# Database backup
if [[ "$BACKUP_TYPE" == "db" || "$BACKUP_TYPE" == "all" ]]; then
  log_info "Creating database backup"
  
  # Create backup file on remote server
  ssh "$DEPLOY_USER@$DEPLOY_HOST" "bash -s" << EOF
    cd ~
    echo "Dumping database"
    pg_dump -U \$DB_USER -h \$DB_HOST -p \$DB_PORT -d \$DB_NAME -F c -f ~/hiresync-db-backup-$TIMESTAMP.dump
    echo "Database dump complete"
EOF
  
  # Download the backup file
  log_info "Downloading database backup"
  scp "$DEPLOY_USER@$DEPLOY_HOST:~/hiresync-db-backup-$TIMESTAMP.dump" "$BACKUP_DIR/"
  
  # Remove the remote backup file
  ssh "$DEPLOY_USER@$DEPLOY_HOST" "rm ~/hiresync-db-backup-$TIMESTAMP.dump"
  
  log_success "Database backup saved to $BACKUP_DIR/hiresync-db-backup-$TIMESTAMP.dump"
fi

# Files backup
if [[ "$BACKUP_TYPE" == "files" || "$BACKUP_TYPE" == "all" ]]; then
  log_info "Creating files backup"
  
  # Create backup archive on remote server
  ssh "$DEPLOY_USER@$DEPLOY_HOST" "bash -s" << EOF
    cd ~
    echo "Creating files archive"
    sudo tar -czf ~/hiresync-files-backup-$TIMESTAMP.tar.gz /opt/hiresync/uploads /opt/hiresync/config
    sudo chown \$USER:\$USER ~/hiresync-files-backup-$TIMESTAMP.tar.gz
    echo "Files archive complete"
EOF
  
  # Download the backup file
  log_info "Downloading files backup"
  scp "$DEPLOY_USER@$DEPLOY_HOST:~/hiresync-files-backup-$TIMESTAMP.tar.gz" "$BACKUP_DIR/"
  
  # Remove the remote backup file
  ssh "$DEPLOY_USER@$DEPLOY_HOST" "rm ~/hiresync-files-backup-$TIMESTAMP.tar.gz"
  
  log_success "Files backup saved to $BACKUP_DIR/hiresync-files-backup-$TIMESTAMP.tar.gz"
fi

# Create a backup list file
log_info "Updating backup inventory"
{
  echo "Backup created on $(date)"
  echo "Environment: $ENVIRONMENT"
  echo "Type: $BACKUP_TYPE"
  echo "Files:"
  ls -la "$BACKUP_DIR" | grep "$TIMESTAMP"
} >> "$BACKUP_DIR/backup-inventory.txt"

log_success "Backup of $ENVIRONMENT complete" 