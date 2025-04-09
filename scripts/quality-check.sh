#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="$PROJECT_ROOT/target/logs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="$LOG_DIR/quality-check_$TIMESTAMP.log"

# Colorized output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Progress spinner variables
spin=('-' '\' '|' '/')
i=0

# Set up logging
setup_logging() {
  mkdir -p "$LOG_DIR"
  touch "$LOG_FILE"
  echo "===============================================" >> "$LOG_FILE"
  echo "  HireSync Quality Check - $(date)" >> "$LOG_FILE"
  echo "===============================================" >> "$LOG_FILE"
  echo "" >> "$LOG_FILE"
  echo "🔍 Log file created at: $LOG_FILE"
}

# Header function
print_header() {
  echo -e "\n${BLUE}${BOLD}=====================================================================${NC}"
  echo -e "${BLUE}${BOLD}  $1${NC}"
  echo -e "${BLUE}${BOLD}=====================================================================${NC}\n"
  echo "===== $1 =====" >> "$LOG_FILE"
  echo "" >> "$LOG_FILE"
}

# Log function
log() {
  local level=$1
  local message=$2
  local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
  echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
  
  case $level in
    INFO)
      echo -e "${GREEN}$message${NC}"
      ;;
    WARN)
      echo -e "${YELLOW}$message${NC}"
      ;;
    ERROR)
      echo -e "${RED}$message${NC}"
      ;;
    *)
      echo "$message"
      ;;
  esac
}

# Handle errors
handle_error() {
  log "ERROR" "🛑 $1"
  echo -e "\n${RED}${BOLD}ERROR: $1${NC}"
  echo "Script failed at $(date)" >> "$LOG_FILE"
  exit 1
}

# Display a spinner while a command is running
spin_until_finished() {
  local pid=$1
  local message=${2:-"Processing..."}
  
  echo -n "$message "
  log "INFO" "Started: $message" 
  
  while kill -0 $pid 2>/dev/null; do
    echo -ne "\b${spin[$i]}"
    i=$(( (i+1) % 4 ))
    sleep 0.1
  done
  
  echo -e "\b${GREEN}✓${NC}"
  log "INFO" "Completed: $message"
  
  wait $pid
  return $?
}

# Set up logging
setup_logging

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
  handle_error "Docker is not installed. Please install Docker first."
fi

# Check if Docker Compose is installed
if ! docker compose version &> /dev/null; then
  handle_error "Docker Compose is not installed or not in the PATH."
fi

log "INFO" "Quality check starting in directory: $PROJECT_ROOT"
cd "$PROJECT_ROOT" || handle_error "Failed to change directory to project root"

# Check if devtools container is running
ensure_devtools_running() {
  print_header "Ensuring Docker environment is running"
  
  # Check if Docker daemon is running
  if ! docker info &>/dev/null; then
    handle_error "Docker daemon is not running. Please start Docker and try again."
  fi
  
  log "INFO" "Checking if Docker containers are already running"
  if ! docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
    log "INFO" "Starting Docker environment using docker-compose.local.yaml"
    docker compose -f docker/docker-compose.local.yaml up -d 2>&1 | tee -a "$LOG_FILE" || handle_error "Failed to start Docker environment"
    log "INFO" "Docker environment started successfully"
    
    # Give the containers a moment to fully initialize
    log "INFO" "Waiting for services to initialize..."
    sleep 5
  else
    log "INFO" "Docker environment is already running"
  fi
  
  # Verify container is healthy
  if ! docker inspect -f '{{.State.Running}}' hiresync-devtools 2>/dev/null | grep -q "true"; then
    handle_error "Container hiresync-devtools is not in a running state"
  fi
  
  log "INFO" "✅ Docker environment is ready"
}

# Run docker exec command with logging
run_in_docker() {
  log "INFO" "Executing in container: $@"
  docker exec hiresync-devtools $@ 2>&1 | tee -a "$LOG_FILE"
  return ${PIPESTATUS[0]}
}

# Get version info
get_version_info() {
  log "INFO" "Getting version information from container"
  
  # Check Java version in container
  JAVA_VERSION=$(docker exec hiresync-devtools java -version 2>&1 | awk -F '"' '/version/ {print $2}')
  log "INFO" "Java version: $JAVA_VERSION"
  
  # Check Maven version in container
  MVN_VERSION=$(docker exec hiresync-devtools mvn --version | head -n 1)
  log "INFO" "Maven version: $MVN_VERSION"
  
  echo -e "Java version: ${GREEN}${JAVA_VERSION:-"Unknown"}${NC}"
  echo -e "Maven version: ${GREEN}${MVN_VERSION:-"Unknown"}${NC}\n"
}

# Run all quality checks
run_quality_checks() {
  ensure_devtools_running
  
  print_header "Running Code Quality Checks for HireSync"
  FAILED=false
  start_time=$(date +%s)
  
  # Get version info
  get_version_info
  
  # Create reports directory inside container
  log "INFO" "Creating reports directory"
  run_in_docker mkdir -p target/reports
  
  # Maven clean
  print_header "Maven Clean"
  log "INFO" "Running Maven Clean"
  run_in_docker mvn clean || handle_error "Maven clean failed"
  
  # Extract current profile
  ACTIVE_PROFILE="${1:-local}"
  if [ "$1" == "--with-security" ]; then
    ACTIVE_PROFILE="local"
  fi
  log "INFO" "Using profile: $ACTIVE_PROFILE"
  echo -e "Using profile: ${GREEN}${ACTIVE_PROFILE}${NC}\n"
  
  # Checkstyle
  print_header "Running Checkstyle"
  log "INFO" "Running Checkstyle check"
  run_in_docker mvn checkstyle:check -P ${ACTIVE_PROFILE} -Dcheckstyle.outputFile=target/reports/checkstyle-result.txt || {
    log "WARN" "Checkstyle check failed!"
    log "WARN" "See detailed report at: target/reports/checkstyle-result.txt"
    echo -e "${RED}Checkstyle check failed!${NC}"
    echo -e "${YELLOW}See detailed report at: target/reports/checkstyle-result.txt${NC}"
    echo -e "${YELLOW}To fix styling issues, consider running: ./quality-check.sh checkstyle-fix${NC}"
    FAILED=true
  }
  
  # PMD
  print_header "Running PMD"
  log "INFO" "Running PMD check"
  run_in_docker mvn pmd:check -P ${ACTIVE_PROFILE} || {
    log "WARN" "PMD check failed!"
    log "WARN" "See detailed report at: target/pmd.xml"
    echo -e "${RED}PMD check failed!${NC}"
    echo -e "${YELLOW}See detailed report at: target/pmd.xml${NC}"
    
    # Print top 5 PMD issues if the check fails
    if [ -f "$PROJECT_ROOT/target/pmd.xml" ]; then
      echo -e "\n${YELLOW}Top 5 PMD issues:${NC}"
      grep -m 5 -A 2 "<violation" "$PROJECT_ROOT/target/pmd.xml" | sed 's/<[^>]*>//g' | sed '/^$/d' | sed 's/^[ \t]*/    /' | tee -a "$LOG_FILE"
      echo -e "${YELLOW}... and more. See full report for details.${NC}"
    fi
    
    FAILED=true
  }
  
  # SpotBugs
  print_header "Running SpotBugs"
  log "INFO" "Running SpotBugs check"
  run_in_docker mvn spotbugs:check -P ${ACTIVE_PROFILE} || {
    log "WARN" "SpotBugs check failed!"
    log "WARN" "See detailed report at: target/spotbugsXml.xml"
    echo -e "${RED}SpotBugs check failed!${NC}"
    echo -e "${YELLOW}See detailed report at: target/spotbugsXml.xml${NC}"
    FAILED=true
  }
  
  # Dependency Analysis
  print_header "Running Dependency Analysis"
  log "INFO" "Running dependency analysis"
  run_in_docker mvn dependency:analyze -DoutputXML=true -DoutputFile=target/reports/dependency-analysis.xml || {
    log "WARN" "Dependency analysis failed!"
    log "WARN" "See detailed report at: target/reports/dependency-analysis.xml"
    echo -e "${RED}Dependency analysis failed!${NC}"
    echo -e "${YELLOW}See detailed report at: target/reports/dependency-analysis.xml${NC}"
    FAILED=true
  }
  
  # OWASP Dependency Check (optional, can be slow)
  if [ "$1" == "--with-security" ]; then
    print_header "Running OWASP Dependency Check (this may take a while)"
    log "INFO" "Starting security scan..."
    echo -e "${YELLOW}Starting security scan...${NC}"
    
    # Create a separate log file for security scan
    SECURITY_LOG="$LOG_DIR/security-scan_$TIMESTAMP.log"
    
    # Run security check in the background to show spinner
    run_in_docker mvn org.owasp:dependency-check-maven:check > "$SECURITY_LOG" 2>&1 &
    PID=$!
    spin_until_finished $PID "Running security scan"
    
    wait $PID
    SCAN_RESULT=$?
    
    if [ $SCAN_RESULT -ne 0 ]; then
      log "WARN" "OWASP Dependency check failed!"
      log "WARN" "See detailed report at: target/dependency-check-report.html"
      echo -e "${RED}OWASP Dependency check failed!${NC}"
      echo -e "${YELLOW}See detailed report at: target/dependency-check-report.html${NC}"
      echo -e "${YELLOW}Detailed log: $SECURITY_LOG${NC}"
      FAILED=true
    else
      log "INFO" "Security scan completed successfully"
      echo -e "${GREEN}Security scan completed successfully.${NC}"
    fi
  fi
  
  # Test Compilation
  print_header "Compiling Test Classes"
  log "INFO" "Compiling test classes"
  run_in_docker mvn test-compile -P ${ACTIVE_PROFILE} || {
    log "WARN" "Test compilation failed!"
    echo -e "${RED}Test compilation failed!${NC}"
    FAILED=true
  }
  
  # Calculate execution time
  end_time=$(date +%s)
  execution_time=$((end_time - start_time))
  minutes=$((execution_time / 60))
  seconds=$((execution_time % 60))
  
  log "INFO" "Total execution time: ${minutes}m ${seconds}s"
  
  # Summary
  if [ "$FAILED" == "true" ]; then
    print_header "❌ Quality Checks Failed"
    log "ERROR" "One or more quality checks failed. Please fix the issues before committing your code."
    echo -e "${RED}One or more quality checks failed. Please fix the issues before committing your code.${NC}"
    echo -e "\nTotal execution time: ${minutes}m ${seconds}s"
    echo -e "\n${YELLOW}Run with specific checks to troubleshoot:${NC}"
    echo -e "  ./quality-check.sh pmd           # For PMD issues"
    echo -e "  ./quality-check.sh checkstyle    # For style issues"
    log "INFO" "Quality check process completed with errors"
    exit 1
  else
    print_header "✅ All Quality Checks Passed"
    log "INFO" "All quality checks passed successfully"
    echo -e "${GREEN}Your code meets all quality standards!${NC}"
    echo -e "\nTotal execution time: ${minutes}m ${seconds}s"
    log "INFO" "Quality check process completed successfully"
  fi
}

# Shut down Docker environment
stop_docker_env() {
  print_header "Shutting down Docker environment"
  log "INFO" "Stopping Docker containers"
  docker compose -f docker/docker-compose.local.yaml down | tee -a "$LOG_FILE" || handle_error "Failed to stop Docker environment"
  log "INFO" "Docker environment stopped successfully"
  echo -e "${GREEN}Docker environment stopped successfully${NC}"
}

# Print version info
print_version() {
  ensure_devtools_running
  
  log "INFO" "Displaying version information"
  echo -e "${BLUE}HireSync Quality Check Tool v1.0.0${NC}"
  echo -e "${BLUE}==========================================${NC}"
  get_version_info
  PROJECT_NAME=$(grep -m 1 "<artifactId>" "$PROJECT_ROOT/pom.xml" | sed 's/<[^>]*>//g' | tr -d ' \t\n\r')
  log "INFO" "Project: $PROJECT_NAME"
  echo -e "Project: ${PROJECT_NAME}"
  echo -e "==========================================${NC}"
}

# Log the command being executed
log "INFO" "Command: $0 $*"

# Check for specific tool to run
if [ "$1" == "checkstyle" ]; then
  ensure_devtools_running
  print_header "Running Checkstyle Only"
  log "INFO" "Running Checkstyle check only"
  run_in_docker mvn checkstyle:check -Dcheckstyle.outputFile=target/reports/checkstyle-result.txt
elif [ "$1" == "checkstyle-fix" ]; then
  ensure_devtools_running
  print_header "Running Checkstyle (non-failing)"
  log "INFO" "Running Checkstyle in non-failing mode"
  run_in_docker mvn checkstyle:check -Dcheckstyle.failOnViolation=false
elif [ "$1" == "pmd" ]; then
  ensure_devtools_running
  print_header "Running PMD Only"
  log "INFO" "Running PMD check only"
  run_in_docker mvn pmd:check
elif [ "$1" == "spotbugs" ]; then
  ensure_devtools_running
  print_header "Running SpotBugs Only"
  log "INFO" "Running SpotBugs check only"
  run_in_docker mvn spotbugs:check
elif [ "$1" == "dependencies" ]; then
  ensure_devtools_running
  print_header "Running Dependency Analysis Only"
  log "INFO" "Running dependency analysis only"
  run_in_docker mvn dependency:analyze -DoutputXML=true -DoutputFile=target/reports/dependency-analysis.xml
elif [ "$1" == "security" ]; then
  ensure_devtools_running
  print_header "Running Security Check Only"
  log "INFO" "Running security check only"
  run_in_docker mvn org.owasp:dependency-check-maven:check
elif [ "$1" == "compile" ]; then
  ensure_devtools_running
  print_header "Compiling Only"
  log "INFO" "Running compilation only"
  run_in_docker mvn compile
elif [ "$1" == "version" ] || [ "$1" == "-v" ]; then
  print_version
elif [ "$1" == "stop" ]; then
  stop_docker_env
elif [ "$1" == "logs" ]; then
  print_header "Viewing Logs"
  if [ -d "$LOG_DIR" ]; then
    echo "Available logs:"
    ls -lt "$LOG_DIR" | head -n 10 | awk '{print "  " $9}' | grep -v "^  $"
    echo -e "\nTo view a specific log: cat $LOG_DIR/logname"
  else
    echo "No logs found"
  fi
elif [ "$1" == "help" ] || [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
  echo -e "${BLUE}${BOLD}HireSync Quality Check Tool${NC}"
  echo ""
  echo "Usage: ./quality-check.sh [OPTION]"
  echo ""
  echo "Options:"
  echo "  (no option)      Run all checks except security scan"
  echo "  --with-security  Run all checks including security scan"
  echo "  checkstyle       Run only checkstyle"
  echo "  checkstyle-fix   Run checkstyle in non-failing mode"
  echo "  pmd              Run only PMD"
  echo "  spotbugs         Run only SpotBugs"
  echo "  dependencies     Run only dependency analysis"
  echo "  security         Run only security check"
  echo "  compile          Run only compilation check"
  echo "  stop             Stop Docker environment"
  echo "  logs             View available log files"
  echo "  version, -v      Show version information"
  echo "  help, --help, -h Display this help message"
  log "INFO" "Help message displayed"
  exit 0
else
  # Run all checks by default
  run_quality_checks "$1"
fi

log "INFO" "Script completed successfully" 