import os
import joblib
import pandas as pd
import numpy as np
from django.conf import settings


def get_ai_prediction(patient_data):
    """
    Rules-based clinical decision support for COPD oxygen therapy.
    Follows specific sequence: NIV Check -> Oxygenation Check -> Overrides.
    """
    try:
        spo2 = patient_data.get('spo2', 95)
        ph = patient_data.get('ph', 7.4)
        paco2 = patient_data.get('paco2', 40)
        pao2 = patient_data.get('pao2', 85)
        hco3 = patient_data.get('hco3', 24)
        rr = patient_data.get('resp_rate', 20)
        
        rec_device = "Nasal Cannula"
        flow_rate = "1-4 L/min"
        risk_level = "LOW"
        diagnosis_context = "Stable COPD"

        # New Clinical Rules Hierarchy
        if spo2 < 80:
            rec_device = "Non-Rebreather Mask"
            flow_rate = "10-15 L/min"
            risk_level = "CRITICAL"
            diagnosis_context = "Severe Hypoxemia"
        elif (ph < 7.35) and (paco2 > 45):
            rec_device = "Venturi Mask"
            flow_rate = "24-40% FiO2"
            risk_level = "HIGH"
            diagnosis_context = "Respiratory Acidosis / Hypercapnia"
        elif 80 <= spo2 < 88:
            rec_device = "Venturi Mask"
            flow_rate = "24-40% FiO2"
            risk_level = "HIGH"
            diagnosis_context = "Moderate Hypoxemia"
        elif (88 <= spo2 <= 92) and (rr > 28):
            rec_device = "High-Flow Nasal Cannula"
            flow_rate = "30-60 L/min"
            risk_level = "MODERATE"
            diagnosis_context = "Respiratory Distress / High Work of Breathing"
        elif spo2 > 94:
            rec_device = "Nasal Cannula"
            flow_rate = "Room Air"
            risk_level = "LOW"
            diagnosis_context = "Stable / Room Air"
        # NIV (Non-Invasive Ventilation) Decision Logic
        if ph < 7.35 and paco2 > 45:
            niv_status = "BiPAP Indicated"
            niv_rationale = f"Acute hypercapnic respiratory failure with pH {ph:.2f} (< 7.35) and PaCO2 {paco2:.1f} (> 45 mmHg)."
            niv_settings = {"ipap": 14, "epap": 4, "backup_rate": 12}
        else:
            niv_status = "Not Currently Indicated"
            niv_rationale = f"pH is {ph:.2f} (Target > 7.35) and PaCO2 is {paco2:.1f}. Continue careful oxygen therapy and monitor."
            niv_settings = None

        # ICU Admission Criteria
        icu_status = "No ICU Review Required"
        icu_triggers = []
        if spo2 < 80:
            icu_status = "ICU Review Required"
            icu_triggers.append("SpO2 < 80% on high flow")

        return {
            "recommended_device": rec_device,
            "flow_rate": flow_rate,
            "confidence_score": 99,
            "risk_level": risk_level,
            "clinical_context": diagnosis_context,
            "target_spo2": "88–92% (COPD Safety)",
            "niv_status": niv_status,
            "niv_rationale": niv_rationale,
            "niv_settings": niv_settings,
            "icu_status": icu_status,
            "icu_triggers": icu_triggers
        }
    except Exception as e:
        return {"error": str(e)}

def analyze_trends(patient_vitals, patient_abgs):
    """
    Analyzes historical data to determine trends.
    Expected: patient_vitals and patient_abgs are ordered by -created_at (descending)
    """
    if not patient_vitals and not patient_abgs:
        return {"summary": "Insufficient data for trend analysis", "overall_trend": "Stable"}

    # Basic logic: compare latest vs previous
    summary = "Patient shows stable respiratory parameters."
    overall = "Stable"
    spo2_trend = "Stable"
    
    # Latest is index 0, Previous is index 1
    if len(patient_vitals) >= 2:
        latest = patient_vitals[0]
        previous = patient_vitals[1]
        
        if latest.spo2 < previous.spo2:
            summary = f"Patient shows declining SpO2 levels (from {previous.spo2}% to {latest.spo2}%)."
            overall = "Worsening"
            spo2_trend = "Declining"
        elif latest.spo2 > previous.spo2:
            summary = f"Patient shows improving SpO2 levels (from {previous.spo2}% to {latest.spo2}%)."
            overall = "Improving"
            spo2_trend = "Improving"
        else:
            summary = f"Patient shows stable SpO2 levels ({latest.spo2}%)."
            overall = "Stable"
            spo2_trend = "Stable"
    elif len(patient_vitals) == 1:
        summary = f"Initial vitals recorded (SpO2: {patient_vitals[0].spo2}%). More data needed for trend analysis."
        overall = "Stable"
        spo2_trend = "Initial"

    # Add ABG Paco2 trend if available
    paco2_trend = "Stable"
    if len(patient_abgs) >= 2:
        latest_abg = patient_abgs[0]
        prev_abg = patient_abgs[1]
        if latest_abg.paco2 > prev_abg.paco2 + 2:
            paco2_trend = "Rising"
            if overall != "Worsening":
                summary += " Note: PaCO2 is rising."
        elif latest_abg.paco2 < prev_abg.paco2 - 2:
            paco2_trend = "Falling"

    return {
        "overall_trend": overall,
        "spo2_trend": spo2_trend,
        "paco2_trend": paco2_trend,
        "ph_trend": "Stable",
        "summary": summary
    }
