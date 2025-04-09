set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.local.yaml"
DEVTOOLS_CONTAINER="hiresync-devtools"

# Load environment variables
if [ -f "$PROJECT_ROOT/.env" ]; then
  export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check Docker is running
if ! docker info &>/dev/null; then
  echo "Error: Docker is not running. Please start Docker and try again."
  exit 1
fi

# Start containers quietly
echo "Starting Docker containers..."
docker compose -f "$COMPOSE_FILE" up -d --quiet-pull 2>/dev/null

# Start Spring Boot application with optimizations
echo "Starting Spring Boot application..."
docker exec -it "$DEVTOOLS_CONTAINER" bash -c 'cd /app && MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven=WARN" mvn -B -q spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dlogging.level.org.springframework.boot.context.config=ERROR \
  -Dlogging.level.org.springframework.core.env=ERROR \
  -Dspring.binder.autoCreateTopics=false \
  -Dspring.cloud.bootstrap.enabled=false \
  -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8 -XX:TieredStopAtLevel=1 -Xverify:none -XX:+TieredCompilation -XX:+UseParallelGC -XX:+UseStringDeduplication -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  -Dfile.encoding=UTF-8'