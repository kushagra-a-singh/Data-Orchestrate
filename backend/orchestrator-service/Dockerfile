FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/orchestrator-service.jar orchestrator-service.jar
ENTRYPOINT ["java", "-jar", "orchestrator-service.jar"]
