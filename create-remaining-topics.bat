@echo off
setlocal enabledelayedexpansion

:: Set Kafka home directory
set KAFKA_HOME=C:\kafka_2.12-3.9.0

echo Creating/Verifying Kafka topics...

:: Function to check Kafka connection with timeout
:check_kafka
echo Checking Kafka connection...
set /a attempts=0
:retry_connection
set /a attempts+=1
%KAFKA_HOME%\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 > nul 2>&1
if errorlevel 1 (
    if !attempts! lss 30 (
        echo Kafka is not ready yet. Attempt !attempts! of 30. Waiting 5 seconds...
        timeout /t 5 /nobreak > nul
        goto retry_connection
    ) else (
        echo Error: Could not connect to Kafka after 30 attempts.
        echo Please make sure:
        echo 1. Kafka is running (run start-kafka.bat first)
        echo 2. No firewall is blocking port 9092
        echo 3. Kafka is properly installed at %KAFKA_HOME%
        pause
        exit /b 1
    )
)

:: List of required topics
set "TOPICS=file-upload-topic file-processing file-storage notifications file-processed-topic file-error-topic file-deletion file-status processing-request file-replication-topic file-deleted-topic"

:: Create each topic if it doesn't exist
for %%t in (%TOPICS%) do (
    echo.
    echo ============================================
    echo Processing topic: %%t
    echo ============================================
    
    :: First try to describe the topic
    echo Checking if topic exists...
    %KAFKA_HOME%\bin\windows\kafka-topics.bat --describe --topic %%t --bootstrap-server localhost:9092 > nul 2>&1
    if errorlevel 1 (
        echo Topic does not exist. Attempting to create...
        echo Creating topic with command: %KAFKA_HOME%\bin\windows\kafka-topics.bat --create --topic %%t --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1
        %KAFKA_HOME%\bin\windows\kafka-topics.bat --create --topic %%t --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1
        if errorlevel 1 (
            echo Error: Failed to create topic %%t
            echo Please check if:
            echo 1. Kafka is running properly
            echo 2. You have sufficient permissions
            echo 3. The topic name is valid
            echo.
            echo Current Kafka topics:
            %KAFKA_HOME%\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
            pause
            exit /b 1
        )
        echo Successfully created topic: %%t
    ) else (
        echo Topic already exists: %%t
    )
)

:: List all topics at the end
echo.
echo ============================================
echo Final list of all topics:
echo ============================================
%KAFKA_HOME%\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092

echo.
echo Topic creation/verification complete!
pause 