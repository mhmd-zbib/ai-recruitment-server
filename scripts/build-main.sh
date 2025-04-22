#!/bin/bash
set -e

echo "🔨 Compiling main code without running tests..."

# Get the absolute path of the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEVTOOLS_CONTAINER="hiresync-devtools"

# Check if Docker is running
if ! docker info &>/dev/null; then
  echo "Error: Docker is not running. Please start Docker and try again."
  exit 1
fi

# Check if containers are already running
if ! docker ps | grep -q "$DEVTOOLS_CONTAINER"; then
  echo "Error: Container $DEVTOOLS_CONTAINER is not running. Run ./scripts/start.sh first."
  exit 1
fi

# Compile main sources only
echo "Compiling main sources..."
docker exec "$DEVTOOLS_CONTAINER" bash -c "cd /app && mvn clean compile -DskipTests"

echo "✅ Main compilation complete." 