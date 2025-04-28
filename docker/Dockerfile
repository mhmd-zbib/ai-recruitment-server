# Stage 1: Build and test the application
FROM eclipse-temurin:21-jdk AS builder

# Set working directory
WORKDIR /app

# Install Maven and curl for healthchecks
RUN apt-get update && \
    apt-get install -y maven curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy POM file first for dependency resolution
COPY pom.xml .

# Use Maven directly instead of the wrapper
# Download dependencies (separate layer for caching)
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src src

# Build arguments with defaults
ARG SPRING_PROFILES_ACTIVE=prod
ARG DEBUG_MODE=false
ARG SPRINGDOC_ENABLED=true
ARG SWAGGER_UI_ENABLED=true
ARG SKIP_TESTS=true

# Build the application
# Tests can be skipped based on the build argument
RUN mvn package \
    ${SKIP_TESTS:+-DskipTests} \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -Ddebug.mode=${DEBUG_MODE} \
    -Dspringdoc.enabled=${SPRINGDOC_ENABLED} \
    -Dswagger.ui.enabled=${SWAGGER_UI_ENABLED}

# Stage 2: Create a minimal runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime

# Set working directory
WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Add a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set default environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport" \
    DB_HOST=postgres_db \
    DB_PORT=5432 \
    DB_NAME=hiresync \
    DB_USERNAME=postgres \
    SERVER_PORT=8080 \
    JPA_DDL_AUTO=update \
    JPA_SHOW_SQL=false \
    TZ=UTC

# Create directory for logs with proper permissions
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

# Switch to non-root user
USER appuser

# Expose the port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Stage 3: Create a test image that includes Maven for running tests
FROM builder AS test

# Set environment variables for testing
ENV SPRING_PROFILES_ACTIVE=test \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport" \
    DB_HOST=test-db \
    DB_PORT=5432 \
    DB_NAME=hiresync_test \
    DB_USERNAME=test_user \
    DB_PASSWORD=test_password \
    SERVER_PORT=8080 \
    JPA_DDL_AUTO=create-drop \
    JPA_SHOW_SQL=true

# Create directory for test results
RUN mkdir -p /app/target/surefire-reports

# Default command to run tests
CMD ["mvn", "test"]