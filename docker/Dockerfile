# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Install build dependencies and set up non-root user
RUN apk add --no-cache maven curl && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Copy Maven configuration first for better layer caching
COPY --chown=appuser:appgroup pom.xml mvnw* ./
COPY --chown=appuser:appgroup .mvn ./.mvn

# Download dependencies as a separate layer
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY --chown=appuser:appgroup src ./src
COPY --chown=appuser:appgroup config ./config
COPY --chown=appuser:appgroup db ./db

# Switch to non-root user for build
USER appuser

# Build the application
RUN ./mvnw package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add labels for better maintainability
LABEL maintainer="HireSync Team" \
      description="HireSync AI Recruitment Platform" \
      version="1.0"

# Install runtime dependencies and create non-root user
RUN apk add --no-cache curl tzdata && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Copy the application
COPY --from=build --chown=appuser:appgroup /app/target/*.jar /app/app.jar
COPY --chown=appuser:appgroup db/init.sql /app/db/
COPY --chown=appuser:appgroup docker/entrypoint.sh /app/

# Ensure scripts are executable
RUN chmod +x /app/entrypoint.sh

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 8080
EXPOSE 5005

# Set sensible JVM defaults
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xms256m -Xmx1024m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE="prod" \
    TZ="UTC"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/app/entrypoint.sh"] 