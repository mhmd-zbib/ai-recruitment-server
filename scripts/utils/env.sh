#!/usr/bin/env bash

# Description: Provides functions to load environment variables from .env files and get the project root path.

# Simple environment file loader
load_env() {
  local env_file="${1:-.env}"
  
  if [ ! -f "$env_file" ]; then
    echo "Error: Environment file not found: $env_file"
    return 1
  fi
  
  # Export all variables from the .env file
  export $(grep -v '^#' "$env_file" | xargs)
  return 0
}

# Get project root directory with Windows path normalization
get_project_root() {
  local project_root
  project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
  
  # Normalize path for Windows environments
  if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    project_root=$(echo "$project_root" | sed 's/\\/\//g')
  fi
  
  echo "$project_root"
} 
