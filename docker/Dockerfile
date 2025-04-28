FROM openjdk:21-jre-slim as runtime

WORKDIR /app

COPY /target/hiresync-1.0.0.jar /app/hiresync-1.0.0.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/hiresync-1.0.0.jar"]