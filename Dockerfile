FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Determine which env vars to load based on SPRING_PROFILES_ACTIVE
ARG SPRING_PROFILES_ACTIVE=prod
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

# Default port to expose
ENV PORT=8080

# Expose the port and run the application
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "/app/app.jar"] 