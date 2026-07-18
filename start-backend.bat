@echo off
REM Salvation Army WhatsApp Data Collection - Backend Startup Script
REM ===================================================================

echo.
echo ========================================
echo  Salvation Army WhatsApp Backend
echo ========================================
echo.

REM Check if .env file exists
if not exist ".env" (
    echo [WARNING] .env file not found!
    echo Creating .env from env.example...
    copy env.example .env
    echo.
    echo [IMPORTANT] Please edit .env file with your configuration before continuing.
    echo Press any key when ready...
    pause >nul
)

echo [INFO] Checking if Docker is running...
docker ps >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [INFO] Checking if database is running...
docker ps | findstr "salvation-army-db" >nul
if errorlevel 1 (
    echo [INFO] Database not running. Starting PostgreSQL...
    docker-compose up -d
    echo [INFO] Waiting for database to be ready...
    timeout /t 10 /nobreak >nul
    
    echo [INFO] Creating database if it doesn't exist...
    docker exec salvation-army-db psql -U postgres -c "CREATE DATABASE salvation_army_db;" 2>nul
    if errorlevel 1 (
        echo [INFO] Database already exists or will be created by Flyway
    )
) else (
    echo [OK] Database is already running
    echo [INFO] Ensuring database exists...
    docker exec salvation-army-db psql -U postgres -c "CREATE DATABASE salvation_army_db;" 2>nul
    if errorlevel 1 (
        echo [INFO] Database already exists
    )
)

echo.
echo [INFO] Starting Spring Boot backend...
echo [INFO] Backend will be available at: http://localhost:8080
echo [INFO] API Documentation: http://localhost:8080/swagger-ui.html
echo.
echo Press Ctrl+C to stop the backend
echo.
echo ========================================
echo.

REM Run Maven Spring Boot
mvn spring-boot:run

pause

