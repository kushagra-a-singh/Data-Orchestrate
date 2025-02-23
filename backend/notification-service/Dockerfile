FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/notification-service.jar notification-service.jar
ENTRYPOINT ["java", "-jar", "notification-service.jar"]
