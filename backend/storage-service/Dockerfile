FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/storage-service.jar storage-service.jar
ENTRYPOINT ["java", "-jar", "storage-service.jar"]
