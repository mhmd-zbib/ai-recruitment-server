#!/usr/bin/env bash
set -e

# Get the absolute path of the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.local.yaml"
DEVTOOLS_CONTAINER="hiresync-devtools"

# Detect Windows under Git Bash and fix path if needed
IS_WINDOWS=false
if [[ "$(uname -s)" == MINGW* ]] || [[ "$(uname -s)" == CYGWIN* ]]; then
  IS_WINDOWS=true
  echo "Windows environment detected. Adjusting paths..."
  # Convert Windows paths to Docker-compatible paths
  PROJECT_ROOT_DOCKER=$(echo "$PROJECT_ROOT" | sed 's/^\([a-zA-Z]\):/\/\1/' | sed 's/\\/\//g')
  # Set working directory to docker directory
  cd "$PROJECT_ROOT/docker"
else
  # Linux/Mac - standard path
  PROJECT_ROOT_DOCKER="$PROJECT_ROOT"
  cd "$PROJECT_ROOT/docker"  # Always work from docker directory for consistency
fi

# Load environment variables
if [ -f "$PROJECT_ROOT/.env" ]; then
  export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check Docker is running
if ! docker info &>/dev/null; then
  echo "Error: Docker is not running. Please start Docker and try again."
  exit 1
fi

# Print working directory for debugging
echo "Current working directory: $(pwd)"
echo "Project root: $PROJECT_ROOT"
if [ "$IS_WINDOWS" = true ]; then
  echo "Docker-compatible project root: $PROJECT_ROOT_DOCKER"
fi

# Check network connectivity
echo "Checking network connectivity..."
if ! ping -c 1 8.8.8.8 &>/dev/null && ! ping -n 1 8.8.8.8 &>/dev/null; then
  echo "Warning: Network connectivity issues detected. Maven might have trouble downloading dependencies."
fi

# Check if containers are already running - NEVER STOP THEM
if docker ps | grep -q "$DEVTOOLS_CONTAINER"; then
  echo "Containers are already running. Will NOT stop them."
else
  # ONLY start containers if they're not already running
  echo "Starting Docker containers..."
  
  # Use the appropriate docker-compose command based on Docker version
  if docker compose version &>/dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
  else
    DOCKER_COMPOSE_CMD="docker-compose"
  fi
  
  $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" up -d

  # Ensure the container has started
  echo "Waiting for containers to be ready..."
  sleep 5

  # Check if container is running
  if ! docker ps | grep -q "$DEVTOOLS_CONTAINER"; then
    echo "Error: Container $DEVTOOLS_CONTAINER is not running. Check docker logs for details."
    $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" logs
    exit 1
  fi
  
  echo "Docker containers are ready."
fi

# Print container mount details for debugging
echo "Checking container mount points..."
docker exec "$DEVTOOLS_CONTAINER" bash -c "ls -la / | grep app"
docker exec "$DEVTOOLS_CONTAINER" bash -c "ls -la /app | grep -E 'pom.xml|src'"

# Print more diagnostics if pom.xml isn't found
if ! docker exec "$DEVTOOLS_CONTAINER" bash -c "[ -f /app/pom.xml ]"; then
  echo "Warning: pom.xml not found. Running detailed diagnostics..."
  docker exec "$DEVTOOLS_CONTAINER" bash -c "ls -la /app"
  docker exec "$DEVTOOLS_CONTAINER" bash -c "mount | grep app || echo 'No mount info found'"
  
  # Try to determine what's wrong with the mount
  echo "Checking project directory structure..."
  ls -la "$PROJECT_ROOT" | grep -E 'pom.xml|src'
  
  echo "Error: Unable to find pom.xml in container. Please check Docker volume mounts."
  exit 1
fi

# Configure Maven settings for better network handling
echo "Configuring Maven settings..."
docker exec "$DEVTOOLS_CONTAINER" bash -c 'mkdir -p /root/.m2'
docker exec "$DEVTOOLS_CONTAINER" bash -c 'echo "<settings><mirrors><mirror><id>central-https</id><url>https://repo1.maven.org/maven2</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>" > /root/.m2/settings.xml'

# Start Spring Boot application with optimizations
echo "Starting Spring Boot application..."
docker exec -it "$DEVTOOLS_CONTAINER" bash -c '
  cd /app
  if [ -f "pom.xml" ]; then 
    echo "Found pom.xml, starting application..."
    export MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven=WARN -Djava.net.preferIPv4Stack=true" 
    mvn -B spring-boot:run \
      -Dspring-boot.run.profiles=local \
      -Dlogging.level.org.springframework.boot.context.config=ERROR \
      -Dlogging.level.org.springframework.core.env=ERROR \
      -Dspring.binder.autoCreateTopics=false \
      -Dspring.cloud.bootstrap.enabled=false \
      -Dmaven.wagon.http.pool=false \
      -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
      -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8 -XX:TieredStopAtLevel=1 -Xverify:none -XX:+TieredCompilation -XX:+UseParallelGC -XX:+UseStringDeduplication -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
  else
    echo "Error: pom.xml not found in /app directory."
    echo "Contents of current directory:"
    ls -la .
    echo "Current working directory: $(pwd)"
    exit 1
  fi
'