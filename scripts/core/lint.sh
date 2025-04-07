#!/usr/bin/env bash
#
# Lint.sh - Code quality check script for Checkstyle, PMD, and Spotless
# Usage: ./lint.sh [--fix|--checkstyle|--pmd|--spotless|--container|--local|--help]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SETTINGS_FILE="settings.xml"
CONTAINER_NAME="hiresync-devtools"

# Parse command line options
FIX_ISSUES=false
CHECKSTYLE_ONLY=false
PMD_ONLY=false
SPOTLESS_ONLY=false
USE_CONTAINER=false

# Process arguments
for arg in "$@"; do
  case "$arg" in
    --fix) FIX_ISSUES=true ;;
    --checkstyle) CHECKSTYLE_ONLY=true ;;
    --pmd) PMD_ONLY=true ;;
    --spotless) SPOTLESS_ONLY=true ;;
    --container) USE_CONTAINER=true ;;
    --local) USE_CONTAINER=false ;;
    --help) 
      echo "Usage: ./lint.sh [options]"
      echo "Options: --fix, --checkstyle, --pmd, --spotless, --container, --local, --help"
      exit 0 ;;
    *) echo "Unknown option: $arg"; exit 1 ;;
  esac
done

# Check for conflicting options
if [[ "$CHECKSTYLE_ONLY" == true && "$PMD_ONLY" == true ]] || 
   [[ "$CHECKSTYLE_ONLY" == true && "$SPOTLESS_ONLY" == true ]] || 
   [[ "$PMD_ONLY" == true && "$SPOTLESS_ONLY" == true ]]; then
  echo "Error: Cannot use multiple exclusive options"
  exit 1
fi

# Configure Maven settings
if [[ -f "$PROJECT_ROOT/$SETTINGS_FILE" ]]; then
  SETTINGS_PARAM="-s $SETTINGS_FILE"
else
  SETTINGS_PARAM=""
  if [[ ! -f "$PROJECT_ROOT/$SETTINGS_FILE" && -f "$HOME/.m2/settings.xml" ]]; then
    cp "$HOME/.m2/settings.xml" "$PROJECT_ROOT/$SETTINGS_FILE"
    SETTINGS_PARAM="-s $SETTINGS_FILE"
  fi
fi

# Execute commands in container or locally
run_command() {
  if [[ "$USE_CONTAINER" == true ]]; then
    if ! command -v docker &> /dev/null || ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
      echo "Error: Docker not installed or container not running"
      exit 1
    fi
    docker exec "$CONTAINER_NAME" bash -c "cd /workspace && $1"
  else
    (cd "$PROJECT_ROOT" && eval "$1")
  fi
  return $?
}

# Handle command exit codes
handle_result() {
  if [[ $1 -eq 0 ]]; then echo "$2"; else echo "$3"; exit 1; fi
}

# Run specific checks based on options
echo "=== Code Linting ==="

if [[ "$CHECKSTYLE_ONLY" == true ]]; then
  run_command "mvn checkstyle:check $SETTINGS_PARAM"
  handle_result $? "Checkstyle passed" "Checkstyle failed"
elif [[ "$PMD_ONLY" == true ]]; then
  run_command "mvn pmd:check $SETTINGS_PARAM"
  handle_result $? "PMD passed" "PMD failed"
elif [[ "$SPOTLESS_ONLY" == true ]]; then
  if [[ "$FIX_ISSUES" == true ]]; then
    run_command "mvn spotless:apply $SETTINGS_PARAM"
    handle_result $? "Code formatting fixed" "Failed to fix formatting"
  else
    run_command "mvn spotless:check $SETTINGS_PARAM"
    handle_result $? "Spotless check passed" "Formatting issues found. Run with --fix to auto-format"
  fi
else
  if [[ "$FIX_ISSUES" == true ]]; then
    run_command "mvn spotless:apply $SETTINGS_PARAM"
  fi
  run_command "mvn verify -DskipTests $SETTINGS_PARAM -P lint"
  handle_result $? "All linting checks passed" "Linting failed"
fi 