FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y netcat

COPY target/hiresync-1.0.0.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
