#!/bin/bash
# HireSync Logging Utilities
# Common logging functions for consistent output formatting

# Text formatting
BOLD="\033[1m"
UNDERLINE="\033[4m"
NC="\033[0m" # No Color

# Text colors
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
BLUE="\033[0;34m"
PURPLE="\033[0;35m"
CYAN="\033[0;36m"

# Log a section header
log_section() {
  echo -e "\n${BOLD}${BLUE}=== $1 ===${NC}\n"
}

# Log an informational message
log_info() {
  echo -e "${CYAN}[INFO]${NC} $1"
}

# Log a success message
log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Log a warning message
log_warning() {
  echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Log an error message
log_error() {
  echo -e "${RED}[ERROR]${NC} $1" >&2
}

# Log a debug message (only if DEBUG is set)
log_debug() {
  if [[ -n "${DEBUG}" ]]; then
    echo -e "${PURPLE}[DEBUG]${NC} $1"
  fi
}

# Prompt the user for confirmation
# Returns 0 if confirmed, 1 if not
confirm() {
  local message="${1:-Are you sure?}"
  local response
  
  echo -ne "${YELLOW}${message} (y/N)${NC} "
  read -r response
  
  case "$response" in
    [yY][eE][sS]|[yY]) 
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

# Display a spinner while waiting for a command to complete
# Usage: spinner "Message" command args...
spinner() {
  local message="$1"
  shift
  
  local spin='-\|/'
  local i=0
  
  # Run the command in the background
  "$@" &
  local pid=$!
  
  # Display spinner
  echo -ne "${message} "
  while kill -0 $pid 2>/dev/null; do
    i=$(( (i+1) % 4 ))
    echo -ne "\b${spin:$i:1}"
    sleep 0.1
  done
  
  # Check if command succeeded
  wait $pid
  local status=$?
  
  # Clear the spinner
  echo -ne "\r${message} "
  if [ $status -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
  else
    echo -e "${RED}✗${NC}"
  fi
  
  return $status
}

# Run a command and log the result
run_command() {
  local cmd="$1"
  local error_msg="${2:-Command failed}"
  
  log_debug "Executing: $cmd"
  eval "$cmd"
  
  if [ $? -ne 0 ]; then
    log_error "$error_msg"
    return 1
  fi
  
  return 0
} 