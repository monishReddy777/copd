# COPD Clinical Decision Support System (CDSS)

A full-stack AI-powered Clinical Decision Support System for COPD patient management, featuring real-time vitals monitoring, AI therapy recommendations, and clinician workflows.

---

## 🛠 Prerequisites

Before setting up, make sure you have the following installed:

| Software       | Version   | Download Link                          |
|----------------|-----------|----------------------------------------|
| **Python**     | 3.10+     | https://www.python.org/downloads/      |
| **Node.js**    | 18+       | https://nodejs.org/                    |
| **MySQL**      | 8.0+      | https://dev.mysql.com/downloads/       |
| **Git**        | Latest    | https://git-scm.com/                   |

---

## ⚡ Quick Setup (Windows)

### Option 1: Automated Setup
```bash
# Just double-click or run:
setup.bat
```
This will install all dependencies, run migrations, and seed the database.

### Option 2: Manual Setup

#### Step 1: Create MySQL Database
Open MySQL command line or MySQL Workbench and run:
```sql
CREATE DATABASE cdss_copd;
```

#### Step 2: Update Database Credentials
Edit `backend/backend/settings.py` — find the `DATABASES` section (~line 61) and update:
```python
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': 'cdss_copd',          # ← your database name
        'USER': 'root',               # ← your MySQL username
        'PASSWORD': 'your_password',  # ← your MySQL password
        'HOST': 'localhost',
        'PORT': '3306',
    }
}
```

#### Step 3: Set Up Backend
```bash
cd backend

# Create & activate virtual environment
python -m venv venv
venv\Scripts\activate          # Windows
# source venv/bin/activate     # Mac/Linux

# Install dependencies
pip install -r requirements.txt
pip install numpy

# Run database migrations
python manage.py makemigrations api
python manage.py migrate

# Seed database with test users & sample data
python seed_database.py
```

#### Step 4: Set Up Frontend
```bash
cd frontend

# Install Node.js dependencies
npm install
```

---

## 🚀 Running the Application

You need **two terminals** running simultaneously:

### Terminal 1 — Backend (Django API)
```bash
cd backend
venv\Scripts\activate
python manage.py runserver
```
Backend runs at: **http://localhost:8000/**

### Terminal 2 — Frontend (React/Vite)
```bash
cd frontend
npm run dev
```
Frontend runs at: **http://localhost:5173/**

Open **http://localhost:5173** in your browser.

---

## 🔑 Login Credentials

| Role       | Email                | Password    |
|------------|----------------------|-------------|
| **Admin**  | admin@cdss.com       | admin123    |
| **Doctor** | doctor@test.com      | doctor123   |
| **Staff**  | staff@test.com       | staff123    |

---

## 📁 Project Structure

```
COPD_Project_Final/
├── backend/                  # Django REST API
│   ├── api/                  # Main Django app
│   │   ├── models.py         # Database models (Patient, Vitals, ABG, etc.)
│   │   ├── views.py          # API endpoints
│   │   ├── serializers.py    # DRF serializers
│   │   ├── urls.py           # URL routing
│   │   └── ai_utils.py       # AI/ML prediction utilities
│   ├── ml_model/             # Trained ML models (.pkl)
│   ├── backend/              # Django project settings
│   ├── seed_database.py      # Database seeder script
│   ├── requirements.txt      # Python dependencies
│   └── manage.py
├── frontend/                 # React + Vite frontend
│   ├── src/
│   │   ├── pages/            # Page components
│   │   ├── api/              # Axios API service layer
│   │   ├── components/       # Reusable UI components
│   │   └── App.jsx           # Main app with routing
│   └── package.json
├── mobile_app/               # Android mobile app (Kotlin)
├── setup.bat                 # Automated setup script
└── README.md                 # This file
```

---

## 🧠 Features

- **Dashboard** — Real-time patient overview with status indicators
- **Patient Management** — Add, view, and manage COPD patients
- **Vitals Monitoring** — Record and track SpO2, HR, RR, BP, Temperature
- **ABG Records** — Arterial Blood Gas data entry and history
- **AI Therapy Recommendations** — XGBoost-based device recommendation engine
- **AI Risk Analysis** — Automated risk level assessment (LOW/MODERATE/HIGH)
- **Oxygen Status Monitoring** — Real-time oxygen requirement tracking
- **Trend Analysis** — Historical data trend visualization
- **Alert System** — Automated critical/warning alerts for SpO2 drops
- **Doctor Notifications** — Real-time notifications for critical patients
- **Role-Based Access** — Admin, Doctor, and Staff role management

---

## ⚠️ Troubleshooting

### MySQL Connection Error
- Make sure MySQL is running
- Verify credentials in `backend/backend/settings.py`
- Ensure the database exists: `SHOW DATABASES;`

### `mysqlclient` installation fails
On Windows, install the binary wheel:
```bash
pip install mysqlclient
```
If it fails, download the `.whl` file from: https://pypi.org/project/mysqlclient/#files

### Port already in use
```bash
# Kill process on port 8000 (backend)
netstat -ano | findstr :8000
taskkill /PID <PID> /F

# Kill process on port 5173 (frontend)
netstat -ano | findstr :5173
taskkill /PID <PID> /F
```

### Frontend blank page
- Clear browser cache / open in incognito
- Check browser console for errors (F12)
- Make sure the backend is running on port 8000

---

## 📧 Support
For any issues, contact the development team.
