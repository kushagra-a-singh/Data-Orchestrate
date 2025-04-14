@echo off
setlocal enabledelayedexpansion

REM Create logs directory if it doesn't exist
mkdir "logs" 2>nul

REM Set log file
set "LOGFILE=logs\management_%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log"
set "LOGFILE=!LOGFILE: =0!"

REM Start with menu
goto displayMenu

REM Function to log messages
:log
echo %date% %time% - %~1 >> "%LOGFILE%"
echo %~1
goto :eof

REM Function to check service status
:checkServiceStatus
set "port=%~1"
set "service=%~2"
netstat -ano | findstr ":%port%" >nul
if %ERRORLEVEL% equ 0 (
    call :log "%service% is running on port %port%"
    set "status=Running"
    echo %service%: Running
    goto :eof
) else (
    call :log "%service% is not running"
    set "status=Not Running"
    echo %service%: Not Running
    goto :eof
)

REM Function to display menu
:displayMenu
cls
call :log "Data Orchestration Service Manager"
echo.
echo 1. Start All Services
echo 2. Stop All Services
echo 3. Restart All Services
echo 4. Check Service Status
echo 5. View Logs
echo 6. Exit
echo.
set /p choice="Enter your choice (1-6): "

if "%choice%"=="1" goto startAll
if "%choice%"=="2" goto stopAll
if "%choice%"=="3" goto restartAll
if "%choice%"=="4" goto checkAllStatus
if "%choice%"=="5" goto viewLogs
if "%choice%"=="6" goto exitMenu
goto displayMenu

:startAll
call :log "Starting all services..."
start-kafka.bat
timeout /t 5 /nobreak >nul
start-services.bat
echo All services started.
echo Press any key to return to menu...
pause >nul
goto displayMenu

:stopAll
call :log "Stopping all services..."
stop-services.bat
echo All services stopped.
echo Press any key to return to menu...
pause >nul
goto displayMenu

:restartAll
call :log "Restarting all services..."
stop-services.bat
timeout /t 5 /nobreak >nul
start-kafka.bat
timeout /t 5 /nobreak >nul
start-services.bat
echo All services restarted.
echo Press any key to return to menu...
pause >nul
goto displayMenu

:checkAllStatus
cls
call :log "Checking service status..."
echo.
echo Service Status:
echo -----------------
call :checkServiceStatus 2181 "Zookeeper"
call :checkServiceStatus 9092 "Kafka"
call :checkServiceStatus 8085 "Storage Service"
call :checkServiceStatus 8081 "File Upload Service"
call :checkServiceStatus 8082 "Processing Service"
call :checkServiceStatus 8084 "Notification Service"
call :checkServiceStatus 8083 "Orchestrator Service"
echo.
echo Press any key to return to menu...
pause >nul
goto displayMenu

:viewLogs
cls
echo Available Log Files:
echo -----------------
dir /b logs\*.log
echo.
set /p logfile="Enter log file name to view (or press Enter to return): "
if "%logfile%"=="" goto displayMenu
if exist "logs\%logfile%" (
    type "logs\%logfile%"
    echo.
    echo Press any key to return to menu...
    pause >nul
) else (
    echo Log file not found!
    timeout /t 2 /nobreak >nul
)
goto displayMenu

:exitMenu
call :log "Exiting service manager..."
exit /b 0 