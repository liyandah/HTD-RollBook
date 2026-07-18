@echo off
REM Salvation Army WhatsApp Data Collection - Shutdown Script
REM ===========================================================

echo.
echo ========================================
echo  Stopping All Services
echo ========================================
echo.

REM Stop Java processes (Maven/Spring Boot)
echo [1/3] Stopping backend...
taskkill /F /FI "WINDOWTITLE eq Salvation Army Backend*" >nul 2>&1
taskkill /F /IM java.exe /FI "MEMUSAGE gt 100000" >nul 2>&1
echo [OK] Backend stopped

REM Stop Node processes (Vite/React)
echo [2/3] Stopping frontend...
taskkill /F /FI "WINDOWTITLE eq Salvation Army Frontend*" >nul 2>&1
taskkill /F /IM node.exe >nul 2>&1
echo [OK] Frontend stopped

REM Stop Docker containers
echo [3/3] Stopping database...
docker-compose down
echo [OK] Database stopped

echo.
echo ========================================
echo  All Services Stopped
echo ========================================
echo.
echo To start again, run: start-all.bat
echo.

pause






