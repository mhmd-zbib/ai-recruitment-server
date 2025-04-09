#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

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

# Header function
print_header() {
  echo -e "\n${BLUE}${BOLD}=====================================================================${NC}"
  echo -e "${BLUE}${BOLD}  $1${NC}"
  echo -e "${BLUE}${BOLD}=====================================================================${NC}\n"
}

# Handle errors
handle_error() {
  echo -e "\n${RED}${BOLD}ERROR: $1${NC}"
  exit 1
}

# Display a spinner while a command is running
spin_until_finished() {
  local pid=$1
  local message=${2:-"Processing..."}
  
  echo -n "$message "
  
  while kill -0 $pid 2>/dev/null; do
    echo -ne "\b${spin[$i]}"
    i=$(( (i+1) % 4 ))
    sleep 0.1
  done
  
  echo -e "\b${GREEN}✓${NC}"
  
  wait $pid
  return $?
}

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
  handle_error "Docker is not installed. Please install Docker first."
fi

# Check if Docker Compose is installed
if ! docker compose version &> /dev/null; then
  handle_error "Docker Compose is not installed or not in the PATH."
fi

cd "$PROJECT_ROOT" || handle_error "Failed to change directory to project root"

# Check if devtools container is running
ensure_devtools_running() {
  print_header "Ensuring Docker environment is running"
  
  if ! docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
    echo -e "${YELLOW}Starting Docker environment...${NC}"
    docker compose -f docker/docker-compose.local.yaml up -d || handle_error "Failed to start Docker environment"
    echo -e "${GREEN}Docker environment started successfully${NC}"
    
    # Give the containers a moment to fully initialize
    echo -e "${YELLOW}Waiting for services to initialize...${NC}"
    sleep 5
  else
    echo -e "${GREEN}Docker environment is already running${NC}"
  fi
}

# Run docker exec command
run_in_docker() {
  docker exec hiresync-devtools $@
}

# Get version info
get_version_info() {
  # Check Java version in container
  JAVA_VERSION=$(run_in_docker java -version 2>&1 | awk -F '"' '/version/ {print $2}')
  
  # Check Maven version in container
  MVN_VERSION=$(run_in_docker mvn --version | head -n 1)
  
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
  run_in_docker mkdir -p target/reports
  
  # Maven clean
  echo -e "${YELLOW}Running Maven Clean...${NC}"
  run_in_docker mvn clean -q || handle_error "Maven clean failed"
  
  # Extract current profile
  ACTIVE_PROFILE="${1:-local}"
  if [ "$1" == "--with-security" ]; then
    ACTIVE_PROFILE="local"
  fi
  echo -e "Using profile: ${GREEN}${ACTIVE_PROFILE}${NC}\n"
  
  # Checkstyle
  print_header "Running Checkstyle"
  run_in_docker mvn checkstyle:check -P ${ACTIVE_PROFILE} -Dcheckstyle.outputFile=target/reports/checkstyle-result.txt -q || {
    echo -e "${RED}Checkstyle check failed!${NC}"
    echo -e "${YELLOW}See detailed report at: target/reports/checkstyle-result.txt${NC}"
    echo -e "${YELLOW}To fix styling issues, consider running: ./quality-check.sh checkstyle-fix${NC}"
    FAILED=true
  }
  
  # PMD
  print_header "Running PMD"
  run_in_docker mvn pmd:check -P ${ACTIVE_PROFILE} -q || {
    echo -e "${RED}PMD check failed!${NC}"
    echo -e "${YELLOW}See detailed report at: target/pmd.xml${NC}"
    
    # Print top 5 PMD issues if the check fails
    if [ -f "$PROJECT_ROOT/target/pmd.xml" ]; then
      echo -e "\n${YELLOW}Top 5 PMD issues:${NC}"
      grep -m 5 -A 2 "<violation" "$PROJECT_ROOT/target/pmd.xml" | sed 's/<[^>]*>//g' | sed '/^$/d' | sed 's/^[ \t]*/    /'
      echo -e "${YELLOW}... and more. See full report for details.${NC}"
    fi
    
    FAILED=true
  }
  
  # SpotBugs
  print_header "Running SpotBugs"
  run_in_docker mvn spotbugs:check -P ${ACTIVE_PROFILE} -q || {
    echo -e "${RED}SpotBugs check failed!${NC}"
    echo -e "${YELLOW}See detailed report at: target/spotbugsXml.xml${NC}"
    FAILED=true
  }
  
  # Dependency Analysis
  print_header "Running Dependency Analysis"
  run_in_docker mvn dependency:analyze -DoutputXML=true -DoutputFile=target/reports/dependency-analysis.xml -q || {
    echo -e "${RED}Dependency analysis failed!${NC}"
    echo -e "${YELLOW}See detailed report at: target/reports/dependency-analysis.xml${NC}"
    FAILED=true
  }
  
  # OWASP Dependency Check (optional, can be slow)
  if [ "$1" == "--with-security" ]; then
    print_header "Running OWASP Dependency Check (this may take a while)"
    echo -e "${YELLOW}Starting security scan...${NC}"
    
    # Run security check in the background to show spinner
    run_in_docker mvn org.owasp:dependency-check-maven:check -q > /dev/null 2>&1 &
    PID=$!
    spin_until_finished $PID "Running security scan"
    
    wait $PID
    if [ $? -ne 0 ]; then
      echo -e "${RED}OWASP Dependency check failed!${NC}"
      echo -e "${YELLOW}See detailed report at: target/dependency-check-report.html${NC}"
      FAILED=true
    else
      echo -e "${GREEN}Security scan completed successfully.${NC}"
    fi
  fi
  
  # Test Compilation
  print_header "Compiling Test Classes"
  run_in_docker mvn test-compile -P ${ACTIVE_PROFILE} -q || {
    echo -e "${RED}Test compilation failed!${NC}"
    FAILED=true
  }
  
  # Calculate execution time
  end_time=$(date +%s)
  execution_time=$((end_time - start_time))
  minutes=$((execution_time / 60))
  seconds=$((execution_time % 60))
  
  # Summary
  if [ "$FAILED" == "true" ]; then
    print_header "❌ Quality Checks Failed"
    echo -e "${RED}One or more quality checks failed. Please fix the issues before committing your code.${NC}"
    echo -e "\nTotal execution time: ${minutes}m ${seconds}s"
    echo -e "\n${YELLOW}Run with specific checks to troubleshoot:${NC}"
    echo -e "  ./quality-check.sh pmd           # For PMD issues"
    echo -e "  ./quality-check.sh checkstyle    # For style issues"
    exit 1
  else
    print_header "✅ All Quality Checks Passed"
    echo -e "${GREEN}Your code meets all quality standards!${NC}"
    echo -e "\nTotal execution time: ${minutes}m ${seconds}s"
  fi
}

# Shut down Docker environment
stop_docker_env() {
  print_header "Shutting down Docker environment"
  docker compose -f docker/docker-compose.local.yaml down || handle_error "Failed to stop Docker environment"
  echo -e "${GREEN}Docker environment stopped successfully${NC}"
}

# Print version info
print_version() {
  ensure_devtools_running
  
  echo -e "${BLUE}HireSync Quality Check Tool v1.0.0${NC}"
  echo -e "${BLUE}==========================================${NC}"
  get_version_info
  echo -e "Project: $(grep -m 1 "<artifactId>" "$PROJECT_ROOT/pom.xml" | sed 's/<[^>]*>//g' | tr -d ' \t\n\r')"
  echo -e "==========================================${NC}"
}

# Check for specific tool to run
if [ "$1" == "checkstyle" ]; then
  ensure_devtools_running
  print_header "Running Checkstyle Only"
  run_in_docker mvn checkstyle:check -Dcheckstyle.outputFile=target/reports/checkstyle-result.txt
elif [ "$1" == "checkstyle-fix" ]; then
  ensure_devtools_running
  print_header "Running Checkstyle (non-failing)"
  run_in_docker mvn checkstyle:check -Dcheckstyle.failOnViolation=false
elif [ "$1" == "pmd" ]; then
  ensure_devtools_running
  print_header "Running PMD Only"
  run_in_docker mvn pmd:check
elif [ "$1" == "spotbugs" ]; then
  ensure_devtools_running
  print_header "Running SpotBugs Only"
  run_in_docker mvn spotbugs:check
elif [ "$1" == "dependencies" ]; then
  ensure_devtools_running
  print_header "Running Dependency Analysis Only"
  run_in_docker mvn dependency:analyze -DoutputXML=true -DoutputFile=target/reports/dependency-analysis.xml
elif [ "$1" == "security" ]; then
  ensure_devtools_running
  print_header "Running Security Check Only"
  run_in_docker mvn org.owasp:dependency-check-maven:check
elif [ "$1" == "compile" ]; then
  ensure_devtools_running
  print_header "Compiling Only"
  run_in_docker mvn compile
elif [ "$1" == "version" ] || [ "$1" == "-v" ]; then
  print_version
elif [ "$1" == "stop" ]; then
  stop_docker_env
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
  echo "  version, -v      Show version information"
  echo "  help, --help, -h Display this help message"
  exit 0
else
  # Run all checks by default
  run_quality_checks "$1"
fi 