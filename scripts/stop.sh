#!/bin/bash
# HireSync Stop Command
# Stops all services

# Load required modules
source "$(dirname "$0")/common.sh"
source "$(dirname "$0")/docker-utils.sh"

# Stop services
if ! stop_services; then
  log_warning "Some services may not have been stopped properly. Check status for details."
  exit 1
fi

log_success "HireSync has been stopped successfully" 