@echo off
REM Salvation Army Dialogflow Chat - Startup Script
REM ===================================================
REM This script starts the backend and frontend for the Dialogflow chat integration
REM It checks for required Dialogflow environment variables

echo.
echo ============================================================
echo  Salvation Army Dialogflow Chat System
echo  Starting Backend and Frontend
echo ============================================================
echo.

REM Check prerequisites
echo [1/6] Checking prerequisites...
where docker >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker not found! Please install Docker Desktop.
    pause
    exit /b 1
)

docker ps >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Desktop is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven not found! Please install Maven.
    pause
    exit /b 1
)

where node >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Node.js not found! Please install Node.js.
    pause
    exit /b 1
)

echo [OK] All prerequisites met
echo.

REM Check Dialogflow configuration
echo [2/6] Checking Dialogflow configuration...

REM Check GOOGLE_APPLICATION_CREDENTIALS
if "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (
    echo [WARNING] GOOGLE_APPLICATION_CREDENTIALS not set!
    echo.
    echo To set it, run:
    echo   set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\service-account.json
    echo.
    echo Or set it permanently in System Environment Variables.
    echo.
    set /p GOOGLE_APPLICATION_CREDENTIALS="Enter path to service account JSON file (or press Enter to skip): "
    if not "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (
        if not exist "%GOOGLE_APPLICATION_CREDENTIALS%" (
            echo [ERROR] File not found: %GOOGLE_APPLICATION_CREDENTIALS%
            pause
            exit /b 1
        )
        echo [OK] Using credentials: %GOOGLE_APPLICATION_CREDENTIALS%
    ) else (
        echo [WARNING] Continuing without Dialogflow credentials. Chat will not work until configured.
    )
) else (
    if not exist "%GOOGLE_APPLICATION_CREDENTIALS%" (
        echo [ERROR] GOOGLE_APPLICATION_CREDENTIALS points to non-existent file!
        echo Path: %GOOGLE_APPLICATION_CREDENTIALS%
        pause
        exit /b 1
    )
    echo [OK] Using credentials: %GOOGLE_APPLICATION_CREDENTIALS%
)

REM Check DIALOGFLOW_PROJECT_ID
if "%DIALOGFLOW_PROJECT_ID%"=="" (
    echo [WARNING] DIALOGFLOW_PROJECT_ID not set!
    echo.
    echo You can set it in application.properties or as environment variable:
    echo   set DIALOGFLOW_PROJECT_ID=your-project-id
    echo.
    set /p DIALOGFLOW_PROJECT_ID="Enter Dialogflow Project ID (or press Enter to skip): "
    if not "%DIALOGFLOW_PROJECT_ID%"=="" (
        echo [OK] Using project ID: %DIALOGFLOW_PROJECT_ID%
    ) else (
        echo [WARNING] Continuing without Dialogflow project ID. Check application.properties.
    )
) else (
    echo [OK] Using project ID: %DIALOGFLOW_PROJECT_ID%
)

echo.

REM Start database
echo [3/6] Starting PostgreSQL database...
docker-compose up -d
if errorlevel 1 (
    echo [ERROR] Failed to start database!
    pause
    exit /b 1
)
echo [OK] Database container started
echo [INFO] Waiting for PostgreSQL to be ready...
timeout /t 10 /nobreak >nul

echo [4/6] Ensuring database exists...
docker exec salvation-army-db psql -U postgres -c "CREATE DATABASE salvation_army_db;" 2>nul
if errorlevel 1 (
    echo [INFO] Database already exists
) else (
    echo [OK] Database created
)
echo.

REM Check application.properties for Dialogflow config
echo [5/6] Checking application.properties...
findstr /C:"dialogflow.project-id" "src\main\resources\application.properties" >nul 2>&1
if errorlevel 1 (
    echo [WARNING] dialogflow.project-id not found in application.properties
    echo You may need to add it manually.
) else (
    echo [OK] Dialogflow configuration found in application.properties
)
echo.

REM Start backend in new window
echo [6/6] Starting backend...
echo [INFO] Backend will be available at: http://localhost:8081
echo [INFO] Dialogflow webhook: http://localhost:8081/api/dialogflow/webhook
echo [INFO] Chat API: http://localhost:8081/api/chat/message
echo.

REM Set environment variables for this session
if not "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (
    set "ENV_GOOGLE_APPLICATION_CREDENTIALS=%GOOGLE_APPLICATION_CREDENTIALS%"
)
if not "%DIALOGFLOW_PROJECT_ID%"=="" (
    set "ENV_DIALOGFLOW_PROJECT_ID=%DIALOGFLOW_PROJECT_ID%"
)

start "Salvation Army Backend (Dialogflow)" cmd /k "cd /d "%~dp0" && if not "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (set GOOGLE_APPLICATION_CREDENTIALS=%GOOGLE_APPLICATION_CREDENTIALS%) && if not "%DIALOGFLOW_PROJECT_ID%"=="" (set DIALOGFLOW_PROJECT_ID=%DIALOGFLOW_PROJECT_ID%) && mvn spring-boot:run"
echo [OK] Backend starting in new window
echo [INFO] Backend will be ready in ~30 seconds
echo.

REM Wait a bit for backend to start
echo [INFO] Waiting for backend to initialize...
timeout /t 5 /nobreak >nul

REM Start frontend in new window
echo [INFO] Starting frontend...
start "Salvation Army Frontend (Chat)" cmd /k "cd /d "%~dp0\frontend" && if not exist node_modules (call npm install) && call npm run dev"
echo [OK] Frontend starting in new window
echo [INFO] Frontend will be ready at http://localhost:5173
echo.

echo ============================================================
echo  All Services Started!
echo ============================================================
echo.
echo  Backend:   http://localhost:8081
echo  Frontend:  http://localhost:5173
echo  Chat Page: http://localhost:5173/chat
echo  API Docs:  http://localhost:8081/swagger-ui.html
echo.
echo  Dialogflow Webhook: http://localhost:8081/api/dialogflow/webhook
echo  (Use ngrok for local testing: ngrok http 8081)
echo.
if not "%GOOGLE_APPLICATION_CREDENTIALS%"=="" (
    echo [OK] Dialogflow credentials configured
) else (
    echo [WARNING] Dialogflow credentials not set - chat may not work
)
if not "%DIALOGFLOW_PROJECT_ID%"=="" (
    echo [OK] Dialogflow project ID configured
) else (
    echo [WARNING] Dialogflow project ID not set - check application.properties
)
echo.
echo  To stop all services, close the backend and frontend windows
echo  Or run: stop-all.bat
echo.
echo ============================================================
echo.

echo Opening chat page in browser...
timeout /t 20 /nobreak >nul
start http://localhost:5173/chat

echo.
echo System is running! You can close this window.
echo.
echo IMPORTANT: For local Dialogflow webhook testing:
echo   1. Install ngrok: https://ngrok.com/
echo   2. Run: ngrok http 8081
echo   3. Copy the https URL (e.g., https://xxxx.ngrok-free.app)
echo   4. In Dialogflow Console -^> Fulfillment:
echo      Set webhook URL to: https://xxxx.ngrok-free.app/api/dialogflow/webhook
echo.
pause
