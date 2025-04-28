FROM eclipse-temurin:21-jdk AS builder

# Set working directory
WORKDIR /app

# Copy maven wrapper, pom.xml, and mvnw files
COPY ./mvnw .
COPY ./.mvn ./.mvn
COPY ./pom.xml .

# Make the mvnw script executable
RUN chmod +x mvnw

# Download dependencies (separate layer for caching)
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY ./src ./src

# Build the application
ARG SPRING_PROFILES_ACTIVE=prod
ARG DEBUG_MODE=false
ARG SPRINGDOC_ENABLED=false
ARG SWAGGER_UI_ENABLED=false

# Pass build-time arguments to Maven
RUN ./mvnw package -DskipTests \
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

# Set non-sensitive environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV DB_HOST=postgres_db
ENV DB_PORT=5432
ENV DB_NAME=hiresync
ENV DB_USERNAME=postgres

# These should be passed at runtime, not built into the image
# We're removing DB_PASSWORD from the Dockerfile for security reasons

# Switch to non-root user
USER appuser

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "${JAVA_OPTS}", "-jar", "/app/app.jar"]