#!/bin/bash

# Description: Prepares the CI environment for running tests and builds without Docker.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default values
CACHE_DEPS=${CACHE_DEPS:-true}
MVN_ARGS=${MAVEN_ARGS:-"-B -ntp"}

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-cache)
      CACHE_DEPS=false
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: ./hiresync ci-prepare [--no-cache]"
      exit 1
      ;;
  esac
done

echo "===== Preparing CI Environment ====="

# Check Java version
echo "Java version:"
java -version

# Check Maven version
echo "Maven version:"
mvn --version

# Setup application properties for CI
if [ ! -f "$PROJECT_ROOT/src/main/resources/application-ci.properties" ]; then
  echo "Creating CI-specific application properties..."
  
  cat > "$PROJECT_ROOT/src/main/resources/application-ci.properties" << EOF
# CI Environment specific properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
logging.level.root=WARN
logging.level.com.hiresync=INFO
EOF

  echo "CI application properties created"
fi

# Create necessary directories for reports and configs
mkdir -p config
mkdir -p target/reports

# Check if running in CI environment
if [ -n "$CI" ]; then
  echo "Running in CI environment"
  
  # Cache Maven dependencies if running in CI
  echo "Caching Maven dependencies..."
  mvn $MVN_ARGS dependency:go-offline
fi

# Format code automatically before validation
echo "Formatting code with Spotless..."
mvn spotless:apply $MVN_ARGS

echo "===== CI Environment Ready ====="
exit 0 