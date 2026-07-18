@echo off
REM Create the PostgreSQL database
REM ==================================

echo.
echo ========================================
echo  Database Setup
echo ========================================
echo.

REM Check if Docker is running
echo [1/3] Checking Docker...
docker ps >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running!
    echo.
    echo Please start Docker Desktop:
    echo   1. Open Docker Desktop application
    echo   2. Wait for it to start (whale icon in system tray)
    echo   3. Run this script again
    echo.
    pause
    exit /b 1
)
echo [OK] Docker is running

echo.
echo [2/3] Starting PostgreSQL container...
docker-compose up -d
if errorlevel 1 (
    echo [ERROR] Failed to start PostgreSQL!
    pause
    exit /b 1
)
echo [OK] PostgreSQL container started

echo [INFO] Waiting for PostgreSQL to be ready...
timeout /t 5 /nobreak >nul

echo.
echo [3/3] Creating database...

REM Try to create database
docker exec salvation-army-db psql -U postgres -c "SELECT 1 FROM pg_database WHERE datname='salvation_army_db'" | findstr "1" >nul
if errorlevel 1 (
    echo Database doesn't exist. Creating...
    docker exec salvation-army-db psql -U postgres -c "CREATE DATABASE salvation_army_db;"
    if errorlevel 0 (
        echo [OK] Database 'salvation_army_db' created successfully!
    ) else (
        echo [ERROR] Failed to create database
        pause
        exit /b 1
    )
) else (
    echo [OK] Database 'salvation_army_db' already exists
)

echo.
echo ========================================
echo  Database Setup Complete!
echo ========================================
echo.
echo Database: salvation_army_db
echo Host: localhost
echo Port: 5432
echo Username: postgres
echo Password: (set in .env / POSTGRES_PASSWORD)
echo.
echo You can now start the backend with:
echo   start-backend.bat
echo.

pause






