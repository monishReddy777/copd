@echo off
echo ====================================================
echo   COPD CDSS - Automated Setup Script (Windows)
echo ====================================================
echo.

:: Get absolute path to the script folder (ends with \)
set "ROOT_DIR=%~dp0"

:: ─── Check Python ────────────────────────────────────
python --version >nul 2>&1
if errorlevel 1 goto NO_PYTHON

:: ─── Check Node.js ──────────────────────────────────
node --version >nul 2>&1
if errorlevel 1 goto NO_NODE

:: ─── MySQL Warning ──────────────────────────────────
echo [IMPORTANT] Make sure MySQL is running and you have created the database.
echo.
echo   Before running this script, open MySQL and run:
echo     CREATE DATABASE cdss_copd;
echo.
echo   Then update backend/backend/settings.py with YOUR credentials.
echo.
set /p CONTINUE_CHOICE="Have you done the above? (y/n): "
if /i not "%CONTINUE_CHOICE%"=="y" (
    echo Please complete the MySQL setup first.
    pause
    exit /b 0
)

echo.
echo ─── Step 1: Setting up Backend ────────────────────
echo.

if not exist "%ROOT_DIR%backend" goto BACKEND_MISSING
cd /d "%ROOT_DIR%backend"

echo [1/5] Handling Virtual Environment...
if not exist venv (
    echo   - Creating new virtual environment...
    python -m venv venv
) else (
    echo   - Virtual environment already exists.
)

if errorlevel 1 goto VENV_FAIL

:: Verify venv python exists
set "VENV_PYTHON=%ROOT_DIR%backend\venv\Scripts\python.exe"
if not exist "%VENV_PYTHON%" goto VENV_MISSING

echo [2/5] Updating Pip...
"%VENV_PYTHON%" -m pip install --upgrade pip

echo [3/5] Installing dependencies...
echo   (This may take a minute)
"%VENV_PYTHON%" -m pip install -r requirements.txt
if errorlevel 1 goto PIP_FAIL

"%VENV_PYTHON%" -m pip install numpy
if errorlevel 1 echo   - Note: numpy install check/skipped

echo [4/5] Verifying AI Models...
if not exist "ml_model\trained_model\model.pkl" (
    echo   [WARNING] AI Device Model model.pkl missing!
)

echo [5/5] Database Migrations...
echo   (Make sure MySQL is running and you have created 'cdss_copd' database)
"%VENV_PYTHON%" manage.py makemigrations api
"%VENV_PYTHON%" manage.py migrate
if errorlevel 1 goto MIGRATE_RETRY

:SEED_DB
"%VENV_PYTHON%" seed_database.py

echo.
echo ─── Step 2: Setting up Frontend ──────────────────
echo.

if not exist "%ROOT_DIR%frontend" goto FRONTEND_MISSING
cd /d "%ROOT_DIR%frontend"

echo [6/6] Installing Node.js dependencies...
echo   (This may take a minute)
call npm install
if errorlevel 1 goto NPM_FAIL

goto SETUP_COMPLETE

:: --- RECOVERY LABELS ---

:MIGRATE_RETRY
echo.
echo [ERROR] Database migration failed.
echo This often happens if some tables (like 'email_otp') already exist.
echo.
set /p RETRY_CHOICE="Would you like to try 'faking' the initial setup? (y/n): "
if /i "%RETRY_CHOICE%"=="y" (
    echo.
    echo   - Attempting to fake API migrations...
    "%VENV_PYTHON%" manage.py migrate --fake api
    echo   - Retrying regular migrations...
    "%VENV_PYTHON%" manage.py migrate
    if not errorlevel 1 (
        echo [SUCCESS] Migrations completed.
        goto SEED_DB
    )
)
goto MIGRATE_FAIL

:: --- ERROR LABELS ---

:NO_PYTHON
echo [ERROR] Python is not installed or not in PATH!
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
echo [ERROR] Failed to handle virtual environment.
pause
exit /b 1

:VENV_MISSING
echo [ERROR] Virtual environment python not found at:
echo "%VENV_PYTHON%"
pause
exit /b 1

:PIP_FAIL
echo [ERROR] Failed to install Python dependencies.
pause
exit /b 1

:MIGRATE_FAIL
echo [ERROR] Database migration failed completely.
echo Please check your MySQL connection and credentials in backend/backend/settings.py
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
echo   1. Backend: cd backend; venv\Scripts\activate; python manage.py runserver
echo   2. Frontend: cd frontend; npm run dev
echo.
pause
