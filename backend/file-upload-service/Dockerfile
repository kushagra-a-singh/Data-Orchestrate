# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy parent POM and install it
COPY pom.xml /build/pom.xml
WORKDIR /build
RUN mvn install -N

# Copy common-utils module
COPY common-utils /build/common-utils

# Build common-utils first
WORKDIR /build/common-utils
RUN mvn clean install -DskipTests

# Copy service POM and source
COPY file-upload-service/pom.xml /build/file-upload-service/pom.xml
COPY file-upload-service/src /build/file-upload-service/src

# Build the service
WORKDIR /build/file-upload-service
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Add non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create upload directory and set permissions
RUN mkdir -p /app/uploads && \
    chown -R appuser:appuser /app

# Copy application files
COPY --from=builder /build/file-upload-service/target/file-upload-service-0.0.1-SNAPSHOT.jar app.jar
COPY file-upload-service/wait-for-it.sh /usr/local/bin/wait-for-it
RUN chmod +x /usr/local/bin/wait-for-it

# Set environment variables
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"
ENV SPRING_PROFILES_ACTIVE="prod"

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Expose port
EXPOSE 8081

# Start application
ENTRYPOINT ["wait-for-it", "kafka:9092", "--timeout=30", "--", "java", \
    "-jar", "app.jar", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "${JAVA_OPTS}"]
