#!/bin/bash

# CI/CD verification script for HireSync application
# This script runs various checks to ensure code quality and correctness

# Get script directory for reliable sourcing
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
RUN_TESTS=true
RUN_CHECKSTYLE=true
RUN_PMD=true
RUN_SPOTLESS=true
FAIL_ON_ERROR=true

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync CI/CD Verification${NC}"
echo -e "${BLUE}========================================${NC}"

# Process command-line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-tests)
      RUN_TESTS=false
      shift
      ;;
    --skip-checkstyle)
      RUN_CHECKSTYLE=false
      shift
      ;;
    --skip-pmd)
      RUN_PMD=false
      shift
      ;;
    --skip-spotless)
      RUN_SPOTLESS=false
      shift
      ;;
    --no-fail)
      FAIL_ON_ERROR=false
      shift
      ;;
    --help)
      echo -e "Usage: ./scripts/verify.sh [options]"
      echo -e "Options:"
      echo -e "  --skip-tests         Skip running tests"
      echo -e "  --skip-checkstyle    Skip running checkstyle"
      echo -e "  --skip-pmd           Skip running PMD"
      echo -e "  --skip-spotless      Skip running Spotless"
      echo -e "  --no-fail            Don't fail the build on errors (useful for local checks)"
      echo -e "  --help               Display this help message"
      echo -e ""
      echo -e "Examples:"
      echo -e "  ./scripts/verify.sh              # Run all checks"
      echo -e "  ./scripts/verify.sh --skip-tests # Skip tests but run other checks"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Change to project root
cd "$PROJECT_ROOT" || exit 1

# Initialize result status
RESULT=0

# Run Spotless to check code formatting
run_spotless() {
  if [ "$RUN_SPOTLESS" = true ]; then
    echo -e "${YELLOW}Running Spotless to check code formatting...${NC}"
    ./mvnw spotless:check
    
    local spotless_result=$?
    if [ $spotless_result -ne 0 ]; then
      echo -e "${RED}Spotless found formatting issues.${NC}"
      echo -e "${YELLOW}To fix formatting issues automatically, run: ./mvnw spotless:apply${NC}"
      RESULT=1
    else
      echo -e "${GREEN}Spotless check passed.${NC}"
    fi
  fi
}

# Run Checkstyle to verify code style
run_checkstyle() {
  if [ "$RUN_CHECKSTYLE" = true ]; then
    echo -e "${YELLOW}Running Checkstyle to verify code style...${NC}"
    ./mvnw checkstyle:check
    
    local checkstyle_result=$?
    if [ $checkstyle_result -ne 0 ]; then
      echo -e "${RED}Checkstyle found issues.${NC}"
      RESULT=1
    else
      echo -e "${GREEN}Checkstyle check passed.${NC}"
    fi
  fi
}

# Run PMD for static code analysis
run_pmd() {
  if [ "$RUN_PMD" = true ]; then
    echo -e "${YELLOW}Running PMD for static code analysis...${NC}"
    ./mvnw pmd:check
    
    local pmd_result=$?
    if [ $pmd_result -ne 0 ]; then
      echo -e "${RED}PMD found issues.${NC}"
      RESULT=1
    else
      echo -e "${GREEN}PMD check passed.${NC}"
    fi
  fi
}

# Run Tests
run_tests() {
  if [ "$RUN_TESTS" = true ]; then
    echo -e "${YELLOW}Running tests...${NC}"
    ./mvnw test
    
    local test_result=$?
    if [ $test_result -ne 0 ]; then
      echo -e "${RED}Tests failed.${NC}"
      RESULT=1
    else
      echo -e "${GREEN}All tests passed.${NC}"
    fi
  fi
}

# Run all verification checks
run_spotless
run_checkstyle
run_pmd
run_tests

# Final result
if [ $RESULT -ne 0 ]; then
  echo -e "${RED}Verification failed. Please fix the issues before committing.${NC}"
  
  if [ "$FAIL_ON_ERROR" = true ]; then
    exit 1
  fi
else
  echo -e "${GREEN}All verification checks passed successfully!${NC}"
fi

exit $RESULT 