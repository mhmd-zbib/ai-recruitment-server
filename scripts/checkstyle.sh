#!/bin/bash
# Script to run Checkstyle checks on the HireSync codebase

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Set script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Import common utilities
source "$SCRIPT_DIR/common.sh"

# Default parameters
CHECK_MODE="full"
INCLUDE_TESTS=false

# Show usage information
show_usage() {
  echo -e "${BOLD}Usage:${NC} $0 [OPTIONS]"
  echo "Run Checkstyle checks on the project codebase"
  echo
  echo -e "${BOLD}Options:${NC}"
  echo "  -q, --quick        Quick mode: check only modified files"
  echo "  -f, --full         Full mode: check all files (default)"
  echo "  -t, --test         Include test code in checks"
  echo "  -h, --help         Show this help message"
  echo
  echo -e "${BOLD}Examples:${NC}"
  echo "  $0 --quick         # Check only modified files"
  echo "  $0 --full --test   # Check all files including tests"
  exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -q|--quick)
      CHECK_MODE="quick"
      shift
      ;;
    -f|--full)
      CHECK_MODE="full"
      shift
      ;;
    -t|--test)
      INCLUDE_TESTS=true
      shift
      ;;
    -h|--help)
      show_usage
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      show_usage
      ;;
  esac
done

# Find Maven executable
if [ -f "$PROJECT_ROOT/mvnw" ]; then
  MVN_CMD="$PROJECT_ROOT/mvnw"
else
  MVN_CMD="mvn"
fi

# Run Checkstyle checks
run_checkstyle() {
  log_header "Running Checkstyle checks..."
  
  # Change to project root
  cd "$PROJECT_ROOT" || {
    log_error "Failed to change to project root directory"
    return 1
  }
  
  # Build command based on mode
  CHECKSTYLE_CMD="$MVN_CMD checkstyle:check -Dcheckstyle.skip=false"
  
  # Quick mode: only modified files
  if [ "$CHECK_MODE" = "quick" ]; then
    log_info "Running in quick mode (modified files only)"
    
    # Get list of modified Java files
    MODIFIED_FILES=$(git ls-files --modified --others --exclude-standard | grep -E '\.java$' || true)
    
    if [ -z "$MODIFIED_FILES" ]; then
      log_info "No modified Java files found"
      return 0
    fi
    
    # Create a temporary file with the list of files to check
    FILES_LIST=$(mktemp)
    echo "$MODIFIED_FILES" > "$FILES_LIST"
    
    CHECKSTYLE_CMD="$CHECKSTYLE_CMD -DcheckstyleFiles=\"$FILES_LIST\""
  else
    log_info "Running in full mode (all files)"
  fi
  
  # Include test files if requested
  if [ "$INCLUDE_TESTS" = true ]; then
    log_info "Including test files in checks"
    CHECKSTYLE_CMD="$CHECKSTYLE_CMD -Dcheckstyle.includeTestSourceDirectory=true"
  else
    CHECKSTYLE_CMD="$CHECKSTYLE_CMD -Dcheckstyle.includeTestSourceDirectory=false"
  fi
  
  # Run Checkstyle
  log_info "Executing: $CHECKSTYLE_CMD"
  eval "$CHECKSTYLE_CMD"
  
  # Check the result
  if [ $? -eq 0 ]; then
    log_success "Checkstyle checks passed!"
    
    # Clean up temporary file if in quick mode
    if [ "$CHECK_MODE" = "quick" ] && [ -f "$FILES_LIST" ]; then
      rm -f "$FILES_LIST"
    fi
    
    return 0
  else
    log_error "Checkstyle checks failed. Please fix the issues."
    
    # Clean up temporary file if in quick mode
    if [ "$CHECK_MODE" = "quick" ] && [ -f "$FILES_LIST" ]; then
      rm -f "$FILES_LIST"
    fi
    
    return 1
  fi
}

# Run Checkstyle
run_checkstyle 