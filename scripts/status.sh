#!/bin/bash
# HireSync Status Command
# Shows status of all services

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/docker-utils.sh"

# Load environment variables first to ensure we have database info
load_env || {
  log_error "Failed to load environment variables"
  exit 1
}

# Show status 
if ! show_status; then
  exit 1
fi 