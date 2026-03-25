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
if errorlevel 1 echo   (Note: numpy install check/skipped)

echo [4/5] Verifying AI Models...
if not exist "ml_model\trained_model\model.pkl" (
    echo [WARNING] AI Device Model (model.pkl) missing!
)

echo [5/5] Database Migrations...
echo   (Make sure MySQL is running and you have created 'cdss_copd' database)
"%VENV_PYTHON%" manage.py makemigrations api
"%VENV_PYTHON%" manage.py migrate
if errorlevel 1 goto MIGRATE_FAIL
"%VENV_PYTHON%" seed_database.py

echo.
echo ─── Step 2: Setting up Frontend ──────────────────
echo.

if not exist "%ROOT_DIR%frontend" goto FRONTEND_MISSING
cd /d "%ROOT_DIR%frontend"

echo [6/6] Installing Node.js dependencies...
call npm install
if errorlevel 1 goto NPM_FAIL

goto SETUP_COMPLETE

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
echo [ERROR] Database migration failed.
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
