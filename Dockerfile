FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace/app

# Add labels
LABEL org.opencontainers.image.source="https://github.com/moezbib/hiresync"
LABEL org.opencontainers.image.description="HireSync Recruitment API"
LABEL org.opencontainers.image.licenses="MIT"
LABEL org.opencontainers.image.vendor="HireSync"
LABEL org.opencontainers.image.title="HireSync API Server"

# Build arguments for versioning
ARG VERSION="1.0.0"
ARG BUILD_DATE
ARG VCS_REF

# Additional labels for image metadata
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.revision="${VCS_REF}"

# Copy only the maven wrapper and pom.xml first to leverage Docker cache
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./
COPY pom.xml ./

# Make the maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies as a separate step to take advantage of Docker's caching
# This step will be cached as long as the pom.xml file doesn't change
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Runtime stage
FROM eclipse-temurin:17-jre-alpine AS runtime

# Add non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy application from build stage
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Install curl for health checks and any security patches
RUN apk --no-cache add curl tzdata && \
    apk upgrade --no-cache && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app/logs

# Set timezone and other environment variables
ENV TZ=UTC \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:MinRAMPercentage=50.0 -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Define additional runtime environment variables
ARG VERSION="1.0.0"
ENV APP_VERSION=${VERSION}

# Change ownership to non-root user and switch to that user
RUN chown -R appuser:appgroup /app
USER appuser

# Define health check with improved parameters
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run the application with exec form for proper signal handling
ENTRYPOINT ["java", "-cp", "app:app/lib/*", "com.zbib.hiresync.HireSyncApplication"] 