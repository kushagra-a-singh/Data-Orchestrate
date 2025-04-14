@echo off
setlocal enabledelayedexpansion

echo Starting Data Orchestration Services...

echo Checking Java installation...
java -version
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    exit /b 1
)
echo Java check passed.

echo Checking Maven installation...
mvn -version
if %errorlevel% neq 0 (
    echo Error: Maven is not installed or not in PATH
    exit /b 1
)
echo Maven check passed.

echo Checking current directory...
echo Current directory: %CD%
echo.

echo Checking Kafka...
echo Running netstat command...
netstat -ano | findstr ":9092"
set "kafka_running=%errorlevel%"
echo Kafka check result: %kafka_running%
if %kafka_running% neq 0 (
    echo Error: Kafka is not running on port 9092
    echo Please run start-kafka.bat first
    exit /b 1
)
echo Kafka check passed.

echo Creating necessary directories...
if not exist "data\uploads" mkdir "data\uploads"
if not exist "data\processed" mkdir "data\processed"
if not exist "data\storage" mkdir "data\storage"
echo Directories created.

echo Starting backend services...

echo Checking backend directory structure...
if not exist "backend" (
    echo Error: backend directory not found
    exit /b 1
)
echo Backend directory exists.

echo Starting Orchestrator Service...
if not exist "backend\orchestrator-service" (
    echo Error: orchestrator-service directory not found
    exit /b 1
)
start "Orchestrator Service" cmd /k "cd backend\orchestrator-service && echo Starting Orchestrator Service... && mvn spring-boot:run"

echo Starting Notification Service...
if not exist "backend\notification-service" (
    echo Error: notification-service directory not found
    exit /b 1
)
start "Notification Service" cmd /k "cd backend\notification-service && echo Starting Notification Service... && mvn spring-boot:run"

echo Starting Processing Service...
if not exist "backend\processing-service" (
    echo Error: processing-service directory not found
    exit /b 1
)
start "Processing Service" cmd /k "cd backend\processing-service && echo Starting Processing Service... && mvn spring-boot:run"

echo Starting File Upload Service...
if not exist "backend\file-upload-service" (
    echo Error: file-upload-service directory not found
    exit /b 1
)
start "File Upload Service" cmd /k "cd backend\file-upload-service && echo Starting File Upload Service... && mvn spring-boot:run"

echo Starting Storage Service...
if not exist "backend\storage-service" (
    echo Error: storage-service directory not found
    exit /b 1
)
start "Storage Service" cmd /k "cd backend\storage-service && echo Starting Storage Service... && mvn spring-boot:run"

echo Waiting for services to start...
timeout /t 30 /nobreak

echo Starting JavaFX frontend...
if not exist "frontend\gui-app" (
    echo Error: gui-app directory not found
    exit /b 1
)
start "JavaFX Frontend" cmd /k "cd frontend\gui-app && echo Starting JavaFX Frontend... && mvn javafx:run"

echo.
echo Services started:
echo - Orchestrator Service: http://localhost:8080
echo - Notification Service: http://localhost:8084
echo - Processing Service: http://localhost:8083
echo - File Upload Service: http://localhost:8081
echo - Storage Service: http://localhost:8085
echo - JavaFX Frontend: Started
echo.
echo Press Ctrl+C in each window to stop the services 