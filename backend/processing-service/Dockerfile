FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/processing-service-0.0.1-SNAPSHOT.jar processing-service.jar
ENTRYPOINT ["java", "-jar", "processing-service.jar"]
