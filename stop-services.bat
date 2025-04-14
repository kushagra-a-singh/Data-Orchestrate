@echo off
echo Stopping Data Orchestration Services...

REM Kill processes by Java application name for microservices
echo Stopping Java microservices...
taskkill /F /FI "IMAGENAME eq java.exe" /T

REM Kill processes by port
echo Stopping any remaining services by port...

REM Check and kill processes by port
set PORTS=8081 8082 8083 8084 8085 9092 2181
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

echo.
echo Checking service status...
echo.
echo Service Status:
echo -----------------
for %%p in (8085 8081 8082 8084 8083 9092 2181) do (
    netstat -ano | findstr "LISTENING" | findstr ":%%p " > nul
    if not errorlevel 1 (
        echo Port %%p: Still Running
    )
    if errorlevel 1 (
        echo Port %%p: Stopped
    )
)

echo.
echo All services have been successfully stopped.
echo Press any key to exit...
pause > nul