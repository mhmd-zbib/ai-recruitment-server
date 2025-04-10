FROM maven:3.9-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copy POM file first for dependency caching - this layer will only be rebuilt if the POM changes
COPY pom.xml settings.xml* ./

# Download dependencies separately (will be cached if POM doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application with specified profile and test skipping
ARG SPRING_PROFILES_ACTIVE=dev
ARG SKIP_TESTS=true
RUN mvn clean package \
    -DskipTests=${SKIP_TESTS} \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -Dmaven.test.skip=${SKIP_TESTS}

# Create a specific runtime image
FROM eclipse-temurin:21-jre-jammy AS runtime

# Set working directory 
WORKDIR /app

# Create non-root user for running the application
RUN groupadd -r hiresync && useradd -r -g hiresync hiresync

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=dev \
    APP_PORT=8080 \
    ACTUATOR_PORT=8081 \
    PROMETHEUS_PORT=9090 \
    TZ=UTC

# Copy only the built JAR file from the build stage
COPY --from=build /app/target/*.jar /app/app.jar

# Create log directory with appropriate permissions
RUN mkdir -p /app/logs \
    && chown -R hiresync:hiresync /app \
    && chmod -R 755 /app/logs

# Set security limits
RUN echo "hiresync soft nofile 65536" >> /etc/security/limits.conf \
    && echo "hiresync hard nofile 65536" >> /etc/security/limits.conf

# Switch to non-root user
USER hiresync

# Expose necessary ports 
EXPOSE ${APP_PORT} ${ACTUATOR_PORT} ${PROMETHEUS_PORT}

# Health check - check application health endpoint
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:${ACTUATOR_PORT}/actuator/health || exit 1

# JVM tuning parameters for containers
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/logs/", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dserver.port=${APP_PORT}", \
  "-Dmanagement.server.port=${ACTUATOR_PORT}", \
  "-jar", "/app/app.jar"] 