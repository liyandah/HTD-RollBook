@echo off
REM Salvation Army Backend with Dialogflow Support
REM ==============================================

echo.
echo ========================================
echo  Salvation Army Backend (Dialogflow)
echo ========================================
echo.

REM Check if .env file exists
if not exist ".env" (
    echo [WARNING] .env file not found!
    echo Creating .env from env.example...
    if exist "env.example" (
        copy env.example .env
        echo.
        echo [IMPORTANT] Please edit .env file with your configuration before continuing.
    )
)

REM Check Dialogflow credentials
if "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (
    echo [WARNING] GOOGLE_APPLICATION_CREDENTIALS not set!
    echo.
    echo To set it, run:
    echo   set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\service-account.json
    echo.
    echo Or set it in System Environment Variables.
    echo.
    set /p GOOGLE_APPLICATION_CREDENTIALS="Enter path to service account JSON (or press Enter to continue): "
    if not "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (
        if not exist "%GOOGLE_APPLICATION_CREDENTIALS%" (
            echo [ERROR] File not found: %GOOGLE_APPLICATION_CREDENTIALS%
            pause
            exit /b 1
        )
    )
)

REM Check Dialogflow project ID
if "%DIALOGFLOW_PROJECT_ID%"=="" (
    echo [INFO] DIALOGFLOW_PROJECT_ID not set in environment.
    echo [INFO] Will use value from application.properties if configured.
    echo.
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
)

echo.
echo [INFO] Starting Spring Boot backend...
echo [INFO] Backend will be available at: http://localhost:8081
echo [INFO] API Documentation: http://localhost:8081/swagger-ui.html
echo [INFO] Dialogflow Webhook: http://localhost:8081/api/dialogflow/webhook
echo [INFO] Chat API: http://localhost:8081/api/chat/message
echo.
if not "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (
    echo [OK] Using Dialogflow credentials: %GOOGLE_APPLICATION_CREDENTIALS%
) else (
    echo [WARNING] Dialogflow credentials not set - chat may not work
)
echo.
echo Press Ctrl+C to stop the backend
echo.
echo ========================================
echo.

REM Run Maven Spring Boot with environment variables
if not "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (
    set GOOGLE_APPLICATION_CREDENTIALS=%GOOGLE_APPLICATION_CREDENTIALS%
)
if not "%DIALOGFLOW_PROJECT_ID%"=="" (
    set DIALOGFLOW_PROJECT_ID=%DIALOGFLOW_PROJECT_ID%
)

mvn spring-boot:run

pause
