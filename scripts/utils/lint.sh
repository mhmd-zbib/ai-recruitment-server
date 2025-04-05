#!/bin/bash
# HireSync Lint Command
# Performs code quality checks and linting

# Load required modules
source "$(dirname "$0")/common.sh"

log_section "HireSync Code Linting"

# Define directories to lint
SRC_DIR="../src"
TEST_DIR="../src/test"

# Run PMD if available
run_pmd() {
  log_info "Running PMD analysis..."
  
  if command -v mvn &> /dev/null; then
    mvn pmd:check
    if [ $? -eq 0 ]; then
      log_success "PMD checks passed"
      return 0
    else
      log_error "PMD checks failed"
      return 1
    fi
  else
    log_warning "Maven not found, skipping PMD"
    return 0
  fi
}

# Run SpotBugs if available
run_spotbugs() {
  log_info "Running SpotBugs analysis..."
  
  if command -v mvn &> /dev/null; then
    mvn spotbugs:check
    if [ $? -eq 0 ]; then
      log_success "SpotBugs checks passed"
      return 0
    else
      log_error "SpotBugs checks failed"
      return 1
    fi
  else
    log_warning "Maven not found, skipping SpotBugs"
    return 0
  fi
}

# Check for code duplication
check_duplication() {
  log_info "Checking for code duplication..."
  
  if command -v jscpd &> /dev/null; then
    jscpd --min-lines 10 --min-tokens 100 $SRC_DIR
    if [ $? -eq 0 ]; then
      log_success "No significant code duplication found"
      return 0
    else
      log_warning "Potential code duplication detected"
      return 1
    fi
  else
    log_warning "jscpd not found, skipping duplication check"
    return 0
  fi
}

# Verify code formatting
check_formatting() {
  log_info "Checking code formatting..."
  
  # Use Google Java Format if available
  if command -v google-java-format &> /dev/null; then
    UNFORMATTED=$(find $SRC_DIR -name "*.java" -exec google-java-format --dry-run {} \; | grep -v "Unhandled exception" | wc -l)
    
    if [ "$UNFORMATTED" -eq 0 ]; then
      log_success "All Java files are properly formatted"
      return 0
    else
      log_error "$UNFORMATTED Java files have formatting issues"
      return 1
    fi
  else
    log_warning "google-java-format not found, skipping format check"
    return 0
  fi
}

# Main execution
main() {
  ERRORS=0
  
  # Run all checks
  run_pmd || ((ERRORS++))
  run_spotbugs || ((ERRORS++))
  check_duplication || ((ERRORS++))
  check_formatting || ((ERRORS++))
  
  if [ $ERRORS -eq 0 ]; then
    log_section "All lint checks passed successfully!"
    return 0
  else
    log_section "$ERRORS lint check(s) failed"
    return 1
  fi
}

# Execute main function
main 