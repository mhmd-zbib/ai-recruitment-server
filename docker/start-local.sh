#!/usr/bin/env bash
set -e

# This script is a helper for starting the app directly from the docker directory
# Useful for Windows environments where path issues might occur

# Define container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Check if containers are already running - NEVER STOP THEM
if docker ps | grep -q "$DEVTOOLS_CONTAINER"; then
  echo "Containers are already running. Will NOT stop them."
else
  # ONLY start containers if they're not already running
  echo "Starting Docker containers..."
  docker compose -f docker-compose.local.yaml up -d

  # Ensure the container has started
  echo "Waiting for containers to be ready..."
  sleep 5

  # Check if container is running
  if ! docker ps | grep -q "$DEVTOOLS_CONTAINER"; then
    echo "Error: Container $DEVTOOLS_CONTAINER is not running. Check docker logs for details."
    docker compose -f docker-compose.local.yaml logs
    exit 1
  fi

  echo "Containers are ready."
fi

# Print container mount details for debugging
echo "Checking container mount points..."
docker exec "$DEVTOOLS_CONTAINER" bash -c "ls -la / | grep app"
docker exec "$DEVTOOLS_CONTAINER" bash -c "ls -la /app | grep -E 'pom.xml|src'"

# Check if pom.xml exists in container
if ! docker exec "$DEVTOOLS_CONTAINER" bash -c "[ -f /app/pom.xml ]"; then
  echo "Error: pom.xml not found in container."
  echo "Contents of /app directory:"
  docker exec "$DEVTOOLS_CONTAINER" bash -c "ls -la /app"
  echo "Check that volumes are correctly mounted in docker-compose.local.yaml"
  exit 1
fi

# Start Spring Boot application
echo "Starting Spring Boot application..."
docker exec -it "$DEVTOOLS_CONTAINER" bash -c '
  cd /app
  if [ -f "pom.xml" ]; then 
    echo "Found pom.xml, starting application..."
    export MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven=WARN" 
    mvn -B spring-boot:run \
      -Dspring-boot.run.profiles=local \
      -Dlogging.level.org.springframework.boot.context.config=ERROR \
      -Dlogging.level.org.springframework.core.env=ERROR \
      -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8 -XX:TieredStopAtLevel=1 -Xverify:none -XX:+TieredCompilation -XX:+UseParallelGC -XX:+UseStringDeduplication -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
  else
    echo "Error: pom.xml not found in /app directory."
    echo "Current working directory: $(pwd)"
    exit 1
  fi
' 