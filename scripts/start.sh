#!/usr/bin/env bash
set -e

# Parse arguments
DEBUG_MODE=false
for arg in "$@"; do
  case $arg in
    --debug)
      DEBUG_MODE=true
      shift
      ;;
  esac
done

# Get the absolute path of the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.local.yaml"
DEVTOOLS_CONTAINER="hiresync-devtools"

# Detect Windows under Git Bash and fix path if needed
IS_WINDOWS=false
if [[ "$(uname -s)" == MINGW* ]] || [[ "$(uname -s)" == CYGWIN* ]]; then
  IS_WINDOWS=true
  if [ "$DEBUG_MODE" = true ]; then echo "Windows environment detected. Adjusting paths..."; fi
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
  if [ "$DEBUG_MODE" = true ]; then echo "Loaded environment variables from .env file"; fi
fi

# Check Docker is running
if ! docker info &>/dev/null; then
  echo "Error: Docker is not running. Please start Docker and try again."
  exit 1
fi

# Print debug info if requested
if [ "$DEBUG_MODE" = true ]; then
  echo "Debug mode enabled"
  echo "Current working directory: $(pwd)"
  echo "Project root: $PROJECT_ROOT"
  if [ "$IS_WINDOWS" = true ]; then
    echo "Docker-compatible project root: $PROJECT_ROOT_DOCKER"
  fi
  docker version
  docker ps
fi

# Check network connectivity
if [ "$DEBUG_MODE" = true ]; then
  echo "Checking network connectivity..."
  if ! ping -c 1 8.8.8.8 &>/dev/null && ! ping -n 1 8.8.8.8 &>/dev/null; then
    echo "Warning: Network connectivity issues detected. Maven might have trouble downloading dependencies."
  else
    echo "Network connectivity: OK"
  fi
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
  
  if [ "$DEBUG_MODE" = true ]; then
    # More verbose output in debug mode
    $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" up -d --remove-orphans
  else
    $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" up -d
  fi

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
if [ "$DEBUG_MODE" = true ]; then
  echo "Checking container mount points..."
  docker exec "$DEVTOOLS_CONTAINER" bash -c "ls -la / | grep app"
  docker exec "$DEVTOOLS_CONTAINER" bash -c "ls -la /app | grep -E 'pom.xml|src'"
  docker inspect "$DEVTOOLS_CONTAINER" | grep -A 10 Mounts
fi

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
if [ "$DEBUG_MODE" = true ]; then
  echo "Configuring Maven settings..."
fi

docker exec "$DEVTOOLS_CONTAINER" bash -c 'mkdir -p /root/.m2'
docker exec "$DEVTOOLS_CONTAINER" bash -c 'echo "<settings><mirrors><mirror><id>central-https</id><url>https://repo1.maven.org/maven2</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>" > /root/.m2/settings.xml'

# Start Spring Boot application with optimizations
echo "Starting Spring Boot application..."

# Build the Maven command based on debug mode
MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven=WARN -Djava.net.preferIPv4Stack=true"
MAVEN_CMD="cd /app && export MAVEN_OPTS=\"$MAVEN_OPTS\" && mvn -B spring-boot:run"

# Add debug options if in debug mode
if [ "$DEBUG_MODE" = true ]; then
  # Use more strategic logging instead of Maven -X which is too verbose
  LOG_LEVEL="DEBUG"
  SPRING_LOG_LEVEL="DEBUG"
  # Only show important Maven logs, not everything
  MAVEN_CMD="$MAVEN_CMD -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer=WARN"
  MAVEN_CMD="$MAVEN_CMD -Dorg.slf4j.simpleLogger.showDateTime=true"
else
  LOG_LEVEL="INFO"
  SPRING_LOG_LEVEL="INFO"
fi

MAVEN_CMD="$MAVEN_CMD -Dspring-boot.run.profiles=local"
MAVEN_CMD="$MAVEN_CMD -Dlogging.level.root=$LOG_LEVEL"
MAVEN_CMD="$MAVEN_CMD -Dlogging.level.com.zbib.hiresync=$SPRING_LOG_LEVEL"

# Focus logs on important areas when in debug mode
if [ "$DEBUG_MODE" = true ]; then
  # Log database operations but not SQL dumps
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.hibernate=$LOG_LEVEL"
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.hibernate.SQL=$LOG_LEVEL"
  # Tone down SQL parameter logging which can be excessive
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
  
  # Important Spring areas to debug
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.springframework.web=$LOG_LEVEL"
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.springframework.security=$LOG_LEVEL"
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.springframework.data=$LOG_LEVEL"
  
  # Tone down less important areas
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.springframework.context=INFO"
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.springframework.beans=INFO"
  MAVEN_CMD="$MAVEN_CMD -Dlogging.level.org.apache.catalina=INFO"
fi

MAVEN_CMD="$MAVEN_CMD -Dspring.binder.autoCreateTopics=false"
MAVEN_CMD="$MAVEN_CMD -Dspring.cloud.bootstrap.enabled=false"
MAVEN_CMD="$MAVEN_CMD -Dmaven.wagon.http.pool=false"
MAVEN_CMD="$MAVEN_CMD -Dmaven.wagon.httpconnectionManager.ttlSeconds=120"

# JVM arguments that improve startup time
JVM_ARGS="-Dfile.encoding=UTF-8 -XX:TieredStopAtLevel=1 -Xverify:none -XX:+TieredCompilation"
# JVM arguments for debug port
JVM_ARGS="$JVM_ARGS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
# Memory settings to avoid excessive memory usage
JVM_ARGS="$JVM_ARGS -XX:+UseParallelGC -XX:+UseStringDeduplication -Xms256m -Xmx512m"

MAVEN_CMD="$MAVEN_CMD -Dspring-boot.run.jvmArguments=\"$JVM_ARGS\""

if [ "$DEBUG_MODE" = true ]; then
  echo "Starting with enhanced logging..."
fi

docker exec -it "$DEVTOOLS_CONTAINER" bash -c "$MAVEN_CMD"