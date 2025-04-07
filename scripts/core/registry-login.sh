#!/usr/bin/env bash

# Description: Logs into Docker registries for image pushing

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"

# Default values
REGISTRY="ghcr.io"
USERNAME=""
PASSWORD_FILE=""
PASSWORD=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --registry=*)
      REGISTRY="${1#*=}"
      shift
      ;;
    --username=*)
      USERNAME="${1#*=}"
      shift
      ;;
    --password-file=*)
      PASSWORD_FILE="${1#*=}"
      shift
      ;;
    --password=*)
      PASSWORD="${1#*=}"
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync registry-login [--registry=REGISTRY] [--username=USERNAME] [--password-file=FILE] [--password=PASSWORD]"
      exit 1
      ;;
  esac
done

# Use GitHub environment variables if available and not explicitly set
if [ -z "$USERNAME" ] && [ -n "$GITHUB_ACTOR" ]; then
  USERNAME="$GITHUB_ACTOR"
  log_info "Using GitHub actor as username: $USERNAME"
fi

# Check required parameters
if [ -z "$USERNAME" ]; then
  log_error "Username is required"
  echo "Please provide a username with --username=USERNAME"
  exit 1
fi

if [ -z "$PASSWORD" ] && [ -z "$PASSWORD_FILE" ] && [ -n "$GITHUB_TOKEN" ]; then
  log_info "Using GitHub token for authentication"
  PASSWORD="$GITHUB_TOKEN"
elif [ -n "$PASSWORD_FILE" ]; then
  if [ ! -f "$PASSWORD_FILE" ]; then
    log_error "Password file not found: $PASSWORD_FILE"
    exit 1
  fi
  
  PASSWORD=$(cat "$PASSWORD_FILE")
  log_info "Using password from file: $PASSWORD_FILE"
elif [ -z "$PASSWORD" ]; then
  log_error "Password or password file is required"
  echo "Please provide a password with --password=PASSWORD or --password-file=FILE"
  exit 1
fi

log_section "Docker Registry Login"
log_info "Logging into registry: $REGISTRY"

# Login to registry
echo "$PASSWORD" | docker login "$REGISTRY" -u "$USERNAME" --password-stdin

if [ $? -ne 0 ]; then
  log_error "Failed to login to registry: $REGISTRY"
  exit 1
else
  log_success "Successfully logged into registry: $REGISTRY"
fi 