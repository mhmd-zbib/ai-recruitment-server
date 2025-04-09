#!/bin/bash
set -e

# Get directories and setup logging
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
if [ -f "$SCRIPT_DIR/../utils/logging.sh" ]; then source "$SCRIPT_DIR/../utils/logging.sh"; else
  log_info() { echo -e "[INFO] $1"; }; log_error() { echo -e "[ERROR] $1"; }; log_success() { echo -e "[SUCCESS] $1"; }
fi

# Default configuration
TEST_TYPE="all"; TEST_ENV="auto"; TEST_PROFILE="test"; BUILD_IMAGE=false; KEEP_CONTAINERS=false
GENERATE_REPORTS=true; MAVEN_OPTS="-B -ntp"; TEST_ARGS=""
DEVTOOLS_CONTAINER="hiresync-devtools"
DOCKER_COMPOSE_FILE="${PROJECT_ROOT}/docker/docker-compose.test.yaml"

# Parse arguments (with backward compatibility)
while [[ $# -gt 0 ]]; do
  case "$1" in
    --unit) TEST_TYPE="unit"; shift ;;
    --integration) TEST_TYPE="integration"; shift ;;
    --type=*) TEST_TYPE="${1#*=}"; shift ;;
    --env=*) TEST_ENV="${1#*=}"; shift ;;
    --profile=*) TEST_PROFILE="${1#*=}"; shift ;;
    --container) TEST_ENV="container"; shift ;;
    --build-image) BUILD_IMAGE=true; shift ;;
    --keep-containers) KEEP_CONTAINERS=true; shift ;;
    --no-reports) GENERATE_REPORTS=false; shift ;;
    --maven-opts=*) MAVEN_OPTS="${1#*=}"; shift ;;
    --args=*) TEST_ARGS="${1#*=}"; shift ;;
    --help|-h) echo "Usage: ./scripts/core/test.sh [--type=unit|integration|e2e|all] [--env=local|container|docker]"; exit 0 ;;
    *) log_error "Unknown option: $1"; exit 1 ;;
  esac
done

# Auto-detect environment
if [ "$TEST_ENV" = "auto" ]; then
  if [ -n "$CI" ]; then TEST_ENV="local"; 
  elif docker ps --format '{{.Names}}' 2>/dev/null | grep -q "$DEVTOOLS_CONTAINER"; then TEST_ENV="container";
  elif command -v docker-compose >/dev/null && [ -f "$DOCKER_COMPOSE_FILE" ]; then TEST_ENV="docker";
  else TEST_ENV="local"; fi
  log_info "Auto-detected environment: $TEST_ENV"
fi

# Create Maven command based on test type
case "$TEST_TYPE" in
  unit) TEST_CMD="mvn test -Dtest=\"*Test\" -DexcludedGroups=\"integration,e2e\" $MAVEN_OPTS" ;;
  integration) TEST_CMD="mvn verify -Dgroups=\"integration\" -DskipUnitTests=true $MAVEN_OPTS" ;;
  e2e) TEST_CMD="mvn verify -Dgroups=\"e2e\" -DskipUnitTests=true $MAVEN_OPTS" ;;
  all|*) TEST_CMD="mvn verify $MAVEN_OPTS" ;;
esac
TEST_CMD="$TEST_CMD -Dspring.profiles.active=$TEST_PROFILE"
[ "$GENERATE_REPORTS" = true ] && TEST_CMD="$TEST_CMD -Djacoco.skip=false -Dsurefire-report.skip=false" || TEST_CMD="$TEST_CMD -Djacoco.skip=true -Dsurefire-report.skip=true"
[ -n "$TEST_ARGS" ] && TEST_CMD="$TEST_CMD $TEST_ARGS"

# Setup test environment
mkdir -p "$PROJECT_ROOT/target/surefire-reports" "$PROJECT_ROOT/target/failsafe-reports"
test_props="${PROJECT_ROOT}/src/test/resources/application-test.properties"
if [ ! -f "$test_props" ] || ! grep -q "^jwt.secret=" "$test_props"; then
  mkdir -p "$(dirname "$test_props")"
  echo "jwt.secret=test-secret-key-with-minimum-length-of-32-characters" > "$test_props"
fi

# Execute tests based on environment
log_info "Running $TEST_TYPE tests in $TEST_ENV environment"
case "$TEST_ENV" in
  docker)
    # Build image if needed
    if [ "$BUILD_IMAGE" = true ]; then
      export REGISTRY=${REGISTRY:-ghcr.io}; export GITHUB_ACTOR=${GITHUB_ACTOR:-local}
      export IMAGE_NAME=${IMAGE_NAME:-hiresync}; export IMAGE_TAG=${IMAGE_TAG:-ci-latest}
      docker build -t "${REGISTRY}/${GITHUB_ACTOR}/${IMAGE_NAME}:${IMAGE_TAG}" -f "${PROJECT_ROOT}/docker/Dockerfile" "${PROJECT_ROOT}"
    fi
    # Start containers and run tests
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d postgres app-builder
    docker-compose -f "$DOCKER_COMPOSE_FILE" exec -T postgres pg_isready -U hiresync -d testdb
    # Wait for builder to complete
    while docker-compose -f "$DOCKER_COMPOSE_FILE" ps | grep -q app-builder | grep -q "Up"; do sleep 2; done
    # Run tests
    docker-compose -f "$DOCKER_COMPOSE_FILE" run --rm -e SPRING_PROFILES_ACTIVE=$TEST_PROFILE test-runner /bin/bash -c "cd /app && $TEST_CMD"
    EXIT_CODE=$?
    # Copy reports and cleanup
    docker ps -a | grep -q hiresync-test-runner && {
      docker cp hiresync-test-runner:/app/target/surefire-reports/. "${PROJECT_ROOT}/target/surefire-reports/" 2>/dev/null || true
      docker cp hiresync-test-runner:/app/target/failsafe-reports/. "${PROJECT_ROOT}/target/failsafe-reports/" 2>/dev/null || true
    }
    [ "$KEEP_CONTAINERS" != "true" ] && docker-compose -f "$DOCKER_COMPOSE_FILE" down -v
    ;;
  container)
    # Run in dev container
    if ! docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
      log_error "Dev container not running! Start with: docker-compose -f docker/docker-compose.local.yaml up -d"
      exit 1
    fi
    docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && $TEST_CMD"
    EXIT_CODE=$?
    ;;
  local)
    # Run locally
    (cd "$PROJECT_ROOT" && eval "$TEST_CMD")
    EXIT_CODE=$?
    ;;
esac

[ $EXIT_CODE -eq 0 ] && log_success "Tests completed successfully" || { log_error "Tests failed with exit code $EXIT_CODE"; exit $EXIT_CODE; } 