FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/processing-service.jar processing-service.jar
ENTRYPOINT ["java", "-jar", "processing-service.jar"]
