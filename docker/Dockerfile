FROM maven:3.9-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copy pom.xml and download dependencies (for layer caching)
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline

# Copy source code
COPY src ./src
COPY config ./config
COPY db ./db

# Build application
ARG SPRING_PROFILES_ACTIVE=prod
ARG SKIP_TESTS=false
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package \
    -DskipTests=${SKIP_TESTS} \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -Dmaven.test.skip=${SKIP_TESTS}

# Runtime image
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Create non-root user for running the application
RUN groupadd -r hiresync && useradd -r -g hiresync hiresync

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    APP_PORT=8080 \
    ACTUATOR_PORT=8081 \
    PROMETHEUS_PORT=9090 \
    TZ=UTC

# Copy built jar file
COPY --from=build /app/target/*.jar /app/app.jar

# Create directories and set permissions
RUN mkdir -p /app/logs /app/config \
    && chown -R hiresync:hiresync /app

# Switch to non-root user
USER hiresync

# Expose ports for external access
EXPOSE ${APP_PORT} ${ACTUATOR_PORT} ${PROMETHEUS_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:${ACTUATOR_PORT}/actuator/health || exit 1

# Entry point
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/logs/", \
  "-Dserver.port=${APP_PORT}", \
  "-Dmanagement.server.port=${ACTUATOR_PORT}", \
  "-jar", "/app/app.jar"] 