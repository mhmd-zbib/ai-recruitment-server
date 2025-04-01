#!/bin/bash
# HireSync Color Definitions
# ANSI color codes for terminal output formatting

# Reset
export NC='\033[0m' # No Color/Reset

# Regular Colors
export BLACK='\033[0;30m'
export RED='\033[0;31m'
export GREEN='\033[0;32m'
export YELLOW='\033[0;33m'
export BLUE='\033[0;34m'
export PURPLE='\033[0;35m'
export CYAN='\033[0;36m'
export WHITE='\033[0;37m'
export GRAY='\033[0;90m'

# Bold/Bright
export BOLD='\033[1m'
export BOLD_BLACK='\033[1;30m'
export BOLD_RED='\033[1;31m'
export BOLD_GREEN='\033[1;32m'
export BOLD_YELLOW='\033[1;33m'
export BOLD_BLUE='\033[1;34m'
export BOLD_PURPLE='\033[1;35m'
export BOLD_CYAN='\033[1;36m'
export BOLD_WHITE='\033[1;37m'

# Underlined
export UNDERLINE='\033[4m'
export UNDERLINE_BLACK='\033[4;30m'
export UNDERLINE_RED='\033[4;31m'
export UNDERLINE_GREEN='\033[4;32m'
export UNDERLINE_YELLOW='\033[4;33m'
export UNDERLINE_BLUE='\033[4;34m'
export UNDERLINE_PURPLE='\033[4;35m'
export UNDERLINE_CYAN='\033[4;36m'
export UNDERLINE_WHITE='\033[4;37m'

# Background Colors
export BG_BLACK='\033[40m'
export BG_RED='\033[41m'
export BG_GREEN='\033[42m'
export BG_YELLOW='\033[43m'
export BG_BLUE='\033[44m'
export BG_PURPLE='\033[45m'
export BG_CYAN='\033[46m'
export BG_WHITE='\033[47m'

# Disable colors if not in a terminal or if NO_COLOR is set
if [[ ! -t 1 || -n "${NO_COLOR:-}" ]]; then
  # Reset all colors to empty string
  NC=''
  BLACK=''
  RED=''
  GREEN=''
  YELLOW=''
  BLUE=''
  PURPLE=''
  CYAN=''
  WHITE=''
  GRAY=''
  BOLD=''
  BOLD_BLACK=''
  BOLD_RED=''
  BOLD_GREEN=''
  BOLD_YELLOW=''
  BOLD_BLUE=''
  BOLD_PURPLE=''
  BOLD_CYAN=''
  BOLD_WHITE=''
  UNDERLINE=''
  UNDERLINE_BLACK=''
  UNDERLINE_RED=''
  UNDERLINE_GREEN=''
  UNDERLINE_YELLOW=''
  UNDERLINE_BLUE=''
  UNDERLINE_PURPLE=''
  UNDERLINE_CYAN=''
  UNDERLINE_WHITE=''
  BG_BLACK=''
  BG_RED=''
  BG_GREEN=''
  BG_YELLOW=''
  BG_BLUE=''
  BG_PURPLE=''
  BG_CYAN=''
  BG_WHITE=''
fi

# Helper function to test colors
show_colors() {
  echo -e "Color Test:"
  echo -e "${BLACK}BLACK${NC} ${RED}RED${NC} ${GREEN}GREEN${NC} ${YELLOW}YELLOW${NC} ${BLUE}BLUE${NC} ${PURPLE}PURPLE${NC} ${CYAN}CYAN${NC} ${WHITE}WHITE${NC} ${GRAY}GRAY${NC}"
  echo -e "${BOLD}BOLD ${RED}RED${NC} ${BOLD}${GREEN}GREEN${NC} ${BOLD}${BLUE}BLUE${NC}"
  echo -e "${UNDERLINE}UNDERLINE ${UNDERLINE_RED}RED${NC} ${UNDERLINE_GREEN}GREEN${NC} ${UNDERLINE_BLUE}BLUE${NC}"
  echo -e "${BG_RED}BG_RED${NC} ${BG_GREEN}BG_GREEN${NC} ${BG_BLUE}BG_BLUE${NC}"
}

# Export the show_colors function
export -f show_colors