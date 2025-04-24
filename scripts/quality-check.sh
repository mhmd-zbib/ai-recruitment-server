#!/bin/bash
set -e

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print header
print_header() {
  echo -e "\n${BLUE}========== $1 ==========${NC}\n"
}

# Print success message
print_success() {
  echo -e "${GREEN}✓ SUCCESS: $1${NC}"
}

# Print error message
print_error() {
  echo -e "${RED}✗ ERROR: $1${NC}"
}

# Print warning message
print_warning() {
  echo -e "${YELLOW}⚠ WARNING: $1${NC}"
}

# Determine if running inside a Docker container
if [ -f /.dockerenv ] || grep -q 'docker\|lxc' /proc/1/cgroup; then
    IN_DOCKER=true
    # Get container name if possible
    CONTAINER_NAME=$(hostname)
    if [ -n "$CONTAINER_NAME" ]; then
        print_warning "Running in Docker container: $CONTAINER_NAME"
    else
        print_warning "Running in Docker container"
    fi
else
    IN_DOCKER=false
fi

# Set Maven command
MVN_CMD="mvn"
if [ "$IN_DOCKER" = false ]; then
  if [ -f "./mvnw" ]; then
    MVN_CMD="./mvnw"
  elif docker ps | grep -q hiresync-devtools; then
    MVN_CMD="docker exec -it hiresync-devtools mvn"
  fi
fi

# Parse arguments
SKIP_FORMAT=false
SKIP_SPOTBUGS=false
SKIP_CHECKSTYLE=false
SKIP_DEPCHECK=false
FIX_MODE=false

for arg in "$@"; do
  case $arg in
    --skip-format)
      SKIP_FORMAT=true
      shift
      ;;
    --skip-spotbugs)
      SKIP_SPOTBUGS=true
      shift
      ;;
    --skip-checkstyle)
      SKIP_CHECKSTYLE=true
      shift
      ;;
    --skip-depcheck)
      SKIP_DEPCHECK=true
      shift
      ;;
    --fix)
      FIX_MODE=true
      shift
      ;;
    --help)
      echo "Usage: $0 [options]"
      echo "Options:"
      echo "  --skip-format     Skip code formatting with Spotless"
      echo "  --skip-spotbugs   Skip SpotBugs checks"
      echo "  --skip-checkstyle Skip Checkstyle checks"
      echo "  --skip-depcheck   Skip OWASP Dependency Check"
      echo "  --fix             Fix issues when possible (currently only formatting)"
      echo "  --help            Show this help message"
      exit 0
      ;;
  esac
done

# Common Maven options
MVN_OPTS="-P lint -Dskip.tests=true"

# Step 1: Code formatting with Spotless
if [ "$SKIP_FORMAT" = false ]; then
  print_header "AUTO-FORMATTING CODE (SPOTLESS)"
  
  # Always apply formatting first, then check to verify
  $MVN_CMD $MVN_OPTS spotless:apply -Dskip.checks=false
  if [ $? -eq 0 ]; then
    print_success "Spotless formatting applied"
  else
    print_error "Spotless formatting failed"
    exit 1
  fi
else
  print_warning "Skipping code formatting"
fi

# Step 2: SpotBugs for bug detection
if [ "$SKIP_SPOTBUGS" = false ]; then
  print_header "RUNNING SPOTBUGS CHECKS"
  
  $MVN_CMD $MVN_OPTS spotbugs:check -Dskip.checks=false
  if [ $? -eq 0 ]; then
    print_success "SpotBugs checks passed"
  else
    print_error "SpotBugs checks failed"
    echo -e "${YELLOW}See target/spotbugsXml.xml for details${NC}"
    print_warning "Check src/main/resources/spotbugs-exclude.xml to customize exclusions"
    exit 1
  fi
else
  print_warning "Skipping SpotBugs checks"
fi

# Step 3: Checkstyle for coding standards
if [ "$SKIP_CHECKSTYLE" = false ]; then
  print_header "RUNNING CHECKSTYLE CHECKS"
  
  $MVN_CMD $MVN_OPTS checkstyle:check -Dskip.checks=false
  if [ $? -eq 0 ]; then
    print_success "Checkstyle checks passed"
  else
    print_error "Checkstyle checks failed"
    echo -e "${YELLOW}See target/checkstyle-result.xml for details${NC}"
    print_warning "Check src/main/resources/checkstyle-rules.xml to customize rules"
    exit 1
  fi
else
  print_warning "Skipping Checkstyle checks"
fi

# Step 4: OWASP Dependency Check for security vulnerabilities
if [ "$SKIP_DEPCHECK" = false ]; then
  print_header "RUNNING DEPENDENCY CHECK"
  
  $MVN_CMD $MVN_OPTS dependency-check:check -Dskip.checks=false
  if [ $? -eq 0 ]; then
    print_success "Dependency check passed"
  else
    print_error "Dependency check failed"
    echo -e "${YELLOW}See target/dependency-check-report.html for details${NC}"
    print_warning "Check src/main/resources/dependency-check-suppressions.xml to customize suppressions"
    exit 1
  fi
else
  print_warning "Skipping dependency check"
fi

# Print summary
print_header "QUALITY CHECK SUMMARY"
if [ "$SKIP_FORMAT" = false ]; then
  print_success "Spotless formatting"
fi
if [ "$SKIP_SPOTBUGS" = false ]; then
  print_success "SpotBugs checks"
fi
if [ "$SKIP_CHECKSTYLE" = false ]; then
  print_success "Checkstyle checks"
fi
if [ "$SKIP_DEPCHECK" = false ]; then
  print_success "Dependency check"
fi

print_success "All checks completed successfully!"
echo -e "\nRun '$0 --help' for more options"
