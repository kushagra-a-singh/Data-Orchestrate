# Data Orchestrate

A distributed file management system that enables file synchronization and processing across multiple devices.

## Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- MongoDB
- Kafka
- Node.js and npm (for frontend)

## Project Structure

```
.
├── backend/
│   ├── common-utils/          # Shared utilities and components
│   ├── file-upload-service/   # Handles file uploads
│   ├── storage-service/       # Manages file storage
│   ├── processing-service/    # Processes uploaded files
│   ├── notification-service/  # Handles notifications
│   └── orchestrator-service/  # Coordinates services
└── frontend/
    └── gui-app/              # Web interface
```

## Setup

1. Start MongoDB:
```bash
mongod
```

2. Start Kafka:
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties
```

3. Build the backend:
```bash
cd backend
mvn clean install
```

4. Start the services:
```bash
# In separate terminals
cd backend/file-upload-service
mvn spring-boot:run

cd backend/storage-service
mvn spring-boot:run

cd backend/processing-service
mvn spring-boot:run

cd backend/notification-service
mvn spring-boot:run

cd backend/orchestrator-service
mvn spring-boot:run
```

5. Start the frontend:
```bash
cd frontend/gui-app
npm install
npm start
```

## Features

- Automatic device identification
- File upload and storage
- File processing and text extraction
- Real-time notifications
- Cross-device file synchronization

## Configuration

Each service has its own `application.properties` file where you can configure:
- Server ports
- Kafka topics
- MongoDB connections
- File storage directories

## Testing

1. Upload a file through the GUI
2. Check the logs to see the file being processed
3. The file should be available on all connected devices

## Troubleshooting

1. Check service logs for errors
2. Verify MongoDB and Kafka are running
3. Ensure all services can connect to Kafka and MongoDB
4. Check file permissions for upload and storage directories