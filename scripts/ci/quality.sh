#!/bin/bash
# HireSync Quality Checks
# Runs comprehensive code quality checks for the project

# Source core utilities
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="$(cd "${SCRIPT_DIR}/../core" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

source "${CORE_DIR}/logging.sh"
source "${CORE_DIR}/env.sh"

# Run code quality checks
check_quality() {
  log_step "Running code quality checks"
  
  # Load environment variables
  load_env "${PROJECT_ROOT}/.env" true
  
  cd "$PROJECT_ROOT"
  
  # Check for Maven wrapper
  if [[ ! -f "./mvnw" ]]; then
    log_error "Maven wrapper not found"
    return 1
  fi
  
  # Make wrapper executable
  chmod +x ./mvnw
  
  # Check if we're running specific checks only
  if [[ -n "$CHECKS" ]]; then
    log_info "Running selected checks: $CHECKS"
    run_selected_checks "$CHECKS"
  else
    # Run all checks
    log_info "Running all code quality checks"
    run_all_checks
  fi
  
  # Check for serious issues
  local serious_issues=0
  if [[ -f "target/checkstyle-result.xml" ]]; then
    serious_issues=$(grep -c "<error.*severity=\"error\"" "target/checkstyle-result.xml" || echo 0)
  fi
  
  # Show summary
  log_info "Quality check summary:"
  echo -e "${BOLD}Checkstyle errors:${NC} $serious_issues"
  
  if [[ $serious_issues -gt 0 ]]; then
    log_error "Quality checks failed: $serious_issues serious issues found"
    return 1
  else
    log_info "All quality checks passed"
    return 0
  fi
}

# Run all quality checks
run_all_checks() {
  # Run checks in order of importance and speed
  run_style_checks
  run_static_analysis
  run_test_coverage
}

# Run selected checks
run_selected_checks() {
  local checks="$1"
  
  for check in $(echo "$checks" | tr ',' ' '); do
    case "$check" in
      style)
        run_style_checks
        ;;
      spotbugs|bugs)
        run_spotbugs
        ;;
      pmd)
        run_pmd
        ;;
      sonar)
        run_sonar
        ;;
      coverage)
        run_test_coverage
        ;;
      *)
        log_warn "Unknown check: $check"
        ;;
    esac
  done
}

# Run style checks (checkstyle, formatter)
run_style_checks() {
  log_info "Running style checks"
  
  # Check code style and formatting
  start_spinner "Running formatter checks"
  ./mvnw spotless:check -q
  local result=$?
  
  if [[ $result -eq 0 ]]; then
    stop_spinner "true"
    log_info "Formatter check passed"
  else
    stop_spinner "false"
    log_error "Formatter check failed"
    
    if [[ "$AUTO_FIX" == "true" ]]; then
      log_info "Automatically fixing formatting issues"
      ./mvnw spotless:apply -q
    else
      log_info "Run './mvnw spotless:apply' to fix formatting issues"
    fi
  fi
  
  # Run checkstyle
  start_spinner "Running Checkstyle"
  ./mvnw checkstyle:check -q
  result=$?
  
  if [[ $result -eq 0 ]]; then
    stop_spinner "true"
    log_info "Checkstyle check passed"
  else
    stop_spinner "false"
    log_error "Checkstyle check failed"
    log_info "Check target/checkstyle-result.xml for details"
  fi
}

# Run static analysis (SpotBugs, PMD)
run_static_analysis() {
  # Run SpotBugs
  run_spotbugs
  
  # Run PMD
  run_pmd
}

# Run SpotBugs
run_spotbugs() {
  log_info "Running SpotBugs"
  
  start_spinner "Analyzing code with SpotBugs"
  ./mvnw com.github.spotbugs:spotbugs-maven-plugin:check -q
  local result=$?
  
  if [[ $result -eq 0 ]]; then
    stop_spinner "true"
    log_info "SpotBugs check passed"
  else
    stop_spinner "false"
    log_error "SpotBugs check failed"
    log_info "Check target/spotbugsXml.xml for details"
  fi
}

# Run PMD
run_pmd() {
  log_info "Running PMD"
  
  start_spinner "Analyzing code with PMD"
  ./mvnw pmd:check -q
  local result=$?
  
  if [[ $result -eq 0 ]]; then
    stop_spinner "true"
    log_info "PMD check passed"
  else
    stop_spinner "false"
    log_error "PMD check failed"
    log_info "Check target/pmd.xml for details"
  fi
}

# Run SonarQube analysis
run_sonar() {
  log_info "Running SonarQube analysis"
  
  # Check if SonarQube token is available
  if [[ -z "${SONAR_TOKEN:-}" ]]; then
    log_error "SONAR_TOKEN environment variable not set"
    log_info "Set SONAR_TOKEN to run SonarQube analysis"
    return 1
  fi
  
  # Run SonarQube analysis
  start_spinner "Analyzing code with SonarQube"
  ./mvnw sonar:sonar -Dsonar.host.url="${SONAR_HOST_URL:-http://localhost:9000}" -Dsonar.login="$SONAR_TOKEN" -q
  local result=$?
  
  if [[ $result -eq 0 ]]; then
    stop_spinner "true"
    log_info "SonarQube analysis completed"
  else
    stop_spinner "false"
    log_error "SonarQube analysis failed"
  fi
}

# Run test coverage analysis
run_test_coverage() {
  log_info "Running test coverage analysis"
  
  start_spinner "Running tests with JaCoCo"
  ./mvnw verify -q
  local result=$?
  
  if [[ $result -eq 0 ]]; then
    stop_spinner "true"
    log_info "Test coverage analysis completed"
    log_info "Coverage report generated at: target/site/jacoco/index.html"
    
    # Check coverage thresholds
    check_coverage_thresholds
  else
    stop_spinner "false"
    log_error "Test coverage analysis failed"
  fi
}

# Check coverage thresholds
check_coverage_thresholds() {
  local jacoco_csv="target/site/jacoco/jacoco.csv"
  
  if [[ ! -f "$jacoco_csv" ]]; then
    log_warn "JaCoCo report not found"
    return 1
  fi
  
  # Define thresholds
  local min_instruction_coverage=70
  local min_branch_coverage=60
  
  # Extract coverage metrics
  local instruction_coverage=$(awk -F, '{if(NR>1) {covered+=$5; missed+=$4}} END {print covered/(covered+missed)*100}' "$jacoco_csv")
  local branch_coverage=$(awk -F, '{if(NR>1) {covered+=$8; missed+=$7}} END {print covered/(covered+missed)*100}' "$jacoco_csv")
  
  # Display coverage metrics
  log_info "Coverage metrics:"
  printf "  ${CYAN}Instruction coverage:${NC} %.2f%%\n" "$instruction_coverage"
  printf "  ${CYAN}Branch coverage:${NC} %.2f%%\n" "$branch_coverage"
  
  # Check if coverage meets thresholds
  local coverage_ok=true
  
  if (( $(echo "$instruction_coverage < $min_instruction_coverage" | bc -l) )); then
    log_warn "Instruction coverage below threshold of $min_instruction_coverage%"
    coverage_ok=false
  fi
  
  if (( $(echo "$branch_coverage < $min_branch_coverage" | bc -l) )); then
    log_warn "Branch coverage below threshold of $min_branch_coverage%"
    coverage_ok=false
  fi
  
  if [[ "$coverage_ok" == "false" ]]; then
    log_warn "Coverage below thresholds"
    return 1
  fi
  
  log_info "Coverage meets thresholds"
  return 0
}

# Show help message
show_help() {
  cat << EOF
HireSync Quality Checks

Usage: ./run.sh verify [options]

Options:
  --checks=<checks>    Comma-separated list of checks to run
                       Available checks: style, spotbugs, pmd, sonar, coverage
  --auto-fix           Automatically fix issues when possible
  --help, -h           Show this help message

Examples:
  ./run.sh verify
  ./run.sh verify --checks=style,spotbugs
  ./run.sh verify --auto-fix
EOF
}

# Parse command line arguments
parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --checks=*)
        CHECKS="${1#*=}"
        shift
        ;;
      --auto-fix)
        AUTO_FIX=true
        shift
        ;;
      --help|-h)
        show_help
        exit 0
        ;;
      *)
        log_error "Unknown option: $1"
        show_help
        exit 1
        ;;
    esac
  done
}

# Main function
main() {
  # Parse command line arguments
  parse_args "$@"
  
  # Run quality checks
  check_quality
  
  return $?
}

# Run main function if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi