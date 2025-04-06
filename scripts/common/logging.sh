#!/usr/bin/env bash

# Simple colorized logging for shell scripts

# Colors with simplified declarations
RED="\033[31m"
GREEN="\033[32m"
YELLOW="\033[33m"
BLUE="\033[34m"
PURPLE="\033[35m"
BOLD="\033[1m"
NC="\033[0m"

# Print info message with blue prefix
log_info() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

# Print success message with green prefix
log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Print warning message with yellow prefix
log_warning() {
  echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Print error message with red prefix
log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# Print section header with purple text
log_section() {
  echo -e "\n${BOLD}${PURPLE}$1${NC}\n"
}