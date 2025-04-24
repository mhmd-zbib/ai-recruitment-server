#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Get the git root directory
GIT_ROOT=$(git rev-parse --show-toplevel)
cd "$GIT_ROOT" || exit 1

# Check if we're in a Docker environment
IN_DOCKER=false
if [ -f "docker/docker-compose.local.yaml" ] && command -v docker &> /dev/null; then
  # Check if devtools container is running
  if docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
    IN_DOCKER=true
    echo -e "${CYAN}🐳 Using Docker container for checks${NC}"
  fi
fi

print_header() {
  echo -e "\n${CYAN}================================================${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}================================================${NC}"
}

print_success() {
  echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
  echo -e "${RED}❌ $1${NC}"
}

print_warning() {
  echo -e "${YELLOW}⚠️  $1${NC}"
}

run_command() {
  local cmd="$1"
  
  if [ "$IN_DOCKER" = true ]; then
    docker exec hiresync-devtools $cmd
  else
    eval "$cmd"
  fi
}

run_check() {
  local command="$1"
  local description="$2"
  local fail_on_error="${3:-true}"
  
  print_header "Running $description"
  
  run_command "$command"
  local exit_code=$?
  
  if [ $exit_code -eq 0 ]; then
    print_success "$description passed!"
    return 0
  else
    print_error "$description failed!"
    if [ "$fail_on_error" = true ]; then
      print_error "Fix the issues before continuing."
      exit 1
    fi
    return 1
  fi
}

check_docker() {
  if ! command -v docker &> /dev/null; then
    print_warning "Docker is not installed or not in PATH. Running locally."
    return 1
  fi
  
  if ! docker ps &> /dev/null; then
    print_warning "Docker is not running or you don't have permissions. Running locally."
    return 1
  fi
  
  if ! docker ps --format '{{.Names}}' | grep -q "hiresync-devtools"; then
    print_warning "HireSync devtools container is not running. Start it with: docker-compose -f docker/docker-compose.local.yaml up -d"
    return 1
  fi
  
  return 0
}

start_docker_env() {
  if [ "$IN_DOCKER" = false ] && [ -f "docker/docker-compose.local.yaml" ]; then
    print_header "Checking Docker Environment"
    
    if check_docker; then
      IN_DOCKER=true
      print_success "Using existing Docker environment"
    else
      print_warning "Would you like to start the Docker environment? (y/n)"
      read -r response
      if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_header "Starting Docker Environment"
        docker-compose -f docker/docker-compose.local.yaml up -d
        if [ $? -eq 0 ]; then
          IN_DOCKER=true
          print_success "Docker environment started"
        else
          print_error "Failed to start Docker environment. Running locally."
        fi
      else
        print_warning "Running locally without Docker"
      fi
    fi
  fi
}

check_all() {
  print_header "Running All Code Quality Checks"
  
  # Clean to ensure we build and check everything
  run_command "mvn clean" >/dev/null 2>&1
  
  # Checkstyle - Force checking all files
  run_check "mvn -P ci -Dskip.checks=false checkstyle:check -Dcheckstyle.skip=false -Dcheckstyle.includeTestSourceDirectory=true" "Checkstyle"
  
  # PMD - Force checking all files
  run_check "mvn -P ci -Dskip.checks=false pmd:check -Dpmd.skip=false -Dpmd.analysisCache=false" "PMD"
  
  # SpotBugs - Force checking all files
  run_check "mvn -P ci -Dskip.checks=false spotbugs:check -Dspotbugs.skip=false -Dspotbugs.effort=Max" "SpotBugs" false
  
  # Dependency Check - Force checking all dependencies
  run_check "mvn -P ci -Dskip.checks=false dependency-check:check -Ddependency-check.skip=false -Ddependency-check.failBuildOnCVSS=11" "Dependency Check" false
  
  print_success "All code quality checks completed!"
}

check_specific() {
  local check_type="$1"
  
  # Clean to ensure we build and check everything
  run_command "mvn clean" >/dev/null 2>&1
  
  case "$check_type" in
    "checkstyle")
      run_check "mvn -P ci -Dskip.checks=false checkstyle:check -Dcheckstyle.skip=false -Dcheckstyle.includeTestSourceDirectory=true" "Checkstyle"
      ;;
    "pmd")
      run_check "mvn -P ci -Dskip.checks=false pmd:check -Dpmd.skip=false -Dpmd.analysisCache=false" "PMD"
      ;;
    "spotbugs")
      run_check "mvn -P ci -Dskip.checks=false spotbugs:check -Dspotbugs.skip=false -Dspotbugs.effort=Max" "SpotBugs"
      ;;
    "dependency")
      run_check "mvn -P ci -Dskip.checks=false dependency-check:check -Ddependency-check.skip=false -Ddependency-check.failBuildOnCVSS=11" "Dependency Check"
      ;;
    *)
      print_error "Unknown check type: $check_type"
      echo "Available checks: checkstyle, pmd, spotbugs, dependency"
      exit 1
      ;;
  esac
}

show_help() {
  echo "Usage: $0 [command]"
  echo ""
  echo "Commands:"
  echo "  all         Run all code quality checks (default)"
  echo "  checkstyle  Run checkstyle check only"
  echo "  pmd         Run PMD check only"
  echo "  spotbugs    Run SpotBugs check only"
  echo "  dependency  Run dependency check only"
  echo "  help        Show this help message"
  echo ""
  echo "Note: All checks run on the entire project, not just changed files."
}

# Main execution
start_docker_env

command=${1:-"all"}

case "$command" in
  "all")
    check_all
    ;;
  "checkstyle"|"pmd"|"spotbugs"|"dependency")
    check_specific "$command"
    ;;
  "help")
    show_help
    ;;
  *)
    print_error "Unknown command: $command"
    show_help
    exit 1
    ;;
esac

exit 0 