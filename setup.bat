@echo off
:: use setlocal to keep environment clean, but be careful with DELAYEDEXPANSION
setlocal enabledelayedexpansion

echo ====================================================
echo   COPD CDSS - Automated Setup Script (Windows)
echo ====================================================
echo.

:: Get current directory with absolute path
set "BASE_DIR=%~dp0"
if "!BASE_DIR:~-1!"=="\" set "BASE_DIR=!BASE_DIR:~0,-1!"

:: ─── Check Python ────────────────────────────────────
python --version >nul 2>&1
if errorlevel 1 goto NO_PYTHON

:: ─── Check Node.js ──────────────────────────────────
node --version >nul 2>&1
if errorlevel 1 goto NO_NODE

:: ─── Check MySQL Warning ────────────────────────────
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

set "CONTINUE_CHOICE=n"
set /p CONTINUE_CHOICE="Have you done the above? (y/n): "
if /i "!CONTINUE_CHOICE!"=="y" goto START_BACKEND
echo Please complete the MySQL setup first, then re-run this script.
pause
exit /b 0

:START_BACKEND
echo.
echo ─── Step 1: Setting up Backend ────────────────────
echo.

if not exist "!BASE_DIR!\backend" goto BACKEND_MISSING
cd /d "!BASE_DIR!\backend"

echo [1/5] Creating Python virtual environment...
if exist venv (
    echo   (Virtual environment already exists, skipping creation)
) else (
    python -m venv venv
    if errorlevel 1 goto VENV_FAIL
)

echo [2/5] Activating virtual environment...
if not exist "venv\Scripts\activate.bat" goto VENV_MISSING
call venv\Scripts\activate.bat
if errorlevel 1 goto VENV_FAIL

echo [3/5] Installing Python dependencies...
python -m pip install --upgrade pip
pip install -r requirements.txt
if errorlevel 1 goto PIP_FAIL
pip install numpy

echo [4/5] Verifying AI Models...
if not exist "ml_model\trained_model\model.pkl" (
    echo [WARNING] AI Device Model (model.pkl) missing! Heuristic fallback will be used.
)
if not exist "ml_model\trained_model\encoder.pkl" (
    echo [WARNING] AI Encoder (encoder.pkl) missing!
)

echo [5/5] Running migrations and seeding...
python manage.py makemigrations api
python manage.py migrate
if errorlevel 1 goto MIGRATE_FAIL
python seed_database.py

echo.
echo ─── Step 2: Setting up Frontend ──────────────────
echo.

if not exist "!BASE_DIR!\frontend" goto FRONTEND_MISSING
cd /d "!BASE_DIR!\frontend"

echo [6/6] Installing Node.js dependencies...
call npm install
if errorlevel 1 goto NPM_FAIL

goto SETUP_COMPLETE

:: --- ERROR LABELS ---

:NO_PYTHON
echo [ERROR] Python is not installed or not in PATH!
echo Please install Python 3.10+ and check "Add to PATH" during installation.
pause
exit /b 1

:NO_NODE
echo [ERROR] Node.js is not installed or not in PATH!
pause
exit /b 1

:BACKEND_MISSING
echo [ERROR] 'backend' folder not found in "!BASE_DIR!"
pause
exit /b 1

:VENV_FAIL
echo [ERROR] Failed to handle virtual environment. 
echo If you see 'Access Denied', try running this script as Administrator.
pause
exit /b 1

:VENV_MISSING
echo [ERROR] venv\Scripts\activate.bat not found. Virtual env creation might have failed.
pause
exit /b 1

:PIP_FAIL
echo [ERROR] Failed to install Python requirements.
pause
exit /b 1

:MIGRATE_FAIL
echo [ERROR] Database migration failed. Check your MySQL connection and credentials.
pause
exit /b 1

:FRONTEND_MISSING
echo [ERROR] 'frontend' folder not found in "!BASE_DIR!"
pause
exit /b 1

:NPM_FAIL
echo [ERROR] npm install failed.
pause
exit /b 1

:SETUP_COMPLETE
echo.
echo ====================================================
echo   Setup Complete!
echo ====================================================
echo.
echo   New Features:
echo   1. AI Therapy: Model-driven device recommendations.
echo   2. SpO2 Alerts: Critical @ 80%, Warning @ 88%.
echo   3. Smart OTP: Required ONLY for Signup and FIRST Login.
echo.
echo   To run:
echo   - Backend: cd backend; venv\Scripts\activate; python manage.py runserver
echo   - Frontend: cd frontend; npm run dev
echo.
echo   Login:
echo   Admin:   admin@cdss.com    / admin123
echo   Doctor:  doctor@test.com   / doctor123 (OTP on 1st Login)
echo   Staff:   staff@test.com    / staff123  (OTP on 1st Login)
echo.
cd /d "!BASE_DIR!"
pause


