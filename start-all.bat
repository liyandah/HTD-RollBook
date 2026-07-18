@echo off
REM Salvation Army WhatsApp Data Collection - Full System Startup
REM ===============================================================

echo.
echo ============================================================
echo  Salvation Army WhatsApp Data Collection System
echo  Starting All Services
echo ============================================================
echo.

REM Check prerequisites
echo [1/5] Checking prerequisites...
where docker >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker not found! Please install Docker Desktop.
    pause
    exit /b 1
)

REM Check if Docker is running
docker ps >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Desktop is not running!
    echo.
    echo Please start Docker Desktop:
    echo   1. Open Docker Desktop from Start Menu
    echo   2. Wait for the whale icon to appear in system tray
    echo   3. Run this script again
    echo.
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

REM Start database
echo [2/5] Starting PostgreSQL database...
docker-compose up -d
if errorlevel 1 (
    echo [ERROR] Failed to start database!
    pause
    exit /b 1
)
echo [OK] Database container started
echo [INFO] Waiting for PostgreSQL to be ready...
timeout /t 10 /nobreak >nul

echo [3/5] Creating database if needed...
docker exec salvation-army-db psql -U postgres -c "CREATE DATABASE salvation_army_db;" 2>nul
if errorlevel 1 (
    echo [INFO] Database already exists
) else (
    echo [OK] Database created
)
echo.

REM Check .env file
if not exist ".env" (
    echo [WARNING] .env file not found! Creating from template...
    copy env.example .env
    echo.
    echo [IMPORTANT] Please edit .env with your WhatsApp credentials:
    echo   - META_VERIFY_TOKEN
    echo   - META_ACCESS_TOKEN
    echo   - META_PHONE_NUMBER_ID
    echo.
    pause
)

REM Start backend in new window
echo [4/5] Starting backend...
start "Salvation Army Backend" cmd /k "cd /d "%~dp0" && start-backend.bat"
echo [OK] Backend starting in new window
echo [INFO] Backend will be ready in ~30 seconds at http://localhost:8599
echo.

REM Wait a bit for backend to start
echo [INFO] Waiting for backend to initialize...
timeout /t 5 /nobreak >nul

REM Start frontend in new window
echo [5/5] Starting frontend...
start "Salvation Army Frontend" cmd /k "cd /d "%~dp0" && start-frontend.bat"
echo [OK] Frontend starting in new window
echo [INFO] Frontend will be ready at http://localhost:5173
echo.

echo ============================================================
echo  All Services Started!
echo ============================================================
echo.
echo  Backend:   http://localhost:8599
echo  Frontend:  http://localhost:5173
echo  API Docs:  http://localhost:8599/swagger-ui.html
echo.
echo  Login: admin / admin123
echo.
echo  To stop all services, run: stop-all.bat
echo  Or close the backend and frontend windows
echo.
echo ============================================================
echo.

echo Opening dashboard in browser...
timeout /t 15 /nobreak >nul
start http://localhost:5173

echo.
echo System is running! You can close this window.
pause

