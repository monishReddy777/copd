import os
import sys
import django
import joblib
import pandas as pd
import numpy as np

# Set up Django environment
sys.path.append(r'c:\Users\monis\Downloads\cdss2 (6)\COPD_Project_Final\backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'backend.settings')
django.setup()

from api.ai_utils import get_ai_prediction

# Sample data that should trigger a specific recommendation
# Low SpO2, abnormal pH
test_data = {
    'spo2': 82,
    'ph': 7.25,
    'paco2': 55,
    'pao2': 60,
    'hco3': 28,
    'resp_rate': 24,
    'heart_rate': 110
}

print("--- Testing AI Prediction Flow ---")
result = get_ai_prediction(test_data)

print(f"Input: SpO2={test_data['spo2']}, pH={test_data['ph']}")
print(f"Output Device: {result.get('recommended_device')}")
print(f"Flow Rate: {result.get('flow_rate')}")
print(f"Confidence: {result.get('confidence_score')}%")
print(f"Risk Level: {result.get('risk_level')}")

# Check if model files are actually loaded
MODEL_DIR = os.path.join(r'c:\Users\monis\Downloads\cdss2 (6)\COPD_Project_Final\backend', 'ml_model', 'trained_model')
DEVICE_MODEL_PATH = os.path.join(MODEL_DIR, 'model.pkl')
ENCODER_PATH = os.path.join(MODEL_DIR, 'encoder.pkl')

if os.path.exists(DEVICE_MODEL_PATH) and os.path.exists(ENCODER_PATH):
    print("\n[SUCCESS] ML Model files found.")
    try:
        model = joblib.load(DEVICE_MODEL_PATH)
        print(f"[SUCCESS] ML Model loaded: {type(model).__name__}")
    except Exception as e:
        print(f"[ERROR] Failed to load ML Model: {e}")
else:
    print("\n[ERROR] ML Model files NOT found at expected path.")
