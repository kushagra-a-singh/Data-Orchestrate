@echo off
echo Starting Kafka services...

:: Clean up any existing Java processes
echo Cleaning up existing Java processes...
taskkill /F /IM java.exe

:: Start Zookeeper
echo Starting Zookeeper...
start "Zookeeper" cmd /c "C:\kafka_2.12-3.9.0\bin\windows\zookeeper-server-start.bat C:\kafka_2.12-3.9.0\config\zookeeper.properties"

:: Wait for Zookeeper to start
echo Waiting for Zookeeper to start...
timeout /t 10 /nobreak >nul

:: Start Kafka
echo Starting Kafka...
start "Kafka" cmd /c "C:\kafka_2.12-3.9.0\bin\windows\kafka-server-start.bat C:\kafka_2.12-3.9.0\config\server.properties"

:: Wait for Kafka to start
echo Waiting for Kafka to start...
timeout /t 10 /nobreak >nul

:: Create topics
echo Creating topics...
C:\kafka_2.12-3.9.0\bin\windows\kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic raw-data
C:\kafka_2.12-3.9.0\bin\windows\kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic processed-data

echo Kafka setup complete! 
