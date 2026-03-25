@echo off
setlocal enabledelayedexpansion

echo ====================================================
echo   COPD CDSS - Automated Setup Script (Windows)
echo ====================================================
echo.

set "BASE_DIR=%~dp0"
if "!BASE_DIR:~-1!"=="\" set "BASE_DIR=!BASE_DIR:~0,-1!"

:: ─── Check Python ────────────────────────────────────
python --version >nul 2>&1
if errorlevel 1 goto NO_PYTHON

:: ─── Check Node.js ──────────────────────────────────
node --version >nul 2>&1
if errorlevel 1 goto NO_NODE

echo.
echo ─── Step 1: Setting up Backend ────────────────────
echo.

if not exist "!BASE_DIR!\backend" goto BACKEND_MISSING
cd /d "!BASE_DIR!\backend"

echo [1/5] Handling Virtual Environment...
if not exist venv (
    echo   - Creating new virtual environment...
    python -m venv venv
    if errorlevel 1 goto VENV_FAIL
) else (
    echo   - Virtual environment already exists.
)

:: Verify venv python exists
set "VENV_PYTHON=venv\Scripts\python.exe"
if not exist "!VENV_PYTHON!" goto VENV_MISSING

echo [2/5] Updating Pip...
"!VENV_PYTHON!" -m pip install --upgrade pip
if errorlevel 1 echo   (Note: minor pip upgrade failure ignored)

echo [3/5] Installing dependencies...
echo   (This may take a minute)
"!VENV_PYTHON!" -m pip install -r requirements.txt
if errorlevel 1 goto PIP_FAIL
"!VENV_PYTHON!" -m pip install numpy
if errorlevel 1 echo   (Note: numpy install check/skipped)

echo [4/5] Verifying AI Models...
if not exist "ml_model\trained_model\model.pkl" (
    echo [WARNING] AI Device Model (model.pkl) missing! Heuristic fallback will be used.
)

echo [5/5] Database Migrations...
echo   (Make sure MySQL is running and you have created 'cdss_copd' database)
"!VENV_PYTHON!" manage.py makemigrations api
"!VENV_PYTHON!" manage.py migrate
if errorlevel 1 goto MIGRATE_FAIL
"!VENV_PYTHON!" seed_database.py

echo.
echo ─── Step 2: Setting up Frontend ──────────────────
echo.

if not exist "!BASE_DIR!\frontend" goto FRONTEND_MISSING
cd /d "!BASE_DIR!\frontend"

echo [6/6] Installing Node.js dependencies...
echo   (This may take a minute)
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
echo [ERROR] 'backend' folder not found.
pause
exit /b 1

:VENV_FAIL
echo [ERROR] Failed to create venv. Try running as Administrator.
pause
exit /b 1

:VENV_MISSING
echo [ERROR] Virtual environment python not found at '!VENV_PYTHON!'
pause
exit /b 1

:PIP_FAIL
echo [ERROR] Failed to install Python dependencies.
pause
exit /b 1

:MIGRATE_FAIL
echo [ERROR] Database migration failed. Check MySQL connection and credentials in backend/backend/settings.py
pause
exit /b 1

:FRONTEND_MISSING
echo [ERROR] 'frontend' folder not found.
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
echo   To run the app:
echo   1. Start Backend: cd backend; venv\Scripts\activate; python manage.py runserver
echo   2. Start Frontend: cd frontend; npm run dev
echo.
cd /d "!BASE_DIR!"
pause
