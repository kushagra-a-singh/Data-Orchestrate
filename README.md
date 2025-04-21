# Data Orchestrate

A distributed file management system for seamless file synchronization, processing and storage across multiple devices. Data Orchestrate is designed for environments where files need to be uploaded, processed (including text extraction from PDFs) and synchronized in real-time between several devices, with robust metadata tracking and notification support.

## Requirements

- Java 17 or higher
- Maven 3.8 or higher
- Kafka
- MongoDB Atlas account (cloud-hosted)
- (Optional) Docker & Docker Compose (for containerized deployments)
- (Optional) Kubernetes & Minikube (for local or cloud orchestration)

> **Note:** Docker, Kubernetes and Minikube deployment files are available in the `containerized` branch of this repository. Switch to that branch to access and use them.

## Containerization & Orchestration

Data Orchestrate supports containerized deployment using Docker and orchestration via Docker Compose and Kubernetes (including Minikube for local development).

### Docker & Docker Compose

- All core services (Kafka, Zookeeper, file-upload, processing, storage, notification, orchestrator) are containerized.
- Use the provided `deployment/docker-compose.yml` to spin up the full stack for local or development use.

![file_2025-04-18_16 22 56 1](https://github.com/user-attachments/assets/cf1c71ff-d480-48a2-98ec-c87570c982ab)


**To start everything with Docker Compose:**
```bash
docker-compose -f deployment/docker-compose.yml up --build -d
```
- This will build images (if needed), start all services and create a bridge network.
- Service healthchecks are included for robust startup.
- Data volumes are mapped for persistence.

**To stop and remove containers:**
```bash
docker-compose -f deployment/docker-compose.yml down
```

### Kubernetes & Minikube

- Kubernetes manifests for each service are provided in `deployment/kubernetes/`.
- Supports scaling, rolling updates, resource limits and liveness/readiness probes.
- Persistent volumes and secrets (e.g., MongoDB URI, SMTP credentials) are managed via YAML.

**To deploy on Minikube:**
1. Start Minikube:
   ```bash
   minikube start
   ```
2. Apply persistent volume (PVC) and secrets:
   ```bash
   kubectl apply -f deployment/kubernetes/data-pvc.yaml
   kubectl apply -f deployment/kubernetes/mongodb-secret.yaml
   ```
3. Deploy all services:
   ```bash
   kubectl apply -f deployment/kubernetes/
   ```
4. Expose services as needed (e.g., NodePort or Ingress for accessing from host).

**Notes:**
- Images are referenced as `deployment-<service>:latest` and must be built and loaded into your Minikube Docker environment:
  ```bash
  eval $(minikube docker-env)
  # Then build each image, e.g.:
  docker build -t deployment-file-upload-service backend/file-upload-service
  # Repeat for other services
  ```
- Update secrets and environment variables as per your environment.
- Prometheus annotations are included for monitoring.

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

- **file-upload-service:** Accepts file uploads, stores metadata in MongoDB Atlas and triggers downstream processing.
- **processing-service:** Processes incoming files (e.g., extracts text from PDFs), updates metadata and forwards processed files for storage.
- **storage-service:** Manages persistent storage of files in the data directory, handles retrieval requests and ensures files are available for sync.
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
2. **Processing:** The processing-service picks up new files, performs necessary processing (e.g., text extraction) and updates the database.
3. **Storage:** Processed files are stored in the storage-service, making them available for retrieval and synchronization.
4. **Synchronization:** The orchestrator-service ensures files are replicated to all connected devices, maintaining consistency.
5. **Notifications:** Users receive real-time status updates via the notification-service and frontend.

![file_2025-04-18_16 00 11 1](https://github.com/user-attachments/assets/8136fddb-7444-4af7-803d-c5eeea0785ae)


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

### 1. Kafka & MongoDB Atlas

- Ensure you have a running Kafka instance and a MongoDB Atlas cluster.

### 2. (Optional) Docker & Docker Compose (branch: `containerized`)

- Switch to the `containerized` branch to use Docker Compose files.
- Start all services:
  ```bash
  docker-compose -f deployment/docker-compose.yml up --build -d
  ```
- Stop and remove containers:
  ```bash
  docker-compose -f deployment/docker-compose.yml down
  ```

### 3. (Optional) Kubernetes & Minikube (branch: `containerized`)

- Switch to the `containerized` branch to use Kubernetes manifests.
- Start Minikube:
  ```bash
  minikube start
  ```
- Apply persistent volume and secrets:
  ```bash
  kubectl apply -f deployment/kubernetes/data-pvc.yaml
  kubectl apply -f deployment/kubernetes/mongodb-secret.yaml
  # (and smtp-secret.yaml if needed)
  ```
- Build and load images into Minikube:
  ```bash
  eval $(minikube docker-env)
  docker build -t deployment-file-upload-service backend/file-upload-service
  # Repeat for other services
  ```
- Deploy all services:
  ```bash
  kubectl apply -f deployment/kubernetes/
  ```
- Expose services as needed (NodePort/Ingress).

### 4. Manual (Non-containerized) Setup

1. Start Kafka:
    ```bash
    # Start Zookeeper
    bin/zookeeper-server-start.sh config/zookeeper.properties
    # Start Kafka
    bin/kafka-server-start.sh config/server.properties
    ```
2. (Optional) List Kafka topics:
    ```bash
    ./kafka-topics.bat --list --bootstrap-server localhost:9092
    ```
    ![file_2025-04-18_16 17 30 1](https://github.com/user-attachments/assets/e4c07ae9-e42a-48a3-b144-f65606eccf3f)

3. Build the backend:
    ```bash
    cd backend
    mvn clean install
    ```
4. Start the services (in separate terminals):
    ```bash
    cd backend/file-upload-service && mvn spring-boot:run
    cd backend/storage-service && mvn spring-boot:run
    cd backend/processing-service && mvn spring-boot:run
    cd backend/notification-service && mvn spring-boot:run
    cd backend/orchestrator-service && mvn spring-boot:run
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
