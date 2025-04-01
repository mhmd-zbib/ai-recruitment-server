#!/bin/bash
# HireSync Environment Management
# Handles application configuration through environment variables

# Source logging
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/logging.sh"

# Project root path
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Environment file path
readonly ENV_FILE="${PROJECT_ROOT}/.env"
readonly ENV_EXAMPLE_FILE="${PROJECT_ROOT}/.env.example"

# Default configuration
declare -A DEFAULTS=(
  [DB_HOST]="localhost"
  [DB_PORT]="5432"
  [DB_NAME]="hiresync_db"
  [DB_USERNAME]="hiresync_user"
  [DB_PASSWORD]="hiresync_password"
  [READ_PASSWORD]="read_password"
  [WRITE_PASSWORD]="write_password"
  [DB_POOL_SIZE]="10"
  [DB_TIMEOUT]="30000"
  [SERVER_PORT]="8080"
  [JWT_EXPIRATION]="86400000"
  [SPRING_PROFILES_ACTIVE]="dev"
  [LOG_LEVEL]="INFO"
)

# Required variables that must be set
readonly REQUIRED_VARS=("DB_HOST" "DB_NAME" "DB_USERNAME" "DB_PASSWORD")

# Load environment variables from file
load_env() {
  local env_file="${1:-$ENV_FILE}"
  local silent="${2:-false}"
  
  if [[ ! -f "$env_file" ]]; then
    [[ "$silent" != "true" ]] && log_warn "Environment file not found: $env_file"
    return 1
  fi
  
  log_debug "Loading environment from $env_file"
  
  # Read and export each non-comment, non-empty line
  while IFS='=' read -r key value || [[ -n "$key" ]]; do
    # Skip comments and empty lines
    [[ "$key" =~ ^#.*$ || -z "$key" ]] && continue
    
    # Trim whitespace from key and value
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)
    
    # Handle quoted values
    value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
    
    # Export the variable
    export "$key=$value"
    log_trace "Set $key=$value"
  done < "$env_file"
  
  # Ensure required variables have values
  ensure_required_vars
  
  [[ "$silent" != "true" ]] && log_info "Environment loaded from $env_file"
  return 0
}

# Ensure all required variables have values
ensure_required_vars() {
  local missing=()
  
  # Check required variables
  for var in "${REQUIRED_VARS[@]}"; do
    if [[ -z "${!var}" ]]; then
      missing+=("$var")
      # Set default if available
      if [[ -n "${DEFAULTS[$var]}" ]]; then
        export "$var=${DEFAULTS[$var]}"
        log_debug "Set missing required variable $var to default: ${DEFAULTS[$var]}"
      fi
    fi
  done
  
  # Set defaults for other variables
  for var in "${!DEFAULTS[@]}"; do
    if [[ -z "${!var}" ]]; then
      export "$var=${DEFAULTS[$var]}"
      log_trace "Set $var to default: ${DEFAULTS[$var]}"
    fi
  done
  
  # Report missing required variables
  if [[ ${#missing[@]} -gt 0 ]]; then
    log_warn "Missing required variables: ${missing[*]}"
    log_info "Default values have been applied where available"
  fi
}

# Create a new environment file
create_env_file() {
  local env_file="${1:-$ENV_FILE}"
  local force="${2:-false}"
  
  # Check if file exists and handle backup
  if [[ -f "$env_file" && "$force" != "true" ]]; then
    log_warn "Environment file already exists: $env_file"
    
    # Backup existing file if force mode or user confirms
    if [[ "$force" == "true" ]] || ask_yes_no "Backup and overwrite existing environment file?"; then
      backup_env_file "$env_file"
    else
      log_info "Using existing environment file"
      return 0
    fi
  fi
  
  log_info "Creating new environment file: $env_file"
  
  # Generate random JWT secret if not set
  local jwt_secret="${JWT_SECRET:-$(generate_secret 32)}"
  
  # Create the environment file
  cat > "$env_file" << EOF
# HireSync Application Environment
# Generated on $(date)

# Database Configuration
DB_HOST=${DB_HOST:-${DEFAULTS[DB_HOST]}}
DB_PORT=${DB_PORT:-${DEFAULTS[DB_PORT]}}
DB_NAME=${DB_NAME:-${DEFAULTS[DB_NAME]}}
DB_USERNAME=${DB_USERNAME:-${DEFAULTS[DB_USERNAME]}}
DB_PASSWORD=${DB_PASSWORD:-${DEFAULTS[DB_PASSWORD]}}
READ_PASSWORD=${READ_PASSWORD:-${DEFAULTS[READ_PASSWORD]}}
WRITE_PASSWORD=${WRITE_PASSWORD:-${DEFAULTS[WRITE_PASSWORD]}}
DB_POOL_SIZE=${DB_POOL_SIZE:-${DEFAULTS[DB_POOL_SIZE]}}
DB_TIMEOUT=${DB_TIMEOUT:-${DEFAULTS[DB_TIMEOUT]}}

# Application Configuration
SERVER_PORT=${SERVER_PORT:-${DEFAULTS[SERVER_PORT]}}
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-${DEFAULTS[SPRING_PROFILES_ACTIVE]}}
LOG_LEVEL=${LOG_LEVEL:-${DEFAULTS[LOG_LEVEL]}}

# Security Configuration
JWT_SECRET=$jwt_secret
JWT_EXPIRATION=${JWT_EXPIRATION:-${DEFAULTS[JWT_EXPIRATION]}}

# Debug Configuration (development only)
DEBUG_ENABLED=${DEBUG_ENABLED:-false}
DEBUG_PORT=${DEBUG_PORT:-5005}
EOF

  # Set proper permissions (only owner can read/write)
  chmod 600 "$env_file"
  
  log_info "Created environment file with secure permissions"
  
  # Also create an example file without sensitive data
  create_env_example
  
  return 0
}

# Create .env.example file
create_env_example() {
  log_debug "Creating example environment file"
  
  cat > "$ENV_EXAMPLE_FILE" << EOF
# HireSync Application Environment Example
# Copy this file to .env and configure for your environment

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=hiresync_db
DB_USERNAME=hiresync_user
DB_PASSWORD=your_secure_password
READ_PASSWORD=your_read_password
WRITE_PASSWORD=your_write_password
DB_POOL_SIZE=10
DB_TIMEOUT=30000

# Application Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=INFO

# Security Configuration
JWT_SECRET=your_secret_key_at_least_32_chars_long
JWT_EXPIRATION=86400000

# Debug Configuration (development only)
DEBUG_ENABLED=false
DEBUG_PORT=5005
EOF

  chmod 644 "$ENV_EXAMPLE_FILE"
  log_debug "Created example environment file"
}

# Backup existing environment file
backup_env_file() {
  local env_file="$1"
  local backup_file="${env_file}.backup.$(date +%Y%m%d%H%M%S)"
  
  log_info "Backing up environment file to $backup_file"
  cp "$env_file" "$backup_file"
  chmod 600 "$backup_file"
}

# Generate a random secret
generate_secret() {
  local length="${1:-32}"
  
  # Try openssl first
  if command -v openssl &>/dev/null; then
    openssl rand -hex "$length"
    return
  fi
  
  # Fallback to /dev/urandom on Unix-like systems
  if [[ -f /dev/urandom ]]; then
    head -c "$length" /dev/urandom | xxd -p
    return
  fi
  
  # Last resort - use bash's $RANDOM
  local result=""
  local chars="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  for i in $(seq 1 "$length"); do
    local idx=$(( RANDOM % ${#chars} ))
    result+="${chars:$idx:1}"
  done
  echo "$result"
}

# Display current environment settings (safe for logging)
print_env() {
  [[ $LOG_LEVEL -lt 2 ]] && return 0
  
  log_info "Environment Configuration"
  
  # Print database settings (mask passwords)
  echo -e "${BOLD}Database Settings:${NC}"
  echo -e "  ${CYAN}Host:${NC}     ${DB_HOST:-${DEFAULTS[DB_HOST]}}"
  echo -e "  ${CYAN}Port:${NC}     ${DB_PORT:-${DEFAULTS[DB_PORT]}}"
  echo -e "  ${CYAN}Name:${NC}     ${DB_NAME:-${DEFAULTS[DB_NAME]}}"
  echo -e "  ${CYAN}User:${NC}     ${DB_USERNAME:-${DEFAULTS[DB_USERNAME]}}"
  [[ -n "${DB_PASSWORD}" ]] && echo -e "  ${CYAN}Password:${NC} ********"
  
  # Print application settings
  echo -e "\n${BOLD}Application Settings:${NC}"
  echo -e "  ${CYAN}Port:${NC}       ${SERVER_PORT:-${DEFAULTS[SERVER_PORT]}}"
  echo -e "  ${CYAN}Profile:${NC}    ${SPRING_PROFILES_ACTIVE:-${DEFAULTS[SPRING_PROFILES_ACTIVE]}}"
  echo -e "  ${CYAN}Log Level:${NC}  ${LOG_LEVEL:-${DEFAULTS[LOG_LEVEL]}}"
  
  # Print security settings (mask JWT secret)
  echo -e "\n${BOLD}Security Settings:${NC}"
  [[ -n "${JWT_SECRET}" ]] && echo -e "  ${CYAN}JWT Secret:${NC}      ********"
  echo -e "  ${CYAN}JWT Expiration:${NC} ${JWT_EXPIRATION:-${DEFAULTS[JWT_EXPIRATION]}} ms"
}

# Ask user for yes/no confirmation
ask_yes_no() {
  local prompt="$1"
  local default="${2:-n}"
  
  # Skip if running in non-interactive mode
  if [[ "${INTERACTIVE:-true}" != "true" ]]; then
    [[ "$default" == "y" ]] && return 0 || return 1
  fi
  
  local options="[y/N]"
  [[ "$default" == "y" ]] && options="[Y/n]"
  
  while true; do
    read -p "$prompt $options " answer
    answer=${answer:-$default}
    case "${answer,,}" in
      y|yes) return 0 ;;
      n|no) return 1 ;;
      *) echo "Please answer yes or no." ;;
    esac
  done
}

# Export functions
export -f load_env
export -f ensure_required_vars
export -f create_env_file
export -f print_env
export -f ask_yes_no 