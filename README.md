# Data Orchestrate

A distributed file management system for seamless file synchronization, processing, and storage across multiple devices. Data Orchestrate is designed for environments where files need to be uploaded, processed (including text extraction from PDFs), and synchronized in real-time between several devices, with robust metadata tracking and notification support.

## Architecture Overview

Data Orchestrate is composed of multiple microservices, each responsible for a specific aspect of the system. Services communicate via Kafka and persist metadata in MongoDB Atlas (cloud-hosted). The frontend is built with JavaFX, providing a user-friendly GUI for uploading and monitoring files.

## Project Structure

```
.
├── backend/
│   ├── common-utils/          # Shared utilities and components (e.g., DeviceIdentifier)
│   ├── file-upload-service/   # Handles file uploads and metadata storage
│   ├── storage-service/       # Manages file storage and retrieval
│   ├── processing-service/    # Processes uploaded files (e.g., PDF text extraction)
│   ├── notification-service/  # Handles real-time notifications (WebSocket, etc.)
│   └── orchestrator-service/  # Coordinates file replication and device sync
└── frontend/
    └── gui-app/              # JavaFX desktop interface for users
```

## Services

- **file-upload-service:** Accepts file uploads, stores metadata in MongoDB Atlas, and triggers downstream processing.
- **processing-service:** Processes incoming files (e.g., extracts text from PDFs), updates metadata, and forwards processed files for storage.
- **storage-service:** Manages persistent storage of files in the data directory, handles retrieval requests, and ensures files are available for sync.
- **orchestrator-service:** Coordinates file replication and synchronization between devices, ensuring consistency across the distributed network.
- **notification-service:** Sends real-time notifications to the frontend and other services (e.g., WebSocket updates for file status).
- **common-utils:** Shared codebase for utilities such as device identification and common configuration.

## Device Configuration

Devices participating in synchronization are described in `backend/common-utils/src/main/resources/devices.json`:

```
[
  {
    "name": "Kushagra",
    "ip": "10.23.78.68",
    "port": "8081",
    "file_upload_port": "8081",
    "processing_port": "8083",
    "notification_port": "8084",
    "storage_port": "8085"
  },
  {
    "name": "Anil Cerejo",
    "ip": "10.23.48.160",
    "port": "8081",
    "file_upload_port": "8081",
    "processing_port": "8083",
    "notification_port": "8084",
    "storage_port": "8085"
  },
  {
    "name": "Third",
    "ip": "192.168.1.9",
    "port": "8081",
    "file_upload_port": "8081",
    "processing_port": "8083",
    "notification_port": "8084",
    "storage_port": "8085"
  }
]
```

Each device entry contains the device name, IP, and service ports. Update this file to add or remove devices in your distributed network.

## How It Works

1. **File Upload:** Users upload files via the JavaFX frontend. The file-upload-service receives the file and stores metadata in MongoDB Atlas.
2. **Processing:** The processing-service picks up new files, performs necessary processing (e.g., text extraction), and updates the database.
3. **Storage:** Processed files are stored in the storage-service, making them available for retrieval and synchronization.
4. **Synchronization:** The orchestrator-service ensures files are replicated to all connected devices, maintaining consistency.
5. **Notifications:** Users receive real-time status updates via the notification-service and frontend.

## Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- MongoDB Atlas account (cloud-hosted)
- Kafka

## Environment Variables

Sample `.env`:
```
MONGODB_URI=mongodb+srv://username:password@cluster.example.mongodb.net/?retryWrites=true&w=majority&appName=example-cluster
SPRING_PROFILES_ACTIVE=prod
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

UPLOAD_DIR=./data/uploads
STORAGE_DIR=./data/storage
PROCESSED_DIR=./data/processed
SYNC_DIR=./data/sync

FILE_UPLOAD_PORT=8081
PROCESSING_PORT=8082
ORCHESTRATOR_PORT=8083
NOTIFICATION_PORT=8084
STORAGE_PORT=8085

spring.data.mongodb.uri=mongodb+srv://username:password@cluster.example.mongodb.net/?retryWrites=true&w=majority&appName=example-cluster
spring.data.mongodb.database=file-orchestrator
MONGODB_DATABASE=file-orchestrator

DEVICE_NAME=Kushagra
```

## Setup

1. Start Kafka:
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties
```

2. (Optional) List Kafka topics to verify setup:
```bash
./kafka-topics.bat --list --bootstrap-server localhost:9092
```

3. Build the backend:
```bash
cd backend
mvn clean install
```

4. Start the services (in separate terminals):
```bash
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

5. Start the JavaFX frontend:
```bash
cd frontend/gui-app
mvn javafx:run
```

## Features

- Automatic device identification and registration
- File upload with metadata tracking
- File processing (including PDF text extraction)
- Real-time notifications and status updates
- Cross-device file synchronization and replication
- Robust logging and monitoring

## Configuration

Each service has its own `application.properties` file where you can configure:
- Server ports
- Kafka topics
- MongoDB Atlas connections
- File storage directories

## Testing

1. Upload a file through the JavaFX GUI
2. Check the logs to see the file being processed
3. The file should be available on all connected devices

## Troubleshooting

1. Check service logs for errors
2. Verify Kafka is running
3. Ensure all services can connect to Kafka and MongoDB Atlas
4. Check file permissions for upload and storage directories
