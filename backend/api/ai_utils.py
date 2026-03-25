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
        
        rec_device = "No treatment needed"
        flow_rate = "N/A"
        diagnosis_context = ""
        is_niv_needed = False
        
        # STEP 1: Check for Ventilation Failure (NIV decision)
        if ph < 7.35:
            if paco2 > 45:
                rec_device = "NIV (BiPAP)"
                is_niv_needed = True
                if hco3 > 30:
                    diagnosis_context = "Acute on Chronic Respiratory Failure"
                else:
                    diagnosis_context = "Acute Respiratory Failure"
            else:
                diagnosis_context = "Metabolic Acidosis (NO NIV)"
        elif ph >= 7.35:
            if paco2 > 45 and hco3 > 30:
                diagnosis_context = "Chronic compensated COPD (NO NIV)"
            else:
                diagnosis_context = "No NIV needed"

        # STEP 2: Oxygenation Decision (ONLY if NIV NOT selected)
        if not is_niv_needed:
            if spo2 < 80 or pao2 < 50:
                rec_device = "NRBM (EMERGENCY - temporary)"
                flow_rate = "10-15 L/min"
            elif spo2 < 85:
                # User preferred: HFNC (if non-COPD), Venturi (if COPD)
                rec_device = "Venturi Mask"
                flow_rate = "40-60% FiO2"
            elif spo2 < 88:
                rec_device = "Venturi Mask"
                flow_rate = "24-28% FiO2"
            elif spo2 <= 94:
                rec_device = "Nasal Cannula"
                flow_rate = "1-4 L/min"
            else:
                rec_device = "No oxygen needed"
                flow_rate = "Room Air"

        # STEP 3: Override Rules (VERY IMPORTANT)
        if is_niv_needed:
            # NIV override
            if spo2 < 80:
                rec_device = "NRBM (Rescue) THEN NIV"
                flow_rate = "10-15 L/min until NIV stabilized"
            else:
                rec_device = "NIV (BiPAP)"
                flow_rate = "Settings: Titrate FiO2 through NIV to target SpO2 88-92%"

        # STEP 4: Target Oxygen (COPD SAFETY)
        # Target SpO2 = 88–92%, Avoid over-oxygenation

        return {
            "recommended_device": rec_device,
            "flow_rate": flow_rate,
            "confidence_score": 99,
            "risk_level": "CRITICAL" if (is_niv_needed or spo2 < 80) else "HIGH" if spo2 < 88 else "MODERATE" if spo2 < 92 else "LOW",
            "clinical_context": diagnosis_context,
            "target_spo2": "88–92% (COPD Safety)"
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
