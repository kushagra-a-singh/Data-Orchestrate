FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/file-upload-service.jar file-upload-service.jar
ENTRYPOINT ["java", "-jar", "file-upload-service.jar"]
