#!/bin/bash
# HireSync Clean Command
# Stops services and removes volumes

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/docker-utils.sh"

# Check Docker first
if ! check_docker; then
  log_warning "Docker is not running. Only partial cleaning may be possible."
fi

# Clean environment
if ! clean_environment; then
  log_warning "Clean operation may not have completed successfully"
  exit 1
fi

log_success "Environment has been cleaned successfully" 