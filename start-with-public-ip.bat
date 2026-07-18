@echo off
REM Salvation Army WhatsApp Data Collection - Public IP Access Startup
REM ====================================================================

echo.
echo ============================================================
echo  Starting with Public IP Access
echo ============================================================
echo.

REM Navigate to project root
cd /d "%~dp0"

REM Check prerequisites
echo [1/4] Checking prerequisites...
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

REM Start database
echo [2/4] Starting PostgreSQL database...
docker-compose up -d
if errorlevel 1 (
    echo [ERROR] Failed to start database!
    pause
    exit /b 1
)
echo [OK] Database container started
echo [INFO] Waiting for PostgreSQL to be ready...
timeout /t 10 /nobreak >nul

echo [INFO] Ensuring database exists...
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
    if exist "env.example" (
        copy env.example .env
        echo [OK] .env file created from template
    ) else (
        echo [WARNING] env.example not found. Creating basic .env...
        echo. > .env
    )
    echo.
)

REM Get local IP address
echo [3/4] Detecting network configuration...
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4 Address"') do (
    set LOCAL_IP=%%a
    goto :found_ip
)
:found_ip
set LOCAL_IP=%LOCAL_IP:~1%
set LOCAL_IP=%LOCAL_IP: =%

if "%LOCAL_IP%"=="" (
    echo [WARNING] Could not detect local IP address
    set LOCAL_IP=localhost
)

echo.
echo ============================================================
echo  Network Information
echo ============================================================
echo  Local IP: %LOCAL_IP%
echo  Backend:  http://%LOCAL_IP%:8081
echo  Frontend: http://%LOCAL_IP%:5173
echo.
echo  For public access, use one of these options:
echo  1. ngrok: ngrok http 8081 (for backend)
echo  2. ngrok: ngrok http 5173 (for frontend)
echo  3. Configure port forwarding on your router
echo ============================================================
echo.

REM Start backend in new window
echo [4/4] Starting backend...
start "Salvation Army Backend (Public IP)" cmd /k "cd /d "%~dp0" && mvn spring-boot:run"
echo [OK] Backend starting in new window
echo [INFO] Backend will be ready in ~30 seconds
echo.

REM Wait a bit for backend to start
echo [INFO] Waiting for backend to initialize...
timeout /t 5 /nobreak >nul

REM Start frontend in new window
echo [INFO] Starting frontend...
cd frontend

REM Check if frontend .env exists
if not exist ".env" (
    echo [INFO] Creating frontend .env file...
    echo VITE_API_URL=http://%LOCAL_IP%:8081 > .env
    echo [OK] Frontend .env created
) else (
    echo [INFO] Frontend .env already exists
)

REM Check if node_modules exists
if not exist "node_modules" (
    echo [INFO] Installing frontend dependencies...
    call npm install
)

cd ..
start "Salvation Army Frontend (Public IP)" cmd /k "cd /d "%~dp0\frontend" && npm run dev"
echo [OK] Frontend starting in new window
echo.

echo ============================================================
echo  Services Starting!
echo ============================================================
echo.
echo  Backend:  http://%LOCAL_IP%:8081
echo  Frontend: http://%LOCAL_IP%:5173
echo  Chat:     http://%LOCAL_IP%:5173/chat
echo.
echo  Login: admin / admin123
echo.
echo  To stop services, close the backend and frontend windows
echo  Or run: stop-all.bat
echo.
echo ============================================================
echo.

echo Opening chat page in browser...
timeout /t 15 /nobreak >nul
start http://%LOCAL_IP%:5173/chat

echo.
echo System is running! You can close this window.
pause
