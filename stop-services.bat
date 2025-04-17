@echo off
setlocal enabledelayedexpansion

:menu
cls
echo ===================================================
echo        Data Orchestration Service Manager
echo ===================================================
echo.
echo Select a service to stop:
echo.
echo  1. File Upload Service (Port 8081)
echo  2. Storage Service (Port 8082)
echo  3. Processing Service (Port 8083)
echo  4. Orchestrator Service (Port 8084)
echo  5. GUI Service (Port 8085)
echo  6. Notification Service (Port 8086)
echo  7. Kafka (Port 9092)
echo  8. Zookeeper (Port 2181)
echo.
echo  9. Stop ALL Services (Current Implementation)
echo  0. Exit
echo.
echo ===================================================
set /p choice=Enter your choice (0-9): 

if "%choice%"=="1" goto stop_file_upload
if "%choice%"=="2" goto stop_storage
if "%choice%"=="3" goto stop_processing
if "%choice%"=="4" goto stop_orchestrator
if "%choice%"=="5" goto stop_gui
if "%choice%"=="6" goto stop_notification
if "%choice%"=="7" goto stop_kafka
if "%choice%"=="8" goto stop_zookeeper
if "%choice%"=="9" goto stop_all
if "%choice%"=="0" goto exit
echo Invalid choice. Please try again.
timeout /t 2 >nul
goto menu

:stop_file_upload
echo Stopping File Upload Service (Port 8081)...
call :stop_service_by_port 8081
goto check_status

:stop_storage
echo Stopping Storage Service (Port 8082)...
call :stop_service_by_port 8082
goto check_status

:stop_processing
echo Stopping Processing Service (Port 8083)...
call :stop_service_by_port 8083
goto check_status

:stop_orchestrator
echo Stopping Orchestrator Service (Port 8084)...
call :stop_service_by_port 8084
goto check_status

:stop_gui
echo Stopping GUI Service (Port 8085)...
call :stop_service_by_port 8085
goto check_status

:stop_notification
echo Stopping Notification Service (Port 8086)...
call :stop_service_by_port 8086
goto check_status

:stop_kafka
echo Stopping Kafka (Port 9092)...
call :stop_service_by_port 9092
goto check_status

:stop_zookeeper
echo Stopping Zookeeper (Port 2181)...
call :stop_service_by_port 2181
goto check_status

:stop_all
echo Stopping ALL Data Orchestration Services...

REM Kill processes by Java application name for microservices
echo Stopping Java microservices...
taskkill /F /FI "IMAGENAME eq java.exe" /T

REM Kill processes by port
echo Stopping any remaining services by port...

REM Check and kill processes by port
set PORTS=8081 8082 8083 8084 8085 8086 9092 2181
for %%p in (%PORTS%) do (
    echo Checking port %%p...
    netstat -ano | findstr "LISTENING" | findstr ":%%p " > nul
    if not errorlevel 1 (
        for /f "tokens=5" %%a in ('netstat -ano ^| findstr "LISTENING" ^| findstr ":%%p "') do (
            echo   Killing process on port %%p (PID: %%a)
            taskkill /F /PID %%a /T
        )
    )
    if errorlevel 1 (
        echo   No process found on port %%p
    )
)

REM Give processes a moment to fully terminate
timeout /t 3 /nobreak > nul
goto check_status

:stop_service_by_port
echo Checking port %1...
netstat -ano | findstr "LISTENING" | findstr ":%1 " > nul
if not errorlevel 1 (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr "LISTENING" ^| findstr ":%1 "') do (
        echo   Killing process on port %1 (PID: %%a)
        taskkill /F /PID %%a /T
    )
) else (
    echo   No process found on port %1
)
timeout /t 2 /nobreak > nul
exit /b

:check_status
echo.
echo Checking service status...
echo.
echo Service Status:
echo -----------------
for %%p in (8081 8082 8083 8084 8085 8086 9092 2181) do (
    netstat -ano | findstr "LISTENING" | findstr ":%%p " > nul
    if not errorlevel 1 (
        echo Port %%p: Still Running
    )
    if errorlevel 1 (
        echo Port %%p: Stopped
    )
)

echo.
echo Operation completed.
echo.
echo Press any key to return to menu...
pause > nul
goto menu

:exit
echo Exiting service manager...
timeout /t 1 /nobreak > nul
exit