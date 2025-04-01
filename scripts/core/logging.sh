#!/bin/bash
# HireSync Logging
# Provides consistent logging and output formatting

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source colors if not already sourced
source "${SCRIPT_DIR}/colors.sh"

# Logging levels
readonly LOG_LEVEL_DEBUG=0
readonly LOG_LEVEL_INFO=1
readonly LOG_LEVEL_WARN=2
readonly LOG_LEVEL_ERROR=3
readonly LOG_LEVEL_NONE=4

# Current log level (can be overridden by environment)
LOG_LEVEL="${LOG_LEVEL:-$LOG_LEVEL_INFO}"

# Convert string log level to numeric
if [[ "$LOG_LEVEL" == "DEBUG" ]]; then
  LOG_LEVEL="$LOG_LEVEL_DEBUG"
elif [[ "$LOG_LEVEL" == "INFO" ]]; then
  LOG_LEVEL="$LOG_LEVEL_INFO"
elif [[ "$LOG_LEVEL" == "WARN" ]]; then
  LOG_LEVEL="$LOG_LEVEL_WARN"
elif [[ "$LOG_LEVEL" == "ERROR" ]]; then
  LOG_LEVEL="$LOG_LEVEL_ERROR"
elif [[ "$LOG_LEVEL" == "NONE" ]]; then
  LOG_LEVEL="$LOG_LEVEL_NONE"
fi

# Current timestamp for logging
timestamp() {
  date +"%Y-%m-%d %H:%M:%S"
}

# Log a debug message
log_debug() {
  if [[ $LOG_LEVEL -le $LOG_LEVEL_DEBUG ]]; then
    echo -e "${BLUE}[$(timestamp)] [DEBUG] $*${NC}" >&2
  fi
}

# Log an info message
log_info() {
  if [[ $LOG_LEVEL -le $LOG_LEVEL_INFO ]]; then
    echo -e "${GREEN}[$(timestamp)] [INFO] $*${NC}" >&2
  fi
}

# Log a warning message
log_warn() {
  if [[ $LOG_LEVEL -le $LOG_LEVEL_WARN ]]; then
    echo -e "${YELLOW}[$(timestamp)] [WARN] $*${NC}" >&2
  fi
}

# Log an error message
log_error() {
  if [[ $LOG_LEVEL -le $LOG_LEVEL_ERROR ]]; then
    echo -e "${RED}[$(timestamp)] [ERROR] $*${NC}" >&2
  fi
}

# Log a success message
log_success() {
  if [[ $LOG_LEVEL -le $LOG_LEVEL_INFO ]]; then
    echo -e "${BOLD_GREEN}[$(timestamp)] [SUCCESS] $*${NC}" >&2
  fi
}

# Log a section step
log_step() {
  if [[ $LOG_LEVEL -le $LOG_LEVEL_INFO ]]; then
    echo -e "\n${CYAN}▶ $*${NC}" >&2
  fi
}

# Check if we're running in CI
is_ci() {
  [[ -n "${CI:-}" || -n "${GITHUB_ACTIONS:-}" ]]
  return $?
}

# Spinner variables
SPINNER_PID=""
SPINNER_MESSAGE=""
SPINNER_RUNNING=false

# Start a spinner for long-running tasks
start_spinner() {
  local message="$1"
  SPINNER_MESSAGE="$message"
  
  # Don't start spinner in CI environment
  if is_ci; then
    echo -e "${CYAN}$message...${NC}" >&2
    return 0
  fi
  
  # Check if another spinner is running
  if [[ "$SPINNER_RUNNING" == "true" ]]; then
    stop_spinner "false"
  fi
  
  SPINNER_RUNNING=true
  
  # Start the spinner in background
  (
    local chars=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
    local delay=0.1
    local i=0
    
    while true; do
      local char="${chars[$i]}"
      printf "\r${CYAN}${char} %s...${NC}" "$message" >&2
      sleep $delay
      i=$(( (i + 1) % ${#chars[@]} ))
    done
  ) &
  
  SPINNER_PID=$!
  
  # Ensure spinner is killed on exit
  trap 'stop_spinner "false"' EXIT
}

# Stop the spinner
stop_spinner() {
  local success="${1:-true}"
  
  # If not running or in CI, do nothing
  if [[ "$SPINNER_RUNNING" != "true" || $(is_ci) ]]; then
    return 0
  fi
  
  # Kill the spinner process
  kill $SPINNER_PID 2>/dev/null
  wait $SPINNER_PID 2>/dev/null || true
  SPINNER_PID=""
  SPINNER_RUNNING=false
  
  # Clear the spinner line
  printf "\r\033[K" >&2
  
  # Print final status
  if [[ "$success" == "true" ]]; then
    echo -e "${GREEN}✓ ${SPINNER_MESSAGE} - Done${NC}" >&2
  else
    echo -e "${RED}✗ ${SPINNER_MESSAGE} - Failed${NC}" >&2
  fi
}

# Ask a yes/no question
ask_yes_no() {
  local question="$1"
  local default="${2:-true}"
  local prompt
  
  # Don't ask in non-interactive mode
  if [[ ! -t 0 || -n "${CI:-}" || -n "${NON_INTERACTIVE:-}" ]]; then
    return $([ "$default" == "true" ] && echo 0 || echo 1)
  fi
  
  if [[ "$default" == "true" ]]; then
    prompt="${question} [Y/n] "
  else
    prompt="${question} [y/N] "
  fi
  
  while true; do
    read -p "$prompt" response
    response="${response:-$default}"
    
    case "$response" in
      [Yy]|[Yy][Ee][Ss]|"true")
        return 0
        ;;
      [Nn]|[Nn][Oo]|"false")
        return 1
        ;;
      *)
        echo "Please answer with yes/no or y/n"
        ;;
    esac
  done
}

# Export functions
export LOG_LEVEL
export -f timestamp
export -f log_debug
export -f log_info
export -f log_warn
export -f log_error
export -f log_success
export -f log_step
export -f is_ci
export -f start_spinner
export -f stop_spinner
export -f ask_yes_no 