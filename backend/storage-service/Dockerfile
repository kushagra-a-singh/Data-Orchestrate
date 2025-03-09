FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/storage-service-0.0.1-SNAPSHOT.jar storage-service.jar
ENTRYPOINT ["java", "-jar", "storage-service.jar"]
