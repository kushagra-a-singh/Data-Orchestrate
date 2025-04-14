@echo off
setlocal enabledelayedexpansion

echo Starting Kafka...

:: Kill any existing Java processes (Kafka and Zookeeper)
taskkill /F /IM java.exe /T >nul 2>&1

:: Clean up Zookeeper data
if exist "D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0\data\zookeeper" (
    echo Cleaning up Zookeeper data...
    rmdir /S /Q "D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0\data\zookeeper"
)

:: Clean up Kafka logs
if exist "D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0\logs" (
    echo Cleaning up Kafka logs...
    rmdir /S /Q "D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0\logs"
)

:: Create necessary directories
mkdir "D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0\data\zookeeper"
mkdir "D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0\logs"

:: Start Zookeeper
echo Starting Zookeeper...
start "Zookeeper" cmd /c "cd /d D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0 && .\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties"

:: Wait for Zookeeper to start
echo Waiting for Zookeeper to start...
timeout /t 15 /nobreak >nul

:: Start Kafka
echo Starting Kafka...
start "Kafka" cmd /c "cd /d D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0 && .\bin\windows\kafka-server-start.bat .\config\server.properties"

:: Wait for Kafka to start
echo Waiting for Kafka to start...
timeout /t 20 /nobreak >nul

:: Verify Kafka is running
echo Verifying Kafka is running...
cd /d D:\Kushagra\Programming\MPJ-MP\Data-Orchestrate\kafka_2.13-3.7.0
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 >nul 2>&1
if errorlevel 1 (
    echo Kafka broker is not responding. Waiting additional time...
    timeout /t 10 /nobreak >nul
    .\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Kafka broker is still not responding. Please check the Kafka logs.
        exit /b 1
    )
)

:: Create topics
echo Creating topics...
.\bin\windows\kafka-topics.bat --create --topic file-upload --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic file-replication --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic file-deletion --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic file-sync --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

echo Kafka and Zookeeper started successfully!
echo Topics created successfully!

:: Keep the window open
pause 