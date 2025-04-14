@echo off
setlocal enabledelayedexpansion

REM Create monitoring directory if it doesn't exist
mkdir "monitoring" 2>nul
mkdir "monitoring\backups" 2>nul
mkdir "monitoring\reports" 2>nul

REM Set log file
set "LOGFILE=monitoring\monitor_%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log"
set "LOGFILE=!LOGFILE: =0!"

REM Start with displaying the menu
goto displayMenu

REM Function to log messages
:log
echo %date% %time% - %~1 >> "%LOGFILE%"
echo %~1
goto :eof

REM Function to backup configuration
:backupConfig
set "source=%~1"
set "dest=monitoring\backups\%~nx1_%date:~-4,4%%date:~-7,2%%date:~-10,2%.bak"
copy "%source%" "%dest%" >nul
if %ERRORLEVEL% equ 0 (
    call :log "Backed up %source% to %dest%"
) else (
    call :log "Failed to backup %source%"
)
goto :eof

REM Function to generate performance report
:generatePerformanceReport
set "report=monitoring\reports\performance_%date:~-4,4%%date:~-7,2%%date:~-10,2%.html"
echo ^<!DOCTYPE html^> > "%report%"
echo ^<html^>^<head^>^<title^>Service Performance Report^</title^> >> "%report%"
echo ^<style^> >> "%report%"
echo body { font-family: Arial, sans-serif; margin: 20px; } >> "%report%"
echo .service { border: 1px solid #ccc; padding: 10px; margin: 10px 0; } >> "%report%"
echo .healthy { color: green; } >> "%report%"
echo .warning { color: orange; } >> "%report%"
echo .error { color: red; } >> "%report%"
echo ^</style^>^</head^>^<body^> >> "%report%"
echo ^<h1^>Service Performance Report - %date% %time%^</h1^> >> "%report%"

REM Check each service
for %%s in (
    "Storage Service:8085"
    "File Upload Service:8081"
    "Processing Service:8082"
    "Notification Service:8084"
    "Orchestrator Service:8083"
) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%s") do (
        set "service=%%a"
        set "port=%%b"
        
        echo ^<div class="service"^> >> "%report%"
        echo ^<h2^>%%a^</h2^> >> "%report%"
        
        REM Check if service is running
        netstat -ano | findstr ":%%b" >nul
        if !ERRORLEVEL! equ 0 (
            echo ^<p class="healthy"^>Status: Running^</p^> >> "%report%"
            
            REM Get memory usage
            for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%%b"') do (
                for /f "tokens=5" %%m in ('wmic process where ProcessId^=%%p get WorkingSetSize ^| findstr /r "[0-9]"') do (
                    set /a "memory=%%m/1024/1024"
                    echo ^<p^>Memory Usage: !memory! MB^</p^> >> "%report%"
                )
            )
            
            REM Check response time
            curl -s -w "%%{time_total}\n" -o nul http://localhost:%%b/actuator/health 2>nul >> "%report%"
        ) else (
            echo ^<p class="error"^>Status: Not Running^</p^> >> "%report%"
        )
        
        echo ^</div^> >> "%report%"
    )
)

REM Check Kafka topics
echo ^<div class="service"^> >> "%report%"
echo ^<h2^>Kafka Topics^</h2^> >> "%report%"
C:\kafka\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 2>nul >> "%report%"
echo ^</div^> >> "%report%"

echo ^</body^>^</html^> >> "%report%"
call :log "Generated performance report: %report%"
goto :eof

REM Function to display menu
:displayMenu
cls
call :log "Service Monitoring Dashboard"
echo.
echo 1. Generate Performance Report
echo 2. Backup Configurations
echo 3. View Service Metrics
echo 4. Check Service Health
echo 5. View Kafka Statistics
echo 6. Exit
echo.
set /p choice="Enter your choice (1-6): "

if "%choice%"=="1" goto generateReportMenu
if "%choice%"=="2" goto backupConfigMenu
if "%choice%"=="3" goto viewMetrics
if "%choice%"=="4" goto checkHealth
if "%choice%"=="5" goto kafkaStats
if "%choice%"=="6" goto exitMenu
goto displayMenu

:generateReportMenu
call :generatePerformanceReport
echo Report generated: monitoring\reports\performance_%date:~-4,4%%date:~-7,2%%date:~-10,2%.html
pause
goto displayMenu

:backupConfigMenu
call :log "Backing up configurations..."
call :backupConfig ".env"
call :backupConfig "kafka\kafka-config.yml"
echo Configurations backed up to monitoring\backups\
pause
goto displayMenu

:viewMetrics
cls
echo Service Metrics:
echo -----------------
for %%s in (
    "Storage Service:8085"
    "File Upload Service:8081"
    "Processing Service:8082"
    "Notification Service:8084"
    "Orchestrator Service:8083"
) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%s") do (
        echo.
        echo %%a:
        netstat -ano | findstr ":%%b" >nul
        if !ERRORLEVEL! equ 0 (
            for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%%b"') do (
                for /f "tokens=5" %%m in ('wmic process where ProcessId^=%%p get WorkingSetSize ^| findstr /r "[0-9]"') do (
                    set /a "memory=%%m/1024/1024"
                    echo Memory Usage: !memory! MB
                )
            )
            curl -s -w "Response Time: %%{time_total}s\n" -o nul http://localhost:%%b/actuator/health 2>nul
        ) else (
            echo Service not running
        )
    )
)
pause
goto displayMenu

:checkHealth
cls
echo Service Health Status:
echo -----------------
for %%s in (
    "Storage Service:8085"
    "File Upload Service:8081"
    "Processing Service:8082"
    "Notification Service:8084"
    "Orchestrator Service:8083"
) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%s") do (
        echo.
        echo %%a:
        curl -s http://localhost:%%b/actuator/health >nul 2>nul
        if !ERRORLEVEL! equ 0 (
            echo Status: Healthy
        ) else (
            echo Status: Unhealthy
        )
    )
)
pause
goto displayMenu

:kafkaStats
cls
echo Kafka Statistics:
echo -----------------
echo.
echo Topics:
C:\kafka\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
echo.
echo Topic Details:
for %%t in (file-upload file-processing file-storage notifications) do (
    echo.
    echo Topic: %%t
    C:\kafka\bin\windows\kafka-topics.bat --describe --topic %%t --bootstrap-server localhost:9092
)
pause
goto displayMenu

:exitMenu
call :log "Exiting monitoring dashboard..."
exit /b 0 