@echo off
setlocal enabledelayedexpansion

echo Starting Kafka services...
echo Logs will be saved to logs directory

:: Create logs directory if it doesn't exist
if not exist "logs" mkdir logs

:: Clean up any existing Java processes (Kafka/Zookeeper)
echo Cleaning up existing Java processes...
for /f "tokens=2 delims=," %%p in ('tasklist /fi "imagename eq java.exe" /nh /fo csv ^| findstr /v "No"') do (
    echo Killing process PID: %%~p
    taskkill /F /PID %%~p
)

:: Start Zookeeper first
echo Starting Zookeeper...
start "Zookeeper" cmd /k "C:\kafka_2.12-3.9.0\bin\windows\zookeeper-server-start.bat C:\kafka_2.12-3.9.0\config\zookeeper.properties > logs\zookeeper.log 2>&1"

:: Wait for Zookeeper to start
echo Waiting for Zookeeper to start...
:wait_zk
timeout /t 2 >nul
C:\kafka_2.12-3.9.0\bin\windows\zookeeper-shell.bat localhost:2181 stat / 2>nul | find "Latency" >nul
if errorlevel 1 (
    echo Zookeeper not ready yet...
    goto wait_zk
)

:: Clean up Zookeeper metadata (fixes NodeExists error)
echo Cleaning Zookeeper metadata...
C:\kafka_2.12-3.9.0\bin\windows\zookeeper-shell.bat localhost:2181 deleteall /brokers/ids 2>nul
C:\kafka_2.12-3.9.0\bin\windows\zookeeper-shell.bat localhost:2181 deleteall /brokers/topics 2>nul

:: Start Zookeeper
echo Starting Zookeeper...
start "Zookeeper" cmd /k "C:\kafka_2.12-3.9.0\bin\windows\zookeeper-server-start.bat C:\kafka_2.12-3.9.0\config\zookeeper.properties > logs\zookeeper.log 2>&1"

:: Wait for Zookeeper to start by checking port 2181
:wait_zk
for /f "tokens=2" %%a in ('netstat -ano ^| findstr ":2181 "') do (
    set ZKPID=%%a
)
if not defined ZKPID (
    echo Waiting for Zookeeper to bind to port 2181...
    timeout /t 2 > nul
    goto wait_zk
)
echo Zookeeper is running on port 2181 (PID: %ZKPID%)

:: Set JVM memory parameters before starting Kafka
set KAFKA_HEAP_OPTS=-Xmx2G -Xms2G

:: Start Kafka - using /k to keep window open on error
echo Starting Kafka with 2GB heap memory...
start "Kafka" cmd /k "C:\kafka_2.12-3.9.0\bin\windows\kafka-server-start.bat C:\kafka_2.12-3.9.0\config\server.properties > logs\kafka.log 2>&1"

:: Wait for Kafka to start by checking port 9092 with timeout
set WAIT_COUNT=0
:wait_kafka
timeout /t 2 >nul
netstat -ano | findstr ":9092 " >nul
if errorlevel 1 (
    echo Waiting for Kafka to start (%WAIT_COUNT%/15 attempts)...
    set /a WAIT_COUNT+=1
    if %WAIT_COUNT% gtr 15 (
        echo ERROR: Kafka failed to start within 30 seconds.
        echo Last logs from kafka.log:
        type logs\kafka.log | tail -n 20
        goto cleanup
    )
    goto wait_kafka
)
echo Kafka started successfully on port 9092

:create_topics
:: Create topics with retry logic
echo Creating Kafka topics...
C:\kafka_2.12-3.9.0\bin\windows\kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic file-upload-topic || (
    echo Failed to create topics. Retrying in 5 seconds...
    timeout /t 5 >nul
    goto create_topics
)

:cleanup
if %ERRORLEVEL% neq 0 (
    echo Cleaning up Kafka processes...
    taskkill /F /IM java.exe /FI "WINDOWTITLE eq Kafka"
)

echo.
echo Kafka services started successfully!
echo.
echo Available topics:
C:\kafka_2.12-3.9.0\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092

echo.
echo If Kafka failed to start, check the logs in the logs directory.
echo Zookeeper log: logs\zookeeper.log
echo Kafka log: logs\kafka.log

pause