@echo off
setlocal enabledelayedexpansion
set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

echo ====================================================
echo   COPD CDSS - Automated Setup Script (Windows)
echo ====================================================
echo.

REM ─── Check Python ────────────────────────────────────
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed or not in PATH! 
    echo Please install Python 3.10+ from python.org and check "Add to PATH"
    pause
    exit /b 1
)

REM ─── Check Node.js ──────────────────────────────────
node --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not installed or not in PATH!
    echo Please install Node.js 18+ from nodejs.org
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
if not exist "backend" (
    echo [ERROR] 'backend' folder not found in %BASE_DIR%
    pause
    exit /b 1
)
cd /d "%BASE_DIR%\backend" || (echo [ERROR] Could not enter backend folder & pause & exit /b 1)

echo [1/5] Creating Python virtual environment...
python -m venv venv
if errorlevel 1 (
    echo [ERROR] Failed to create virtual environment. 
    echo If you see 'Access Denied', try running this script as Administrator.
    pause
    exit /b 1
)
call venv\Scripts\activate.bat || (echo [ERROR] Failed to activate venv & pause & exit /b 1)

echo [2/5] Installing Python dependencies...
python -m pip install --upgrade pip
pip install -r requirements.txt || (echo [ERROR] Failed to install requirements & pause & exit /b 1)
pip install numpy

echo [3/5] Verifying AI Models...
if not exist "ml_model\trained_model\model.pkl" (
    echo [WARNING] AI Device Model (model.pkl) missing! Heuristic fallback will be used.
)
if not exist "ml_model\trained_model\encoder.pkl" (
    echo [WARNING] AI Encoder (encoder.pkl) missing!
)

echo [4/5] Running database migrations...
python manage.py makemigrations api
python manage.py migrate || (echo [ERROR] Migration failed. Check your MySQL connection and credentials. & pause & exit /b 1)

echo [5/5] Seeding database with default users...
python seed_database.py

echo.
echo ─── Step 2: Setting up Frontend ──────────────────
echo.
if not exist "%BASE_DIR%\frontend" (
    echo [ERROR] 'frontend' folder not found in %BASE_DIR%
    pause
    exit /b 1
)
cd /d "%BASE_DIR%\frontend" || (echo [ERROR] Could not enter frontend folder & pause & exit /b 1)

echo [6/6] Installing Node.js dependencies...
call npm install || (echo [ERROR] Static dependency install failed & pause & exit /b 1)

echo.
echo ====================================================
echo   Setup Complete!
echo ====================================================
echo.
echo   New Features in this version:
echo   ──────────────────────────────────────────
echo   1. AI Therapy: Model-driven device recommendations.
echo   2. SpO2 Alerts: Critical @ 80%, Warning @ 88%.
echo   3. Smart OTP: Required ONLY for Signup and FIRST Login.
echo   ──────────────────────────────────────────
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
echo   Admin:   admin@cdss.com    / admin123  (Direct Login)
echo   Doctor:  doctor@test.com   / doctor123 (OTP on 1st Login)
echo   Staff:   staff@test.com    / staff123  (OTP on 1st Login)
echo   ──────────────────────────────────────────
echo.
cd /d "%BASE_DIR%"
pause

