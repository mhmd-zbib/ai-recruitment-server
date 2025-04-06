#!/bin/bash

load_env() {
  local env_file="${1:-.env}"
  
  if [ ! -f "$env_file" ]; then
    echo "Error: Environment file not found: $env_file"
    return 1
  fi
  
  while IFS='=' read -r key value || [ -n "$key" ]; do
    # Skip comments and empty lines
    if [[ $key && ! $key =~ ^# ]]; then
      # Remove leading/trailing whitespace
      key=$(echo "$key" | xargs)
      value=$(echo "$value" | xargs)
      
      # Remove quotes if present
      value="${value%\"}"
      value="${value#\"}"
      value="${value%\'}"
      value="${value#\'}"
      
      # Export the variable
      export "$key=$value"
    fi
  done < "$env_file"
  
  return 0
}

# Get project root directory (assuming scripts are in PROJECT_ROOT/scripts/*)
get_project_root() {
  echo "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
} 
