@echo off
echo ========================================
echo Starting Chat with Public Access (ngrok)
echo ========================================
echo.

echo Step 1: Starting ngrok tunnel for backend...
echo Please run this in a NEW terminal window:
echo   ngrok http 8081
echo.
echo After ngrok starts, copy the HTTPS URL (e.g., https://abc123.ngrok-free.app)
echo.
pause

echo.
echo Step 2: Update frontend/.env with the ngrok URL
echo Create or edit frontend/.env and add:
echo   VITE_API_URL=https://YOUR_NGROK_URL_HERE
echo.
pause

echo.
echo Step 3: Starting backend...
cd /d "%~dp0"
start "Backend" cmd /k "mvn spring-boot:run"

timeout /t 10 /nobreak >nul

echo.
echo Step 4: Starting frontend...
cd frontend
start "Frontend" cmd /k "npm run dev"

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Your chat will be accessible at:
echo   Local: http://localhost:5173/chat
echo   Public: Use ngrok URL for backend API
echo.
echo To make frontend also public, run ngrok for port 5173:
echo   ngrok http 5173
echo.
pause
