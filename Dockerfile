FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven configuration files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY settings.xml .

# Download dependencies (cached if pom.xml doesn't change)
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests -B

# Second stage: runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Create user for running the application
RUN groupadd -r spring && useradd -r -g spring spring

# Copy JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose the application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"] 