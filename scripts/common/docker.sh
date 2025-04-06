#!/bin/bash

source "$(dirname "$0")/logging.sh"

check_docker() {
  if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed"
    return 1
  fi
  
  if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running"
    return 1
  fi
  
  return 0
}

check_docker_compose() {
  if ! command -v docker-compose &> /dev/null; then
    if ! docker compose version &> /dev/null; then
      log_error "Docker Compose is not installed"
      return 1
    fi
  fi
  
  return 0
}

get_container_status() {
  local container_name="$1"
  
  if [ -z "$container_name" ]; then
    log_error "Container name not provided"
    return 1
  fi
  
  local status=$(docker ps -a --filter "name=$container_name" --format "{{.Status}}" 2>/dev/null)
  
  if [ -z "$status" ]; then
    echo "not_found"
  elif [[ "$status" == *"Up"* ]]; then
    echo "running"
  elif [[ "$status" == *"Exited"* ]]; then
    echo "stopped"
  else
    echo "unknown"
  fi
}

wait_for_container_health() {
  local container_name="$1"
  local max_attempts="${2:-30}"
  local attempt=0
  
  while [ $attempt -lt $max_attempts ]; do
    local health=$(docker inspect --format="{{.State.Health.Status}}" "$container_name" 2>/dev/null)
    
    if [ "$health" = "healthy" ]; then
      return 0
    fi
    
    attempt=$((attempt + 1))
    sleep 1
  done
  
  return 1
} 
