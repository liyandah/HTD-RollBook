@echo off
REM Salvation Army WhatsApp Data Collection - Frontend Startup Script
REM ====================================================================

echo.
echo ========================================
echo  Salvation Army WhatsApp Frontend
echo ========================================
echo.

REM Navigate to frontend directory
cd frontend

REM Check if node_modules exists
if not exist "node_modules" (
    echo [INFO] Installing dependencies...
    echo This may take a few minutes...
    call npm install
    echo.
)

REM Check if .env file exists
if not exist ".env" (
    echo [INFO] Creating .env file...
    echo VITE_API_BASE_URL=/api > .env
    echo [OK] Frontend .env created ^(Vite proxies /api to backend on 8599^)
)

echo.
echo [INFO] Starting React development server...
echo [INFO] Frontend will be available at: http://localhost:5173
echo.
echo [INFO] Default login credentials:
echo   Username: admin
echo   Password: admin123
echo.
echo Press Ctrl+C to stop the frontend
echo.
echo ========================================
echo.

REM Start Vite dev server
call npm run dev

pause






