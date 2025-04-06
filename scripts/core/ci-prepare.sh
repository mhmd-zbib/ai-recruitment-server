#!/usr/bin/env bash

# Description: Prepares the CI environment for running tests and builds without Docker.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default values
CACHE_DEPS=${CACHE_DEPS:-true}
MVN_ARGS="-B -ntp"

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

# Ensure all dependencies are downloaded and cached
if [ "$CACHE_DEPS" = true ]; then
  echo "Resolving and caching dependencies..."
  mvn $MVN_ARGS dependency:go-offline
fi

# Install necessary tools
echo "Installing tools..."

# Set up environment variables if needed
echo "Setting up environment variables..."

# Create necessary directories
mkdir -p config

# Format code before validation (crucial for first-time CI runs)
echo "Formatting code to match style guide..."
mvn spotless:apply ${MVN_ARGS:-}

echo "===== CI Environment Ready ====="
exit 0 