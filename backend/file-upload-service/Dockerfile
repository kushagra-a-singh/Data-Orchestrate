FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/file-upload-service-0.0.1-SNAPSHOT.jar file-upload-service.jar
ENTRYPOINT ["java", "-jar", "file-upload-service.jar"]
