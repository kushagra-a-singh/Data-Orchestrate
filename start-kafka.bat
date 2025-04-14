@echo off
setlocal enabledelayedexpansion

echo Starting Kafka services...

:: Clean up any existing Java processes
echo Cleaning up existing Java processes...
for /f "tokens=2" %%p in ('tasklist /fi "imagename eq java.exe" /nh') do (
        taskkill /F /PID %%p

)

:: Start Zookeeper
echo Starting Zookeeper...
start "Zookeeper" cmd /c "C:\kafka_2.12-3.9.0\bin\windows\zookeeper-server-start.bat C:\kafka_2.12-3.9.0\config\zookeeper.properties"


:: Wait for Zookeeper to start
echo Waiting for Zookeeper to start...
timeout /t 10 /nobreak > nul

:: Start Kafka
echo Starting Kafka...
start "Kafka" cmd /c "C:\kafka_2.12-3.9.0\bin\windows\kafka-server-start.bat C:\kafka_2.12-3.9.0\config\server.properties"


:: Wait for Kafka to start
echo Waiting for Kafka to start...
timeout /t 10 /nobreak > nul

:: Create topics if they don't exist
echo Creating topics...

:: List of all required topics
set "TOPICS=raw-data processed-data file-upload-topic file-processing file-storage notifications file-processed-topic file-error-topic file-deletion file-status processing-request file-replication-topic file-deleted-topic file-sync-topic"

:: Create each topic if it doesn't exist
for %%t in (%TOPICS%) do (
    C:\kafka_2.12-3.9.0\bin\windows\kafka-topics.bat --describe --topic %%t --bootstrap-server localhost:9092 > nul 2>&1
    if errorlevel 1 (
        echo Creating: %%t
                C:\kafka_2.12-3.9.0\bin\windows\kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic %%t

    )
)

echo.
echo Kafka services started successfully!
echo.
echo Available topics:
C:\kafka_2.12-3.9.0\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092

pause