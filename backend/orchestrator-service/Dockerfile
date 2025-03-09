FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/orchestrator-service-0.0.1-SNAPSHOT.jar orchestrator-service.jar
ENTRYPOINT ["java", "-jar", "orchestrator-service.jar"]
