@echo off
echo ====================================================
echo   COPD CDSS - Automated Setup Script (Windows)
echo ====================================================
echo.

REM ─── Check Python ────────────────────────────────────
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed! Please install Python 3.10+ from python.org
    pause
    exit /b 1
)

REM ─── Check Node.js ──────────────────────────────────
node --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not installed! Please install Node.js 18+ from nodejs.org
    pause
    exit /b 1
)

REM ─── Check MySQL ────────────────────────────────────
echo.
echo [IMPORTANT] Make sure MySQL is running and you have created the database.
echo.
echo   Before running this script, open MySQL and run:
echo     CREATE DATABASE cdss_copd;
echo.
echo   Then update backend/backend/settings.py with YOUR credentials:
echo     - MySQL NAME, USER, PASSWORD
echo     - EMAIL_HOST_USER: Your Gmail
echo     - EMAIL_HOST_PASSWORD: Your Gmail App Password (REQUIRED for OTP)
echo.
set /p CONTINUE="Have you done the above? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Please complete the MySQL setup first, then re-run this script.
    pause
    exit /b 0
)

echo.
echo ─── Step 1: Setting up Backend ────────────────────
echo.
cd backend

echo [1/5] Creating Python virtual environment...
python -m venv venv
call venv\Scripts\activate.bat

echo [2/5] Installing Python dependencies...
pip install -r requirements.txt
pip install numpy

echo [3/5] Running database migrations...
python manage.py makemigrations api
python manage.py migrate

echo [4/5] Seeding database with default users...
python seed_database.py

echo.
echo ─── Step 2: Setting up Frontend ──────────────────
echo.
cd ..\frontend

echo [5/5] Installing Node.js dependencies...
call npm install

echo.
echo ====================================================
echo   Setup Complete!
echo ====================================================
echo.
echo   To run the application:
echo.
echo   Terminal 1 (Backend):
echo     cd backend
echo     venv\Scripts\activate
echo     python manage.py runserver
echo.
echo   Terminal 2 (Frontend):
echo     cd frontend
echo     npm run dev
echo.
echo   Then open http://localhost:5173 in your browser
echo.
echo   Login Credentials:
echo   ──────────────────────────────────────────
echo   Admin:   admin@cdss.com    / admin123
echo   Doctor:  doctor@test.com   / doctor123
echo   Staff:   staff@test.com    / staff123
echo   ──────────────────────────────────────────
echo.
cd ..
pause
