import os
import joblib
import pandas as pd
import numpy as np
from django.conf import settings

# Paths to models
MODEL_DIR = os.path.join(settings.BASE_DIR, 'ml_model', 'trained_model')
DEVICE_MODEL_PATH = os.path.join(MODEL_DIR, 'model.pkl')
ENCODER_PATH = os.path.join(MODEL_DIR, 'encoder.pkl')

def get_ai_prediction(patient_data):
    """
    Takes a dictionary of patient vitals/ABGs and returns a risk level and recommendation.
    """
    try:
        # Load model and encoder if available
        model = None
        le = None
        confidence = 85 # Default confidence for heuristic
        
        ml_device = None
        if os.path.exists(DEVICE_MODEL_PATH) and os.path.exists(ENCODER_PATH):
            try:
                model = joblib.load(DEVICE_MODEL_PATH)
                le = joblib.load(ENCODER_PATH)
                
                # Map data to features (SpO2, pH, PaCO2, PaO2, HCO3, Respiratory_Rate, Heart_Rate)
                input_data = pd.DataFrame([{
                    "SpO2": patient_data.get('spo2', 95),
                    "pH": patient_data.get('ph', 7.4),
                    "PaCO2": patient_data.get('paco2', 40),
                    "PaO2": patient_data.get('pao2', 85),
                    "HCO3": patient_data.get('hco3', 24),
                    "Respiratory_Rate": patient_data.get('resp_rate', 16),
                    "Heart_Rate": patient_data.get('heart_rate', 75)
                }])

                # Predict with model if loaded
                try:
                    preds = model.predict(input_data)
                    if hasattr(le, 'inverse_transform') and isinstance(preds[0], (int, np.integer)):
                        ml_device = le.inverse_transform(preds)[0]
                    else:
                        ml_device = preds[0]
                        
                    probs = model.predict_proba(input_data)
                    confidence = int(np.max(probs) * 100)
                except:
                    pass
            except:
                pass

        # Heuristic rules for 4 specific devices
        spo2 = patient_data.get('spo2', 95)
        ph = patient_data.get('ph', 7.4)
        
        if ml_device:
            rec_device = ml_device
            # Provide an appropriate flow rate based on the predicted device
            d = str(rec_device).lower()
            if 'non-rebreather' in d:
                flow_rate = "10-15 L/min (60-90%)"
            elif 'high-flow' in d or 'hfnc' in d:
                flow_rate = "30-60 L/min"
            elif 'venturi' in d:
                flow_rate = "24-60% FiO2"
            elif 'bipap' in d or 'cpap' in d or 'niv' in d:
                flow_rate = "NIV Settings Required"
            else:
                flow_rate = "1-4 L/min"
        else:
            if spo2 < 85:
                rec_device = "Non-Rebreather Mask"
                flow_rate = "10-15 L/min (60-90%)"
            elif spo2 < 88 or (ph < 7.35):
                rec_device = "High-Flow Nasal Cannula (HFNC)"
                flow_rate = "30-60 L/min"
            elif spo2 <= 92:
                rec_device = "Venturi Mask"
                flow_rate = "24-60% FiO2"
            else:
                rec_device = "Nasal Cannula"
                flow_rate = "1-4 L/min"

        return {
            "recommended_device": rec_device,
            "flow_rate": flow_rate,
            "confidence_score": confidence,
            "risk_level": "HIGH" if spo2 < 88 else "MODERATE" if spo2 < 92 else "LOW"
        }
    except Exception as e:
        return {"error": str(e)}

def analyze_trends(patient_vitals, patient_abgs):
    """
    Analyzes historical data to determine trends.
    """
    if not patient_vitals and not patient_abgs:
        return {"summary": "Insufficient data for trend analysis", "overall_trend": "Stable"}

    # Basic logic: compare latest two entries
    summary = "Patient shows stable respiratory parameters."
    overall = "Stable"
    
    if len(patient_vitals) >= 2:
        v1 = patient_vitals[0].spo2
        v2 = patient_vitals[1].spo2
        if v1 < v2:
            summary = "Patient shows declining SpO2 levels."
            overall = "Worsening"
        elif v1 > v2:
            summary = "Patient show improving SpO2 levels."
            overall = "Improving"

    return {
        "overall_trend": overall,
        "spo2_trend": "Improving" if overall == "Improving" else "Declining" if overall == "Worsening" else "Stable",
        "paco2_trend": "Stable",
        "ph_trend": "Stable",
        "summary": summary
    }
