from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status

from .models import (
    Patient, BaselineDetails, GoldClassification, SpirometryData,
    GasExchangeHistory, CurrentSymptoms, Vitals, AbgEntry
)
from therapy.models import TrendAnalysis
from .serializers import (
    PatientSerializer, AddPatientSerializer,
    BaselineDetailsInputSerializer, GoldClassificationInputSerializer,
    SpirometryDataInputSerializer, GasExchangeHistoryInputSerializer,
    CurrentSymptomsInputSerializer, VitalsInputSerializer,
    ABGEntryInputSerializer, ReassessmentChecklistInputSerializer
)


# ──────────────────────────────────────────────────────────────────────────────
# Patient CRUD
# ──────────────────────────────────────────────────────────────────────────────

class AddPatientAPIView(APIView):
    """
    POST /api/patients/add/
    Body: { full_name, dob, sex, ward, bed_number, assigned_doctor_id, created_by_staff_id }
    """
    def post(self, request):
        serializer = AddPatientSerializer(data=request.data)
        if serializer.is_valid():
            patient = Patient.objects.create(
                full_name=serializer.validated_data['full_name'],
                dob=serializer.validated_data['date_of_birth'],
                sex=serializer.validated_data['sex'],
                ward=serializer.validated_data['ward'],
                bed_number=serializer.validated_data['bed_number'],
                assigned_doctor_id=serializer.validated_data.get('assigned_doctor_id'),
                created_by_staff_id=serializer.validated_data.get('created_by_staff_id'),
                status='stable',
            )
            return Response({
                "message": "Patient added successfully.",
                "patient_id": patient.id,
                "full_name": patient.full_name,
                "ward": patient.ward,
                "bed_number": patient.bed_number,
                "status": patient.status,
            }, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class PatientListAPIView(APIView):
    """
    GET /api/patients/?status=critical|warning|stable
    Returns all patients, optionally filtered by status.
    """
    def get(self, request):
        from datetime import date
        filter_status = request.query_params.get('status', None)
        patients = Patient.objects.all()
        
        # Import StaffChecklist and Reassessment for latest SpO2
        try:
            from staff.models import StaffChecklist, Reassessment
            has_staff_model = True
        except ImportError:
            has_staff_model = False

        data = []
        for p in patients:
            # Fetch the single latest vitals record
            latest_vital = Vitals.objects.filter(patient_id=p.id).order_by('-created_at').first()
            
            spo2_val = None
            rr_val = None
            
            # Check all sources for the most recent SpO2
            vitals_time = latest_vital.created_at if latest_vital else None
            
            if has_staff_model:
                # Check StaffChecklist (reassessment_checklist table)
                latest_staff = StaffChecklist.objects.filter(
                    patient_id=p.id, spo2__isnull=False
                ).exclude(spo2=0).order_by('-created_at').first()
                
                # Check Reassessment table
                latest_reassessment = Reassessment.objects.filter(
                    patient_id=p.id, status='completed', spo2__isnull=False
                ).order_by('-reassessment_time').first()
                
                # Find which source has the most recent SpO2
                sources = []
                if latest_vital and latest_vital.spo2 is not None:
                    sources.append((vitals_time, latest_vital.spo2, latest_vital.respiratory_rate))
                if latest_staff and latest_staff.spo2 is not None and latest_staff.spo2 > 0:
                    sources.append((latest_staff.created_at, latest_staff.spo2, latest_staff.respiratory_rate))
                if latest_reassessment and latest_reassessment.spo2 is not None:
                    sources.append((latest_reassessment.reassessment_time or latest_reassessment.created_at, latest_reassessment.spo2, latest_reassessment.respiratory_rate))
                
                if sources:
                    # Sort by time descending, pick the most recent
                    sources.sort(key=lambda x: x[0] if x[0] else vitals_time, reverse=True)
                    spo2_val = sources[0][1]
                    rr_val = sources[0][2]
            
            # Fallback to vitals only
            if spo2_val is None and latest_vital and latest_vital.spo2 is not None:
                spo2_val = latest_vital.spo2
            if rr_val is None and latest_vital and latest_vital.respiratory_rate is not None:
                rr_val = latest_vital.respiratory_rate
            
            # Dynamic status based on SpO2
            # < 85 = Critical, 85-87 = Warning, >= 88 = Stable
            if spo2_val is not None:
                if spo2_val < 85:
                    display_status = 'CRITICAL'
                elif spo2_val < 88:
                    display_status = 'WARNING'
                else:
                    display_status = 'STABLE'
            else:
                display_status = p.status.upper() if p.status else "STABLE"

            data.append({
                "id": p.id,
                "patient_id": p.id, # Keep for backward compatibility with existing models
                "name": p.full_name,
                "ward_no": p.ward,
                "room_no": p.bed_number,
                "spo2": str(int(spo2_val)) if spo2_val is not None else "--",
                "respiratory_rate": str(int(rr_val)) if rr_val is not None else "--",
                "status": display_status
            })

        # Sorting: Critical -> Warning -> Stable
        status_priority = {'CRITICAL': 0, 'WARNING': 1, 'STABLE': 2}
        data.sort(key=lambda x: status_priority.get(x['status'], 3))

        return Response(data, status=status.HTTP_200_OK)


class PatientDetailAPIView(APIView):
    """
    GET    /api/patients/<patient_id>/
    DELETE /api/patients/<patient_id>/
    Returns full patient details with the most recent vitals, ABG, and symptoms.
    DELETE removes the patient and all related data from the database.
    """
    def get(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        # Get latest records (Use Model.objects.filter instead of reverse relation if no FK)
        latest_vitals = Vitals.objects.filter(patient_id=patient.id).order_by('-created_at').first()
        latest_abg = AbgEntry.objects.filter(patient_id=patient.id).order_by('-created_at').first()
        latest_symptoms = CurrentSymptoms.objects.filter(patient_id=patient.id).order_by('-created_at').first()
        latest_gold = GoldClassification.objects.filter(patient_id=patient.id).order_by('-created_at').first()

        vitals_data = None
        if latest_vitals:
            vitals_data = {
                "spo2": latest_vitals.spo2,
                "respiratory_rate": latest_vitals.respiratory_rate,
                "heart_rate": latest_vitals.heart_rate,
                "temperature": latest_vitals.temperature,
                "blood_pressure": latest_vitals.blood_pressure,
                "recorded_at": latest_vitals.created_at,
            }

        abg_data = None
        if latest_abg:
            abg_data = {
                "ph": latest_abg.ph,
                "pao2": latest_abg.pao2,
                "paco2": latest_abg.paco2,
                "hco3": latest_abg.hco3,
                "fio2": latest_abg.fio2,
                "recorded_at": latest_abg.created_at,
            }

        symptoms_data = None
        if latest_symptoms:
            symptoms_data = {
                "mmrc_grade": latest_symptoms.mmrc_score,
                "cough": latest_symptoms.increased_cough,
                "sputum": latest_symptoms.increased_sputum,
                "wheezing": latest_symptoms.wheezing,
                "fever": latest_symptoms.fever,
                "chest_tightness": latest_symptoms.chest_tightness,
                "recorded_at": latest_symptoms.created_at,
            }

        return Response({
            "patient_id": patient.id,
            "full_name": patient.full_name,
            "dob": patient.dob,
            "sex": patient.sex,
            "ward": patient.ward,
            "bed_number": patient.bed_number,
            "status": patient.status,
            "gold_stage": latest_gold.gold_stage if latest_gold else None,
            "latest_vitals": vitals_data,
            "latest_abg": abg_data,
            "latest_symptoms": symptoms_data,
            "created_at": patient.created_at,
        }, status=status.HTTP_200_OK)

    def delete(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        # Delete all related data
        Vitals.objects.filter(patient_id=patient_id).delete()
        AbgEntry.objects.filter(patient_id=patient_id).delete()
        CurrentSymptoms.objects.filter(patient_id=patient_id).delete()
        BaselineDetails.objects.filter(patient_id=patient_id).delete()
        GoldClassification.objects.filter(patient_id=patient_id).delete()
        SpirometryData.objects.filter(patient_id=patient_id).delete()
        GasExchangeHistory.objects.filter(patient_id=patient_id).delete()

        # Delete therapy-related data
        try:
            from therapy.models import (
                OxygenStatus, AIAnalysis, ABGTrend, TrendAnalysis,
                HypoxemiaCause, OxygenRequirement, DeviceSelection,
                ReviewRecommendation, TherapyRecommendation,
                NIVRecommendation, EscalationCriteria,
                ScheduleReassessment, UrgentAction
            )
            OxygenStatus.objects.filter(patient_id=patient_id).delete()
            AIAnalysis.objects.filter(patient_id=patient_id).delete()
            ABGTrend.objects.filter(patient_id=patient_id).delete()
            TrendAnalysis.objects.filter(patient_id=patient_id).delete()
            HypoxemiaCause.objects.filter(patient_id=patient_id).delete()
            OxygenRequirement.objects.filter(patient_id=patient_id).delete()
            DeviceSelection.objects.filter(patient_id=patient_id).delete()
            ReviewRecommendation.objects.filter(patient_id=patient_id).delete()
            TherapyRecommendation.objects.filter(patient_id=patient_id).delete()
            NIVRecommendation.objects.filter(patient_id=patient_id).delete()
            EscalationCriteria.objects.filter(patient_id=patient_id).delete()
            ScheduleReassessment.objects.filter(patient_id=patient_id).delete()
            UrgentAction.objects.filter(patient_id=patient_id).delete()
        except Exception:
            pass  # If therapy tables don't exist yet, that's okay

        # Delete staff checklists
        try:
            from staff.models import StaffChecklist
            StaffChecklist.objects.filter(patient_id=patient_id).delete()
        except Exception:
            pass

        patient_name = patient.full_name
        patient.delete()

        return Response({
            "message": f"Patient '{patient_name}' and all related data removed successfully."
        }, status=status.HTTP_200_OK)


class PatientDetailsForDoctorAPIView(APIView):
    """
    GET /api/patient/details/<patient_id>/
    Returns patient details for Doctor view:
    name, ward_no, room_no, spo2, respiratory_rate, heart_rate,
    abg_values, device, flow, status
    """
    def get(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        from datetime import date
        age = (date.today() - patient.dob).days // 365 if patient.dob else None

        # Get latest vitals from vitals table
        latest_vitals = Vitals.objects.filter(patient_id=patient.id).order_by('-created_at').first()

        # Get latest staff reassessment from reassessment_checklist table
        from staff.models import StaffChecklist
        latest_staff = StaffChecklist.objects.filter(patient_id=patient.id).order_by('-created_at').first()

        # Get latest ABG entry
        latest_abg = AbgEntry.objects.filter(patient_id=patient.id).order_by('-created_at').first()

        # Determine which values are most recent: vitals table OR staff reassessment
        spo2_val = None
        rr_val = None
        hr_val = None

        vitals_time = latest_vitals.created_at if latest_vitals else None
        staff_time = latest_staff.created_at if latest_staff else None

        # Use staff values if they are more recent than vitals
        if staff_time and (not vitals_time or staff_time > vitals_time):
            # Staff reassessment is newer — use those values
            spo2_val = latest_staff.spo2 if latest_staff.spo2 is not None else None
            rr_val = latest_staff.respiratory_rate if latest_staff.respiratory_rate is not None else None
            hr_val = latest_staff.heart_rate if latest_staff.heart_rate is not None else None
        
        # Fall back to vitals table values if staff didn't provide them
        if spo2_val is None and latest_vitals and latest_vitals.spo2 is not None:
            spo2_val = latest_vitals.spo2
        if rr_val is None and latest_vitals and latest_vitals.respiratory_rate is not None:
            rr_val = latest_vitals.respiratory_rate
        if hr_val is None and latest_vitals and latest_vitals.heart_rate is not None:
            hr_val = latest_vitals.heart_rate

        # Dynamic status based on SpO2
        # < 85 = Critical, 85-87 = Warning, >= 88 = Stable
        if spo2_val is not None:
            if spo2_val < 85:
                display_status = 'CRITICAL'
            elif spo2_val < 88:
                display_status = 'WARNING'
            else:
                display_status = 'STABLE'
        else:
            display_status = patient.status.upper() if patient.status else "STABLE"

        # ABG values
        abg_values = None
        if latest_abg:
            abg_values = {
                "ph": latest_abg.ph,
                "pao2": latest_abg.pao2,
                "paco2": latest_abg.paco2,
                "hco3": latest_abg.hco3,
                "fio2": latest_abg.fio2,
            }

        # Device and flow — attempt to read from therapy recommendation if available
        device = None
        flow = None
        try:
            from therapy.models import TherapyRecommendation
            latest_therapy = TherapyRecommendation.objects.filter(patient_id=patient.id).order_by('-created_at').first()
            if latest_therapy:
                device = getattr(latest_therapy, 'device', None)
                flow = getattr(latest_therapy, 'flow_rate', None)
        except Exception:
            pass

        # Convert numeric values to strings for consistent JSON type
        spo2_str = str(int(spo2_val)) if spo2_val is not None else "--"
        rr_str = str(int(rr_val)) if rr_val is not None else "--"
        hr_str = str(int(hr_val)) if hr_val is not None else "--"

        return Response({
            "patient_id": patient.id,
            "name": patient.full_name,
            "age": age,
            "gender": patient.sex,
            "ward_no": patient.ward,
            "room_no": patient.bed_number,
            "diagnosis": "COPD Exacerbation",
            "spo2": spo2_str,
            "target_spo2": "88-92",
            "respiratory_rate": rr_str,
            "heart_rate": hr_str,
            "abg_values": abg_values,
            "device": device if device else "--",
            "flow": flow if flow else "--",
            "status": display_status,
        }, status=status.HTTP_200_OK)


class AIRiskAPIView(APIView):
    """
    GET /api/patient/ai-risk/<patient_id>/
    1. Fetches latest vitals and ABG for the patient
    2. Calculates risk_level + confidence_score
    3. Stores result in ai_analysis table
    4. Calculates trend values and stores in trend_analysis table
    5. Returns the computed data
    """
    def get(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        # ── 1. Fetch latest vitals ──
        latest_vitals = Vitals.objects.filter(patient_id=patient.id).order_by('-created_at').first()
        # ── 2. Fetch latest ABG ──
        latest_abg = AbgEntry.objects.filter(patient_id=patient.id).order_by('-created_at').first()

        if not latest_vitals and not latest_abg:
            return Response({
                "risk_level": "LOW",
                "confidence_score": 0,
                "acidosis": 0,
                "hypercapnia": 0,
                "key_factors": [],
                "message": "No analysis data available for this patient"
            }, status=status.HTTP_200_OK)

        # Extract current values (use safe defaults if missing)
        spo2 = latest_vitals.spo2 if latest_vitals and latest_vitals.spo2 is not None else 95
        respiratory_rate = latest_vitals.respiratory_rate if latest_vitals and latest_vitals.respiratory_rate is not None else 16
        heart_rate = latest_vitals.heart_rate if latest_vitals and latest_vitals.heart_rate is not None else 80
        ph = latest_abg.ph if latest_abg and latest_abg.ph is not None else 7.40
        paco2 = latest_abg.paco2 if latest_abg and latest_abg.paco2 is not None else 40
        pao2 = latest_abg.pao2 if latest_abg and latest_abg.pao2 is not None else 80

        # ── 3. AI Risk Calculation ──
        # Start from 0 and add risk based on actual clinical findings
        risk_score = 0
        key_factors = []

        # SpO2 assessment for COPD (target 88-92%)
        if spo2 < 85:
            risk_score += 35
            key_factors.append({"factor": "Severe Hypoxaemia (SpO\u2082 < 85%)", "level": "Critical"})
        elif spo2 < 88:
            risk_score += 25
            key_factors.append({"factor": "Hypoxaemia (SpO\u2082 < 88%)", "level": "Critical"})
        elif spo2 > 92:
            risk_score += 10
            key_factors.append({"factor": "SpO\u2082 above target (>92%) \u2013 CO\u2082 retention risk", "level": "Warning"})
        # SpO2 88-92 = on target, no risk added

        # pH assessment
        if ph < 7.25:
            risk_score += 30
            key_factors.append({"factor": "Severe Acidosis (pH < 7.25)", "level": "Critical"})
        elif ph < 7.35:
            risk_score += 15
            key_factors.append({"factor": "Acidosis (pH < 7.35)", "level": "Warning"})

        # PaCO2 assessment
        if paco2 > 60:
            risk_score += 25
            key_factors.append({"factor": "Severe Hypercapnia (PaCO\u2082 > 60 mmHg)", "level": "Critical"})
        elif paco2 > 45:
            risk_score += 15
            key_factors.append({"factor": "Hypercapnia (High CO\u2082)", "level": "Warning"})

        # PaO2 assessment
        if pao2 < 60:
            risk_score += 15
            key_factors.append({"factor": "Low PaO\u2082 (< 60 mmHg)", "level": "Warning"})

        # Respiratory rate assessment
        if respiratory_rate > 30:
            risk_score += 20
            key_factors.append({"factor": "Tachypnoea (RR > 30)", "level": "Critical"})
        elif respiratory_rate > 25:
            risk_score += 10
            key_factors.append({"factor": "Elevated Respiratory Rate (RR > 25)", "level": "Warning"})

        # Heart rate assessment
        if heart_rate > 120:
            risk_score += 10
            key_factors.append({"factor": "Tachycardia (HR > 120)", "level": "Warning"})

        # Cap at 100
        risk_score = min(risk_score, 100)

        # Determine risk level from score
        if risk_score >= 60:
            risk_level = "HIGH"
        elif risk_score >= 30:
            risk_level = "MODERATE"
        else:
            risk_level = "LOW"

        # Confidence score based on data availability
        confidence_score = 50
        if latest_vitals:
            confidence_score += 25
        if latest_abg:
            confidence_score += 25

        # Determine acidosis and hypercapnia flags
        acidosis = 1 if ph < 7.35 else 0
        hypercapnia = 1 if paco2 > 45 else 0

        # ── 4. Store AI result in ai_analysis table ──
        from therapy.models import AIAnalysis
        AIAnalysis.objects.create(
            patient_id=patient.id,
            risk_level=risk_level,
            confidence_score=confidence_score,
            acidosis=acidosis,
            hypercapnia=hypercapnia
        )

        # ── 5. Trend Analysis Calculation ──
        # Compare latest 2 ABG entries and latest 2 vitals entries for real trends
        abg_entries = list(AbgEntry.objects.filter(patient_id=patient.id).order_by('-created_at')[:2])
        vitals_entries = list(Vitals.objects.filter(patient_id=patient.id).order_by('-created_at')[:2])

        # Also check staff reassessments for SpO2 trend
        try:
            from staff.models import StaffChecklist
            staff_entries = list(StaffChecklist.objects.filter(patient_id=patient.id).order_by('-created_at')[:2])
        except ImportError:
            staff_entries = []

        # PaCO2 trend: compare latest 2 ABG entries
        if len(abg_entries) >= 2:
            curr_paco2 = abg_entries[0].paco2 if abg_entries[0].paco2 is not None else 40
            prev_paco2 = abg_entries[1].paco2 if abg_entries[1].paco2 is not None else 40
            if curr_paco2 > prev_paco2 + 2:
                paco2_status = "Rising"
            elif curr_paco2 < prev_paco2 - 2:
                paco2_status = "Decreasing"
            else:
                paco2_status = "Normal"
        else:
            # Only 1 entry — judge by absolute value
            if paco2 > 45:
                paco2_status = "Rising"
            else:
                paco2_status = "Normal"

        # pH trend: compare latest 2 ABG entries
        if len(abg_entries) >= 2:
            curr_ph = abg_entries[0].ph if abg_entries[0].ph is not None else 7.40
            prev_ph = abg_entries[1].ph if abg_entries[1].ph is not None else 7.40
            if curr_ph < prev_ph - 0.02:
                ph_status = "Dropping"
            elif curr_ph > prev_ph + 0.02:
                ph_status = "Improving"
            else:
                ph_status = "Normal"
        else:
            if ph < 7.35:
                ph_status = "Dropping"
            else:
                ph_status = "Normal"

        # SpO2 trend: compare latest 2 vitals or staff entries
        spo2_readings = []
        # Collect SpO2 readings from both vitals and staff, sorted by time
        for v in vitals_entries:
            if v.spo2 is not None:
                spo2_readings.append((v.created_at, v.spo2))
        for s in staff_entries:
            if s.spo2 is not None:
                spo2_readings.append((s.created_at, s.spo2))
        spo2_readings.sort(key=lambda x: x[0], reverse=True)

        if len(spo2_readings) >= 2:
            curr_spo2 = spo2_readings[0][1]
            prev_spo2 = spo2_readings[1][1]
            diff = curr_spo2 - prev_spo2
            if abs(diff) > 3:
                spo2_status = "Unstable"
            elif 88 <= curr_spo2 <= 92:
                spo2_status = "Stable"
            elif curr_spo2 < 88:
                spo2_status = "Unstable"
            else:
                spo2_status = "Stable"
        else:
            if 88 <= spo2 <= 92:
                spo2_status = "Stable"
            elif spo2 < 88:
                spo2_status = "Unstable"
            else:
                spo2_status = "Stable"

        # Overall status
        if paco2_status == "Rising" or ph_status == "Dropping" or spo2_status == "Unstable":
            if risk_level == "HIGH":
                overall_status = "Worsening"
            else:
                overall_status = "Worsening"
        elif paco2_status == "Decreasing" or ph_status == "Improving":
            overall_status = "Improving"
        else:
            overall_status = "Stable"

        # ── 6. Store Trend result in trend_analysis table ──
        TrendAnalysis.objects.create(
            patient_id=patient.id,
            overall_status=overall_status,
            paco2_status=paco2_status,
            ph_status=ph_status,
            spo2_status=spo2_status
        )

        return Response({
            "risk_level": risk_level,
            "confidence_score": confidence_score,
            "acidosis": acidosis,
            "hypercapnia": hypercapnia,
            "key_factors": key_factors
        }, status=status.HTTP_200_OK)


class CustomTrendAnalysisAPIView(APIView):
    """
    GET /api/patient/trend-analysis/<patient_id>/
    Dynamically computes trend analysis by comparing the latest two
    reassessment values (ABG entries and staff checklist SpO2).
    """
    def get(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        from patients.models import AbgEntry, Vitals
        from staff.models import StaffChecklist, Reassessment

        # Fetch the latest 2 ABG entries (from reassessments)
        abg_entries = list(
            AbgEntry.objects.filter(patient_id=patient_id)
            .order_by('-created_at')[:2]
        )

        # Collect SpO2 readings from ALL sources, then pick the latest 2
        spo2_readings = []  # list of (timestamp, spo2_value)

        # Source 1: StaffChecklist (reassessment_checklist table)
        staff_entries = StaffChecklist.objects.filter(
            patient_id=patient_id, spo2__isnull=False
        ).exclude(spo2=0).order_by('-created_at')[:5]
        for sc in staff_entries:
            spo2_readings.append((sc.created_at, sc.spo2))

        # Source 2: Reassessment table
        reassessment_entries = Reassessment.objects.filter(
            patient_id=patient_id, status='completed', spo2__isnull=False
        ).order_by('-reassessment_time')[:5]
        for r in reassessment_entries:
            ts = r.reassessment_time or r.created_at
            spo2_readings.append((ts, r.spo2))

        # Source 3: Vitals table (fallback)
        vitals_entries = list(
            Vitals.objects.filter(patient_id=patient_id, spo2__isnull=False)
            .order_by('-created_at')[:5]
        )
        for v in vitals_entries:
            spo2_readings.append((v.created_at, v.spo2))

        # Sort by timestamp descending and deduplicate
        spo2_readings.sort(key=lambda x: x[0], reverse=True)

        # --- Compute PaCO2 trend ---
        paco2_status = "Insufficient Data"
        if len(abg_entries) >= 2:
            current_paco2 = abg_entries[0].paco2  # latest
            previous_paco2 = abg_entries[1].paco2  # previous
            diff = current_paco2 - previous_paco2
            if diff > 2:
                paco2_status = "Rising"
            elif diff < -2:
                paco2_status = "Decreasing"
            else:
                paco2_status = "Stable"
        elif len(abg_entries) == 1:
            # Only one ABG entry — cannot compare, just report current value status
            paco2_status = "Insufficient Data"

        # --- Compute pH trend ---
        ph_status = "Insufficient Data"
        if len(abg_entries) >= 2:
            current_ph = abg_entries[0].ph
            previous_ph = abg_entries[1].ph
            diff = current_ph - previous_ph
            if diff < -0.02:
                ph_status = "Dropping"
            elif diff > 0.02:
                ph_status = "Rising"
            else:
                ph_status = "Stable"
        elif len(abg_entries) == 1:
            ph_status = "Insufficient Data"

        # --- Compute SpO2 trend ---
        spo2_status = "Insufficient Data"
        # Use aggregated spo2_readings from all sources (already sorted by time desc)
        if len(spo2_readings) >= 2:
            current_spo2 = spo2_readings[0][1]  # latest
            previous_spo2 = spo2_readings[1][1]  # previous
            diff = current_spo2 - previous_spo2
            if diff > 2:
                spo2_status = "Improving"
            elif diff < -2:
                spo2_status = "Declining"
            else:
                spo2_status = "Stable"

        # --- Compute overall status ---
        all_statuses = [paco2_status, ph_status, spo2_status]
        has_data = any(s != "Insufficient Data" for s in all_statuses)

        if not has_data:
            overall_status = "Insufficient Data"
        else:
            # Count concerning indicators
            worsening_indicators = 0
            improving_indicators = 0
            for s in all_statuses:
                if s in ("Rising", "Dropping", "Declining", "Unstable"):
                    worsening_indicators += 1
                elif s in ("Improving", "Decreasing"):
                    improving_indicators += 1

            # For PaCO2: "Rising" is worsening, "Decreasing" is improving
            # For pH: "Dropping" is worsening, "Rising" is improving
            # For SpO2: "Declining" is worsening, "Improving" is improving
            if worsening_indicators >= 2:
                overall_status = "Worsening"
            elif worsening_indicators == 1 and improving_indicators == 0:
                overall_status = "Worsening"
            elif improving_indicators >= 2:
                overall_status = "Improving"
            elif improving_indicators == 1 and worsening_indicators == 0:
                overall_status = "Improving"
            else:
                overall_status = "Stable"

        return Response({
            "overall_status": overall_status,
            "paco2_status": paco2_status,
            "ph_status": ph_status,
            "spo2_status": spo2_status
        }, status=status.HTTP_200_OK)


class DecisionSupportAPIView(APIView):
    """
    GET /api/patient/decision-support/<patient_id>/
    Fetches latest AI analysis + Trend analysis for the patient
    and returns combined decision support data.
    """
    def get(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        from therapy.models import AIAnalysis

        latest_ai = AIAnalysis.objects.filter(patient_id=patient.id).order_by('-recorded_at').first()
        latest_trend = TrendAnalysis.objects.filter(patient_id=patient.id).order_by('-recorded_at').first()

        if not latest_ai and not latest_trend:
            return Response({
                "has_data": False,
                "message": "No analysis data available for this patient"
            }, status=status.HTTP_200_OK)

        # AI Analysis data
        risk_level = latest_ai.risk_level if latest_ai else "LOW"
        confidence_score = latest_ai.confidence_score if latest_ai else 0
        acidosis = latest_ai.acidosis if latest_ai else 0
        hypercapnia = latest_ai.hypercapnia if latest_ai else 0

        # Trend data
        overall_status = latest_trend.overall_status if latest_trend else "Stable"
        paco2_status = latest_trend.paco2_status if latest_trend else "Normal"
        ph_status = latest_trend.ph_status if latest_trend else "Normal"
        spo2_status = latest_trend.spo2_status if latest_trend else "Stable"

        # Decision based on risk_level
        if risk_level == "HIGH":
            recommendation = "Critical: Immediate intervention required. Consider escalating to ICU or initiating NIV therapy."
            action_level = "CRITICAL"
        elif risk_level == "MODERATE":
            recommendation = "Warning: Close monitoring required. Adjust oxygen therapy and reassess within 1 hour."
            action_level = "WARNING"
        else:
            recommendation = "Normal: Continue current monitoring protocol. Reassess at next scheduled interval."
            action_level = "NORMAL"

        return Response({
            "has_data": True,
            "risk_level": risk_level,
            "confidence_score": confidence_score,
            "acidosis": acidosis,
            "hypercapnia": hypercapnia,
            "overall_status": overall_status,
            "paco2_status": paco2_status,
            "ph_status": ph_status,
            "spo2_status": spo2_status,
            "recommendation": recommendation,
            "action_level": action_level
        }, status=status.HTTP_200_OK)

class ClinicalReviewAPIView(APIView):
    """
    GET  /api/patient/clinical-review/<patient_id>/
         Fetches latest vitals + ABG, computes recommended device.
    POST /api/patient/clinical-review/<patient_id>/
         Saves accept/override decision into review_recommendation table.
         Body: { device, fio2, flow_rate, decision: "accepted"|"override", override_reason }
    """
    def get(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        latest_vitals = Vitals.objects.filter(patient_id=patient.id).order_by('-created_at').first()
        latest_abg = AbgEntry.objects.filter(patient_id=patient.id).order_by('-created_at').first()

        if not latest_vitals and not latest_abg:
            return Response({
                "has_data": False,
                "message": "No clinical data available"
            }, status=status.HTTP_200_OK)

        spo2 = latest_vitals.spo2 if latest_vitals and latest_vitals.spo2 is not None else 98

        from therapy.views import AIDeviceRecommendationAPIView
        
        device = "Venturi Mask"
        flow_rate = "4-8 L/min"
        fio2 = "24-60%"
        
        try:
            # Instantiate and manually call the AI recommendation view to get accurate ML predictions
            view = AIDeviceRecommendationAPIView()
            ml_resp = view.get(request, patient_id)
            if ml_resp.status_code == 200:
                device = ml_resp.data.get('recommended_device', 'Venturi Mask')
                flow_rate = ml_resp.data.get('flow_range', '4-8 L/min')
                if device == "Non-Rebreather Mask":
                    fio2 = "60-90%"
                elif device == "Venturi Mask":
                    fio2 = "24-60%"
                elif device == "High Flow Nasal Cannula":
                    fio2 = "40-100%"
                else: # Nasal Cannula
                    fio2 = "24-40%"
            else:
                raise Exception("ML API fallback")
        except Exception as e:
            # Fallback in case ML model fails
            if spo2 < 85:
                device = "Non-Rebreather Mask"
                fio2 = "60-90%"
                flow_rate = "10-15 L/min"
            elif spo2 <= 92:
                device = "Venturi Mask"
                fio2 = "28-35%"
                flow_rate = "4-8 L/min"
            else:
                device = "Nasal Cannula"
                fio2 = "24-28%"
                flow_rate = "1-4 L/min"

        return Response({
            "has_data": True,
            "recommended_device": device,
            "fio2": fio2,
            "flow_rate": flow_rate,
            "spo2": spo2
        }, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        data = request.data
        device = data.get('device', '')
        fio2 = data.get('fio2', '')
        flow_rate = data.get('flow_rate', '')
        decision = data.get('decision', 'accepted')
        override_reason = data.get('override_reason', '')

        from therapy.models import ReviewRecommendation
        ReviewRecommendation.objects.create(
            patient_id=patient.id,
            device=device,
            fio2=fio2,
            flow_rate=flow_rate,
            decision=decision,
            override_reason=override_reason
        )

        return Response({"message": "Review recommendation saved successfully."}, status=status.HTTP_201_CREATED)


class ClinicalTherapyPlanAPIView(APIView):
    """
    GET /api/patient/clinical-therapy/<patient_id>/
    Fetches latest review_recommendation + AI analysis, computes therapy plan,
    stores in therapy_recommendation table, returns the plan.
    """
    def get(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        from therapy.models import ReviewRecommendation as ReviewRec, AIAnalysis, TherapyRecommendation

        latest_rec = ReviewRec.objects.filter(patient_id=patient.id).order_by('-created_at').first()
        latest_ai = AIAnalysis.objects.filter(patient_id=patient.id).order_by('-recorded_at').first()

        if not latest_rec:
            return Response({
                "has_data": False,
                "message": "No recommendation data available"
            }, status=status.HTTP_200_OK)

        device = latest_rec.device
        fio2 = latest_rec.fio2
        flow_rate = latest_rec.flow_rate
        risk_level = latest_ai.risk_level if latest_ai else "LOW"

        # Target SpO2 logic
        if device == "Non-Rebreather Mask":
            target_spo2 = "92-96%"
        elif device == "Venturi Mask":
            target_spo2 = "88-92%"
        else:
            target_spo2 = "94-98%"

        # Next ABG logic
        if risk_level in ("HIGH", "MODERATE"):
            next_abg = "30 mins"
        else:
            next_abg = "1 hour"

        # Rationale
        rationale = "Patient shows " + risk_level + " risk. Maintain oxygen therapy via " + device + " and monitor closely."

        # Store therapy plan in therapy_recommendation table
        TherapyRecommendation.objects.create(
            patient_id=patient.id,
            device=device,
            fio2=fio2,
            flow_rate=flow_rate,
            target_spo2=target_spo2,
            next_abg=next_abg,
            rationale=rationale
        )

        return Response({
            "has_data": True,
            "device": device,
            "fio2": fio2,
            "flow_rate": flow_rate,
            "target_spo2": target_spo2,
            "next_abg_time": next_abg,
            "rationale": rationale,
            "risk_level": risk_level
        }, status=status.HTTP_200_OK)


class ClinicalReassessmentAPIView(APIView):
    """
    POST /api/patient/clinical-reassessment/<patient_id>/
    Body: { reassessment_time_minutes: 30|60|120|240, reassessment_type: "SpO2"|"ABG" }
    Stores in schedule_reassessment table with scheduled_time = NOW() + INTERVAL.
    """
    def post(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        from therapy.models import ScheduleReassessment
        from django.utils import timezone
        import datetime

        minutes = request.data.get('reassessment_time_minutes', 60)
        try:
            minutes = int(minutes)
        except (ValueError, TypeError):
            minutes = 60

        reassessment_type = request.data.get('reassessment_type', 'SpO2')
        scheduled_time = timezone.now() + datetime.timedelta(minutes=minutes)

        ScheduleReassessment.objects.create(
            patient_id=patient.id,
            patient_name=patient.full_name,
            bed_no=patient.bed_number or '',
            ward_no=patient.ward or '',
            reassessment_type=reassessment_type,
            reassessment_minutes=minutes,
            scheduled_time=scheduled_time,
            status='pending',
            scheduled_by='doctor',
        )

        return Response({
            "message": "Reassessment scheduled successfully.",
            "scheduled_time": scheduled_time.isoformat(),
            "reassessment_time_minutes": minutes
        }, status=status.HTTP_201_CREATED)


# ──────────────────────────────────────────────────────────────────────────────
# Baseline Details
# ──────────────────────────────────────────────────────────────────────────────

class AddBaselineDetailsAPIView(APIView):
    """
    POST /api/baseline-details/add/
    Body: { patient_id, copd_history }
    """
    def post(self, request):
        patient_id = request.data.get("patient_id")
        copd_history = request.data.get("copd_history")

        # Validate required fields
        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)
        if not copd_history:
            return Response({"error": "copd_history is required."}, status=status.HTTP_400_BAD_REQUEST)

        BaselineDetails.objects.create(
            patient_id=patient_id,
            copd_history=copd_history
        )

        return Response({"message": "Baseline details saved successfully"}, status=status.HTTP_201_CREATED)



# ──────────────────────────────────────────────────────────────────────────────
# GOLD Classification
# ──────────────────────────────────────────────────────────────────────────────

class AddGoldClassificationAPIView(APIView):
    """
    POST /api/gold-classification/add/
    Body: { patient_id, gold_stage }
    """
    def post(self, request):
        patient_id = request.data.get("patient_id")
        gold_stage = request.data.get("gold_stage")

        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)
        if not gold_stage:
            return Response({"error": "gold_stage is required."}, status=status.HTTP_400_BAD_REQUEST)

        GoldClassification.objects.create(
            patient_id=patient_id,
            gold_stage=gold_stage
        )

        return Response({"message": "GOLD classification saved successfully"}, status=status.HTTP_201_CREATED)


# ──────────────────────────────────────────────────────────────────────────────
# Spirometry Data
# ──────────────────────────────────────────────────────────────────────────────

class AddSpirometryAPIView(APIView):
    """
    POST /api/spirometry/add/
    Body: { patient_id, fev1_percent, fev1_fvc_ratio }
    """
    def post(self, request):
        patient_id = request.data.get("patient_id")
        fev1_percent = request.data.get("fev1_percent")
        fev1_fvc_ratio = request.data.get("fev1_fvc_ratio")

        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)
        
        try:
            fev1_percent = float(fev1_percent) if fev1_percent is not None else None
            fev1_fvc_ratio = float(fev1_fvc_ratio) if fev1_fvc_ratio is not None else None
        except ValueError:
            return Response({"error": "fev1_percent and fev1_fvc_ratio must be numbers."}, status=status.HTTP_400_BAD_REQUEST)

        if fev1_percent is None or fev1_fvc_ratio is None:
            return Response({"error": "fev1_percent and fev1_fvc_ratio are required."}, status=status.HTTP_400_BAD_REQUEST)

        SpirometryData.objects.create(
            patient_id=patient_id,
            fev1_percent=fev1_percent,
            fev1_fvc_ratio=fev1_fvc_ratio
        )

        return Response({"message": "Spirometry data saved successfully"}, status=status.HTTP_201_CREATED)


# ──────────────────────────────────────────────────────────────────────────────
# Gas Exchange History
# ──────────────────────────────────────────────────────────────────────────────

class AddGasExchangeHistoryAPIView(APIView):
    """
    POST /api/gas-exchange-history/add/
    Body: { patient_id, chronic_hypoxemia, home_oxygen_use }
    """
    def post(self, request):
        patient_id = request.data.get("patient_id")
        chronic_hypoxemia = request.data.get("chronic_hypoxemia")
        home_oxygen_use = request.data.get("home_oxygen_use")

        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)
        
        GasExchangeHistory.objects.create(
            patient_id=patient_id,
            chronic_hypoxemia=chronic_hypoxemia,
            home_oxygen_use=home_oxygen_use
        )

        return Response({"message": "Gas exchange history saved successfully"}, status=status.HTTP_201_CREATED)


# ──────────────────────────────────────────────────────────────────────────────
# Current Symptoms
# ──────────────────────────────────────────────────────────────────────────────

class AddCurrentSymptomsAPIView(APIView):
    """
    POST /api/current-symptoms/add/
    Body: { patient_id, mmrc_score, increased_cough, increased_sputum, wheezing, fever, chest_tightness }
    """
    def post(self, request):
        patient_id = request.data.get("patient_id")
        mmrc_score = request.data.get("mmrc_score")

        increased_cough = request.data.get("increased_cough", False)
        increased_sputum = request.data.get("increased_sputum", False)
        wheezing = request.data.get("wheezing", False)
        fever = request.data.get("fever", False)
        chest_tightness = request.data.get("chest_tightness", False)

        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)
        if mmrc_score is None:
            return Response({"error": "mmrc_score is required."}, status=status.HTTP_400_BAD_REQUEST)

        CurrentSymptoms.objects.create(
            patient_id=patient_id,
            mmrc_score=mmrc_score,
            increased_cough=increased_cough,
            increased_sputum=increased_sputum,
            wheezing=wheezing,
            fever=fever,
            chest_tightness=chest_tightness
        )

        return Response({"message": "Current symptoms saved successfully"}, status=status.HTTP_201_CREATED)


# ──────────────────────────────────────────────────────────────────────────────
# Vitals
# ──────────────────────────────────────────────────────────────────────────────

class AddVitalsAPIView(APIView):
    """
    POST /api/vitals/add/
    Body: { patient_id, spo2, respiratory_rate, heart_rate, temperature, blood_pressure }
    """
    def post(self, request):
        patient_id = request.data.get("patient_id")
        spo2 = request.data.get("spo2")
        respiratory_rate = request.data.get("respiratory_rate")
        heart_rate = request.data.get("heart_rate")
        temperature = request.data.get("temperature")
        blood_pressure = request.data.get("blood_pressure")

        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)

        Vitals.objects.create(
            patient_id=patient_id,
            spo2=spo2,
            respiratory_rate=respiratory_rate,
            heart_rate=heart_rate,
            temperature=temperature,
            blood_pressure=blood_pressure
        )

        # Check for SpO2 drop and create doctor alert if needed
        if spo2 is not None:
            try:
                from alerts.views import check_spo2_drop_and_alert
                check_spo2_drop_and_alert(patient_id, spo2)
            except Exception as e:
                print(f"[AddVitals] Alert check error: {e}")

        return Response({"message": "Vitals saved successfully"}, status=status.HTTP_201_CREATED)


# ──────────────────────────────────────────────────────────────────────────────
# ABG Entry
# ──────────────────────────────────────────────────────────────────────────────

class AddAbgEntryAPIView(APIView):
    """
    POST /api/abg-entry/add/
    Body: { patient_id, ph, pao2, paco2, hco3, fio2 }
    """
    def post(self, request):
        patient_id = request.data.get("patient_id")
        ph = request.data.get("ph")
        pao2 = request.data.get("pao2")
        paco2 = request.data.get("paco2")
        hco3 = request.data.get("hco3")
        fio2 = request.data.get("fio2")

        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)

        AbgEntry.objects.create(
            patient_id=patient_id,
            ph=ph,
            pao2=pao2,
            paco2=paco2,
            hco3=hco3,
            fio2=fio2
        )

        return Response({"message": "ABG data saved successfully"}, status=status.HTTP_201_CREATED)


# ──────────────────────────────────────────────────────────────────────────────
# Reassessment Checklist
# ──────────────────────────────────────────────────────────────────────────────

class ReassessmentChecklistAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/reassessment-checklist/
    Returns all staff-entered reassessment checklists for the patient.
    Now reads from staff.models.StaffChecklist (reassessment_checklist table).
    """
    def get(self, request, patient_id):
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        from staff.models import StaffChecklist
        records = StaffChecklist.objects.filter(patient_id=patient_id).order_by('-created_at')
        data = []
        for r in records:
            data.append({
                "id": r.id,
                "patient_id": r.patient_id,
                "reassessment_id": r.reassessment_id,
                "spo2": r.spo2,
                "respiratory_rate": r.respiratory_rate,
                "heart_rate": r.heart_rate,
                "abg_values": r.abg_values,
                "remarks": r.remarks,
                "entered_by": r.entered_by,
                "created_at": r.created_at.strftime("%Y-%m-%d %H:%M:%S") if r.created_at else None,
            })
        return Response({"patient_id": patient_id, "reassessments": data}, status=status.HTTP_200_OK)
