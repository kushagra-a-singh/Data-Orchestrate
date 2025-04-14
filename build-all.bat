@echo off
echo Starting build process...

REM Create logs directory
mkdir "logs" 2>nul

REM Create required directories
echo Creating required directories...
mkdir "data\uploads" 2>nul
mkdir "data\storage" 2>nul
mkdir "data\processed" 2>nul

REM Check Maven
echo Checking Maven installation...
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Error: Maven is not installed or not in PATH
    pause
    exit /b 1
)

REM Build common-utils
echo Building common-utils...
cd backend\common-utils
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Error building common-utils
    cd ..\..
    pause
    exit /b 1
)
cd ..\..

REM Build backend services
echo Building Storage Service...
cd backend\storage-service
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo Building File Upload Service...
cd backend\file-upload-service
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo Building Processing Service...
cd backend\processing-service
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo Building Notification Service...
cd backend\notification-service
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo Building Orchestrator Service...
cd backend\orchestrator-service
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

REM Build JavaFX frontend
echo Building JavaFX frontend application...
cd frontend\gui-app
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo All services built successfully!
echo.
echo Next steps:
echo 1. Run start-kafka.bat to start Kafka
echo 2. Run start-services.bat to start all services
echo.
pause
exit /b 0

:error
echo Build failed! Check the error messages above.
pause
exit /b 1 