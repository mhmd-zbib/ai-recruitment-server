#!/bin/bash
# HireSync Help Command
# Displays usage information

# Load common utilities
source "$(dirname "$0")/common.sh" || {
  echo "Error: Failed to load common functions"
  exit 1
}

# Display usage information
usage 