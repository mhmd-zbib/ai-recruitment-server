FROM eclipse-temurin:21-jdk AS builder

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml for dependency resolution
COPY pom.xml .

# Download dependencies (separate layer for caching)
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src src

# Build the application
ARG SPRING_PROFILES_ACTIVE=prod
ARG DEBUG_MODE=false
ARG SPRINGDOC_ENABLED=false
ARG SWAGGER_UI_ENABLED=false

# Pass build-time arguments to Maven
# Skip tests here as they will be run separately in test environment
RUN mvn package -DskipTests \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -Ddebug.mode=${DEBUG_MODE} \
    -Dspringdoc.enabled=${SPRINGDOC_ENABLED} \
    -Dswagger.ui.enabled=${SWAGGER_UI_ENABLED}

# Runtime image
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Add a non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set default environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms512m -Xmx1024m" \
    DB_HOST=postgres_db \
    DB_PORT=5432 \
    DB_NAME=hiresync \
    DB_USERNAME=postgres \
    SERVER_PORT=8080 \
    JPA_DDL_AUTO=update \
    JPA_SHOW_SQL=false

# Create directory for logs with proper permissions
RUN mkdir -p /app/logs && chown -R appuser:appuser /app/logs

# Switch to non-root user
USER appuser

# Expose the port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]