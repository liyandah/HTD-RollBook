@echo off
REM Check status of all services
REM =============================

echo.
echo ============================================================
echo  Service Status Check
echo ============================================================
echo.

REM Check Database
echo [Database]
docker ps | findstr "salvation-army-db" >nul
if errorlevel 1 (
    echo Status: STOPPED
) else (
    echo Status: RUNNING
    docker ps | findstr "salvation-army-db"
)
echo.

REM Check Backend
echo [Backend]
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo Status: STOPPED
) else (
    echo Status: RUNNING on port 8080
    curl -s http://localhost:8080/actuator/health >nul 2>&1
    if errorlevel 1 (
        echo Health: Starting up...
    ) else (
        echo Health: OK
    )
)
echo.

REM Check Frontend
echo [Frontend]
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo Status: STOPPED
) else (
    echo Status: RUNNING on port 5173
)
echo.

REM Check Prerequisites
echo [Prerequisites]
where docker >nul 2>nul
if errorlevel 1 (
    echo Docker: NOT FOUND
) else (
    echo Docker: OK
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven: NOT FOUND
) else (
    echo Maven: OK
)

where node >nul 2>nul
if errorlevel 1 (
    echo Node.js: NOT FOUND
) else (
    echo Node.js: OK
)

where npm >nul 2>nul
if errorlevel 1 (
    echo npm: NOT FOUND
) else (
    echo npm: OK
)
echo.

echo ============================================================
echo  Access Points
echo ============================================================
echo.
echo  Frontend:  http://localhost:5173
echo  Backend:   http://localhost:8080
echo  API Docs:  http://localhost:8080/swagger-ui.html
echo.
echo ============================================================
echo.

pause






