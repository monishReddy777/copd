from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone

from .models import (
    OxygenStatus, AIAnalysis, ABGTrend, TrendAnalysis, HypoxemiaCause,
    OxygenRequirement, DeviceSelection, ReviewRecommendation,
    TherapyRecommendation, NIVRecommendation, EscalationCriteria,
    ScheduleReassessment, UrgentAction
)


def get_patient_or_404(patient_id):
    from patients.models import Patient
    try:
        return Patient.objects.get(id=patient_id), None
    except Patient.DoesNotExist:
        return None, Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)


class OxygenStatusAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/oxygen-status/
    POST /api/patients/<patient_id>/oxygen-status/
    Body: { current_flow_rate, delivery_device, target_spo2_min, target_spo2_max }
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        records = OxygenStatus.objects.filter(patient_id=patient_id).order_by('-created_at').values()
        return Response({"patient_id": patient_id, "oxygen_status": list(records)}, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        flow_rate = request.data.get('current_flow_rate')
        device = request.data.get('delivery_device')
        if not flow_rate or not device:
            return Response({"error": "current_flow_rate and delivery_device are required."}, status=status.HTTP_400_BAD_REQUEST)
        record = OxygenStatus.objects.create(
            patient_id=patient_id,
            current_flow_rate=float(flow_rate),
            delivery_device=device,
            target_spo2_min=request.data.get('target_spo2_min', 88.0),
            target_spo2_max=request.data.get('target_spo2_max', 92.0),
        )
        return Response({
            "message": "Oxygen status saved.",
            "patient_id": patient_id,
            "current_flow_rate": record.current_flow_rate,
            "delivery_device": record.delivery_device,
            "target_spo2_min": record.target_spo2_min,
            "target_spo2_max": record.target_spo2_max,
            "recorded_at": record.created_at,
        }, status=status.HTTP_201_CREATED)


class AIAnalysisAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/ai-analysis/
    POST /api/patients/<patient_id>/ai-analysis/
    GET returns the latest AI analysis, which is auto-computed from patient vitals and ABG.
    POST saves a manual/computed AI analysis result.
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        from patients.models import Vitals, AbgEntry
        latest_vitals = Vitals.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        latest_abg = AbgEntry.objects.filter(patient_id=patient_id).order_by('-created_at').first()

        # Auto-compute risk based on clinical thresholds
        # Start from 0, build up based on actual clinical findings
        risk_score = 0.0
        risk_level = 'low'
        key_factors = []
        recommendations = []

        if latest_vitals:
            # For COPD: target SpO2 is 88-92%
            if latest_vitals.spo2 < 85:
                risk_score = min(risk_score + 35, 100)
                key_factors.append("Severe Hypoxaemia: SpO2 < 85%")
                recommendations.append("Immediate oxygen therapy adjustment required.")
            elif latest_vitals.spo2 < 88:
                risk_score = min(risk_score + 25, 100)
                key_factors.append("Hypoxaemia: SpO2 < 88% (below COPD target)")
                recommendations.append("Increase oxygen flow rate to target 88-92%.")
            elif latest_vitals.spo2 > 92:
                risk_score = min(risk_score + 10, 100)
                key_factors.append("SpO2 above target (>92%) — CO2 retention risk")
                recommendations.append("Consider reducing oxygen flow to maintain 88-92%.")
            # SpO2 88-92 = on target, no risk added

            if latest_vitals.respiratory_rate > 30:
                risk_score = min(risk_score + 20, 100)
                key_factors.append("Tachypnoea: RR > 30 breaths/min")
                recommendations.append("Consider NIV support.")
            elif latest_vitals.respiratory_rate > 25:
                risk_score = min(risk_score + 10, 100)
                key_factors.append("Elevated respiratory rate (25–30 breaths/min)")

        if latest_abg:
            if latest_abg.ph < 7.25:
                risk_score = min(risk_score + 30, 100)
                key_factors.append("Severe acidosis: pH < 7.25")
                recommendations.append("Urgent medical review and NIV consideration.")
            elif latest_abg.ph < 7.35:
                risk_score = min(risk_score + 15, 100)
                key_factors.append("Acidosis: pH < 7.35")
            if latest_abg.paco2 > 60:
                risk_score = min(risk_score + 25, 100)
                key_factors.append("Severe Hypercapnia: PaCO2 > 60 mmHg")
                recommendations.append("Monitor for CO2 narcosis.")
            elif latest_abg.paco2 > 45:
                risk_score = min(risk_score + 15, 100)
                key_factors.append("Hypercapnia: PaCO2 > 45 mmHg")

        if risk_score >= 60:
            risk_level = 'critical'
        elif risk_score >= 30:
            risk_level = 'high'
        elif risk_score >= 15:
            risk_level = 'moderate'
        else:
            risk_level = 'low'

        deterioration_probability = round(min(risk_score / 100, 1.0), 2)
        if not recommendations:
            recommendations.append("Continue current oxygen therapy and monitor closely.")

        return Response({
            "patient_id": patient_id,
            "risk_score": round(risk_score, 1),
            "risk_level": risk_level,
            "deterioration_probability": deterioration_probability,
            "key_factors": key_factors,
            "recommendations": recommendations,
            "based_on": {
                "vitals_recorded_at": latest_vitals.created_at if latest_vitals else None,
                "abg_recorded_at": latest_abg.created_at if latest_abg else None,
            }
        }, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        record = AIAnalysis.objects.create(
            patient_id=patient_id,
            risk_score=request.data.get('risk_score', 50.0),
            risk_level=request.data.get('risk_level', 'moderate'),
            deterioration_probability=request.data.get('deterioration_probability', 0.5),
            key_factors=str(request.data.get('key_factors', [])),
            recommendations=str(request.data.get('recommendations', [])),
        )
        return Response({"message": "AI analysis saved.", "id": record.id}, status=status.HTTP_201_CREATED)


class ABGTrendsAPIView(APIView):
    """
    GET /api/patients/<patient_id>/abg-trends/
    Returns all ABG entries in chronological order for trend display,
    combining data from BOTH:
      1. abg_entry table (direct ABG entries)
      2. reassessment_checklist table (staff reassessment ABG values)
    Plus patient info, time labels, and trend analysis summary.
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        from patients.models import AbgEntry, Vitals
        from django.utils import timezone
        import datetime
        import json

        # Get filter parameter: '5h', '8h', '24h', '1d', '2d', '3d', '7d', '15d', '1m', or None (all)
        time_filter = request.query_params.get('filter', None)

        # Hour-based filters use exact hours from now
        filter_exact_hours = {
            '5h': 5, '8h': 8, '24h': 24,
        }
        # Day-based filters use calendar days (go back to midnight of N days ago)
        filter_calendar_days = {
            '1d': 1, '2d': 2, '3d': 3, '7d': 7, '15d': 15, '1m': 30,
        }

        cutoff = None
        if time_filter in filter_exact_hours:
            hours = filter_exact_hours[time_filter]
            cutoff = timezone.now() - datetime.timedelta(hours=hours)
        elif time_filter in filter_calendar_days:
            days = filter_calendar_days[time_filter]
            # Go back to midnight (start of day) N days ago
            # e.g., "3D" on Mar 22 → cutoff = Mar 19 00:00:00  (includes all of Mar 19, 20, 21, 22)
            today_start = timezone.now().replace(hour=0, minute=0, second=0, microsecond=0)
            cutoff = today_start - datetime.timedelta(days=days)

        # ── 1. Fetch from abg_entry table ──
        abg_qs = AbgEntry.objects.filter(patient_id=patient_id)
        if cutoff:
            abg_qs = abg_qs.filter(created_at__gte=cutoff)
        abg_records = list(abg_qs.order_by('created_at').values(
            'id', 'ph', 'pao2', 'paco2', 'hco3', 'fio2', 'created_at'
        ))

        # ── 2. Fetch from reassessment_checklist table (StaffChecklist) ──
        # Staff reassessments store ABG values as JSON in the abg_values field
        try:
            from staff.models import StaffChecklist
            checklist_qs = StaffChecklist.objects.filter(
                patient_id=patient_id
            ).exclude(abg_values__isnull=True).exclude(abg_values='')
            if cutoff:
                checklist_qs = checklist_qs.filter(created_at__gte=cutoff)
            checklist_records = list(checklist_qs.order_by('created_at'))
        except Exception:
            checklist_records = []

        # Parse checklist ABG values and convert to same dict format
        checklist_entries = []
        for sc in checklist_records:
            abg_str = sc.abg_values
            if not abg_str or not abg_str.strip():
                continue
            try:
                abg_data = json.loads(abg_str)
            except (json.JSONDecodeError, TypeError):
                # Try parsing as "pH:7.35,PaCO2:45,..." format
                abg_data = {}
                for pair in abg_str.replace(';', ',').split(','):
                    pair = pair.strip()
                    if ':' in pair:
                        k, v = pair.split(':', 1)
                        try:
                            abg_data[k.strip().lower()] = float(v.strip())
                        except ValueError:
                            pass

            if not abg_data:
                continue

            # Normalize keys (handle variations like 'Ph', 'PH', 'pH', etc.)
            ph_val = abg_data.get('ph') or abg_data.get('Ph') or abg_data.get('PH')
            pao2_val = abg_data.get('pao2') or abg_data.get('PaO2') or abg_data.get('paO2')
            paco2_val = abg_data.get('paco2') or abg_data.get('PaCO2') or abg_data.get('paCO2')
            hco3_val = abg_data.get('hco3') or abg_data.get('HCO3') or abg_data.get('Hco3')
            fio2_val = abg_data.get('fio2') or abg_data.get('FiO2') or abg_data.get('fiO2')

            # Only include if at least pH or PaCO2 is present
            if ph_val is not None or paco2_val is not None:
                checklist_entries.append({
                    'id': -sc.id,  # Negative ID to differentiate from AbgEntry
                    'ph': float(ph_val) if ph_val is not None else 0,
                    'pao2': float(pao2_val) if pao2_val is not None else 0,
                    'paco2': float(paco2_val) if paco2_val is not None else 0,
                    'hco3': float(hco3_val) if hco3_val is not None else 0,
                    'fio2': float(fio2_val) if fio2_val is not None else 21,
                    'created_at': sc.created_at,
                })

        # ── 3. Merge and sort all entries chronologically ──
        # Collect timestamps from abg_records to avoid duplicates
        abg_timestamps = set()
        for rec in abg_records:
            if rec.get('created_at'):
                # Round to the nearest minute for dedup
                abg_timestamps.add(rec['created_at'].strftime("%Y-%m-%d %H:%M"))

        # Only add checklist entries that aren't duplicates of abg_entry records
        for ce in checklist_entries:
            ts_key = ce['created_at'].strftime("%Y-%m-%d %H:%M") if ce.get('created_at') else ''
            if ts_key not in abg_timestamps:
                abg_records.append(ce)

        # Sort combined list by created_at
        abg_records.sort(key=lambda x: x.get('created_at') or timezone.now())

        # ── 4. Format timestamps and add time labels ──
        # Convert UTC timestamps to local time (Asia/Kolkata IST) before display
        from zoneinfo import ZoneInfo
        local_tz = ZoneInfo('Asia/Kolkata')

        is_multi_day = time_filter in ('2d', '3d', '7d', '15d', '1m')
        entries = []
        for e in abg_records:
            entry = dict(e)
            if entry.get('created_at'):
                ts = entry['created_at']
                # Convert to IST for display
                try:
                    if hasattr(ts, 'tzinfo') and ts.tzinfo is not None:
                        local_ts = ts.astimezone(local_tz)
                    else:
                        # Naive datetime — assume UTC and convert
                        import datetime as dt_mod
                        local_ts = ts.replace(tzinfo=dt_mod.timezone.utc).astimezone(local_tz)
                except Exception:
                    local_ts = ts  # Fallback: use as-is

                entry['date_label'] = local_ts.strftime("%d/%m %H:%M")
                if is_multi_day:
                    entry['time_label'] = local_ts.strftime("%d/%m")
                else:
                    entry['time_label'] = local_ts.strftime("%H:%M")
                entry['created_at'] = local_ts.strftime("%Y-%m-%d %H:%M:%S")
            else:
                entry['time_label'] = ''
                entry['date_label'] = ''
            entries.append(entry)

        # Determine patient status from latest SpO2 (COPD target: 88-92%)
        status_label = "Stable"
        latest_vitals = Vitals.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        spo2_val = None
        if latest_vitals and latest_vitals.spo2 is not None:
            spo2_val = latest_vitals.spo2
            if spo2_val < 88:
                status_label = "Critical"
            elif spo2_val > 92:
                status_label = "Warning"

        # Generate trend analysis text from latest ABG values
        if time_filter:
            trend_text = f"No ABG data available for the last {time_filter}."
        else:
            trend_text = "No ABG data available for trend analysis."
        if entries:
            latest = entries[-1]
            ph = latest.get('ph', 0)
            paco2 = latest.get('paco2', 0)
            pao2 = latest.get('pao2', 0)
            fio2 = latest.get('fio2', 0)

            issues = []
            if ph < 7.35:
                issues.append("acidosis (pH {:.2f})".format(ph))
            elif ph > 7.45:
                issues.append("alkalosis (pH {:.2f})".format(ph))

            if paco2 > 45:
                issues.append("elevated PaCO2 ({:.0f} mmHg) indicating CO2 retention".format(paco2))
            elif paco2 < 35:
                issues.append("low PaCO2 ({:.0f} mmHg) suggesting hyperventilation".format(paco2))

            if pao2 < 60:
                issues.append("hypoxemia (PaO2 {:.0f} mmHg)".format(pao2))

            if issues:
                trend_text = "Patient is showing " + ", ".join(issues) + "."
                if paco2 > 45 and ph < 7.35:
                    trend_text += " This suggests Type II respiratory failure progression."
                elif pao2 < 60 and ph < 7.35:
                    trend_text += " Consider escalating oxygen therapy."
            else:
                trend_text = "ABG values are within normal ranges. Continue current management."

            # Add trend direction info if multiple entries
            if len(entries) >= 2:
                prev = entries[-2]
                directions = []
                if ph < prev.get('ph', ph):
                    directions.append("pH declining")
                elif ph > prev.get('ph', ph):
                    directions.append("pH improving")

                curr_paco2 = latest.get('paco2', 0)
                prev_paco2 = prev.get('paco2', curr_paco2)
                if curr_paco2 > prev_paco2:
                    directions.append("PaCO2 rising")
                elif curr_paco2 < prev_paco2:
                    directions.append("PaCO2 decreasing")

                if directions:
                    trend_text += " Trend: " + ", ".join(directions) + "."

        return Response({
            "patient_id": patient_id,
            "patient_name": patient.full_name,
            "diagnosis": "COPD Exacerbation",
            "status": status_label,
            "abg_trend_data": entries,
            "total_entries": len(entries),
            "trend_analysis": trend_text,
        }, status=status.HTTP_200_OK)


class TrendAnalysisAPIView(APIView):
    """
    GET /api/patients/<patient_id>/trend-analysis/
    Returns SpO2 and Vitals trend data for chart display.
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        from patients.models import Vitals
        vitals = Vitals.objects.filter(patient_id=patient_id).order_by('created_at').values(
            'spo2', 'resp_rate', 'heart_rate', 'temperature', 'created_at'
        )
        return Response({
            "patient_id": patient_id,
            "vitals_trend": list(vitals),
        }, status=status.HTTP_200_OK)


class HypoxemiaCauseAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/hypoxemia-cause/
    POST /api/patients/<patient_id>/hypoxemia-cause/
    Body: { cause: "vq_mismatch"|"hypoventilation"|"diffusion"|"shunt"|"unknown" }
    """
    VALID_CAUSES = ['V/Q Mismatch', 'Alveolar Hypoventilation', 'Diffusion Impairment', 'Intrapulmonary Shunt', 'Unknown']

    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        latest = HypoxemiaCause.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        if latest:
            return Response({"patient_id": patient_id, "cause": latest.cause, "recorded_at": latest.created_at}, status=status.HTTP_200_OK)
        return Response({"patient_id": patient_id, "cause": None}, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        cause = request.data.get('cause')
        if cause not in self.VALID_CAUSES:
            return Response({"error": f"cause must be one of: {', '.join(self.VALID_CAUSES)}"}, status=status.HTTP_400_BAD_REQUEST)
        record = HypoxemiaCause.objects.create(patient_id=patient_id, cause=cause)
        return Response({"message": "Cause saved successfully", "patient_id": patient_id, "cause": record.cause}, status=status.HTTP_201_CREATED)


class OxygenRequirementAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/oxygen-requirement/
    POST /api/patients/<patient_id>/oxygen-requirement/
    Body: { lpm_required, target_spo2, rationale }
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        latest = OxygenRequirement.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        if latest:
            return Response({
                "patient_id": patient_id,
                "spo2": latest.spo2,
                "hypoxemia_level": latest.hypoxemia_level,
                "symptoms_level": latest.symptoms_level,
                "oxygen_required": latest.oxygen_required,
                "recorded_at": latest.created_at,
            }, status=status.HTTP_200_OK)
        # Auto-compute from patient data
        from patients.models import Vitals, AbgEntry
        latest_vitals = Vitals.objects.filter(patient_id=patient_id).order_by('-created_at').first()

        # Also check staff reassessment values
        spo2_val = None
        try:
            from staff.models import StaffChecklist
            latest_staff = StaffChecklist.objects.filter(patient_id=patient_id).order_by('-created_at').first()
            vitals_time = latest_vitals.created_at if latest_vitals else None
            staff_time = latest_staff.created_at if latest_staff else None
            if staff_time and (not vitals_time or staff_time > vitals_time):
                if latest_staff.spo2 is not None:
                    spo2_val = latest_staff.spo2
        except ImportError:
            pass

        if spo2_val is None and latest_vitals and latest_vitals.spo2 is not None:
            spo2_val = latest_vitals.spo2

        # COPD target SpO2: 88-92%
        if spo2_val is not None:
            if spo2_val < 85:
                hypoxemia_level = 'Severe'
                symptoms_level = 'Severe'
                oxygen_required = 'Yes'
            elif spo2_val < 88:
                hypoxemia_level = 'Moderate'
                symptoms_level = 'Moderate'
                oxygen_required = 'Yes'
            elif spo2_val <= 92:
                # On target for COPD
                hypoxemia_level = 'None'
                symptoms_level = 'Mild'
                oxygen_required = 'Continue current therapy'
            else:
                # >92% — risk of CO2 retention in COPD
                hypoxemia_level = 'None'
                symptoms_level = 'Mild'
                oxygen_required = 'Reduce — risk of CO2 retention'
        else:
            hypoxemia_level = 'Unknown'
            symptoms_level = 'Unknown'
            oxygen_required = 'Assess patient'

        return Response({
            "patient_id": patient_id,
            "spo2": spo2_val if spo2_val is not None else 0.0,
            "hypoxemia_level": hypoxemia_level,
            "symptoms_level": symptoms_level,
            "oxygen_required": oxygen_required,
        }, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        spo2 = request.data.get('spo2')
        if spo2 is None:
            return Response({"error": "spo2 is required."}, status=status.HTTP_400_BAD_REQUEST)
        record = OxygenRequirement.objects.create(
            patient_id=patient_id,
            spo2=float(spo2),
            hypoxemia_level=request.data.get('hypoxemia_level', ''),
            symptoms_level=request.data.get('symptoms_level', ''),
            oxygen_required=request.data.get('oxygen_required', ''),
        )
        return Response({
            "message": "Oxygen requirement saved.", 
            "patient_id": patient_id, 
            "spo2": record.spo2
        }, status=status.HTTP_201_CREATED)

class CustomOxygenRequirementAPIView(APIView):
    """
    POST /api/patient/oxygen-requirement/
    Body: { "patient_id": 5, "spo2": 86, "hypoxemia_level": "Severe", "symptoms_level": "Moderate", "oxygen_required": "Yes" }
    """
    def post(self, request):
        patient_id = request.data.get('patient_id')
        if not patient_id:
             return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)
             
        try:
            from patients.models import Patient
            patient = Patient.objects.get(id=patient_id)
        except Exception:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        spo2 = request.data.get('spo2')
        record = OxygenRequirement.objects.create(
            patient_id=patient_id,
            spo2=float(spo2) if spo2 is not None else 0.0,
            hypoxemia_level=request.data.get('hypoxemia_level', ''),
            symptoms_level=request.data.get('symptoms_level', ''),
            oxygen_required=request.data.get('oxygen_required', '')
        )
        return Response({"message": "Oxygen requirement saved successfully"}, status=status.HTTP_201_CREATED)


class CustomHypoxemiaCauseAPIView(APIView):
    """
    POST /api/patient/hypoxemia-cause/
    Body: { "patient_id": 3, "cause": "V/Q Mismatch" }
    """
    VALID_CAUSES = ['V/Q Mismatch', 'Alveolar Hypoventilation', 'Diffusion Impairment', 'Intrapulmonary Shunt', 'Unknown']

    def post(self, request):
        patient_id = request.data.get('patient_id')
        cause = request.data.get('cause')
        
        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)
        
        try:
            from patients.models import Patient
            patient = Patient.objects.get(id=patient_id)
        except Exception:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)
            
        if cause not in self.VALID_CAUSES:
            return Response({"error": f"cause must be one of: {', '.join(self.VALID_CAUSES)}"}, status=status.HTTP_400_BAD_REQUEST)
            
        record = HypoxemiaCause.objects.create(patient_id=patient_id, cause=cause)
        return Response({
            "message": "Cause saved successfully"
        }, status=status.HTTP_201_CREATED)


class DeviceSelectionAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/device-selection/
    POST /api/patients/<patient_id>/device-selection/
    Body: { device: "venturi"|"nasal"|"high_flow"|"non_rebreather", rationale }
    """
    VALID_DEVICES = ['venturi', 'nasal', 'high_flow', 'non_rebreather']

    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        latest = DeviceSelection.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        if latest:
            return Response({"patient_id": patient_id, "device": latest.device, "rationale": latest.rationale, "recorded_at": latest.created_at}, status=status.HTTP_200_OK)
        return Response({"patient_id": patient_id, "device": None}, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        device = request.data.get('device')
        if device not in self.VALID_DEVICES:
            return Response({"error": f"device must be one of: {', '.join(self.VALID_DEVICES)}"}, status=status.HTTP_400_BAD_REQUEST)
        record = DeviceSelection.objects.create(
            patient_id=patient_id,
            device=device,
            rationale=request.data.get('rationale', ''),
        )
        return Response({"message": "Device selection saved.", "patient_id": patient_id, "device": record.device}, status=status.HTTP_201_CREATED)


class ReviewRecommendationAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/review-recommendation/
    POST /api/patients/<patient_id>/review-recommendation/
    Body: { decision: "accept"|"override", override_reason }
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        latest = ReviewRecommendation.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        if latest:
            return Response({"patient_id": patient_id, "decision": latest.decision, "override_reason": latest.override_reason, "recorded_at": latest.created_at}, status=status.HTTP_200_OK)
        return Response({"patient_id": patient_id, "decision": None}, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        decision = request.data.get('decision')
        if decision not in ['accept', 'override']:
            return Response({"error": "decision must be 'accept' or 'override'."}, status=status.HTTP_400_BAD_REQUEST)
        override_reason = request.data.get('override_reason', '')
        if decision == 'override' and not override_reason:
            return Response({"error": "override_reason is required when decision is 'override'."}, status=status.HTTP_400_BAD_REQUEST)
        record = ReviewRecommendation.objects.create(patient_id=patient_id, decision=decision, override_reason=override_reason)
        return Response({"message": f"Recommendation {decision}ed.", "patient_id": patient_id, "decision": record.decision}, status=status.HTTP_201_CREATED)


class TherapyRecommendationAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/therapy-recommendation/
    POST /api/patients/<patient_id>/therapy-recommendation/
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        latest = TherapyRecommendation.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        if latest:
            return Response({
                "patient_id": patient_id,
                "therapy_type": latest.therapy_type,
                "flow_rate": latest.flow_rate,
                "device": latest.device,
                "duration": latest.duration,
                "precautions": latest.precautions,
                "recorded_at": latest.created_at,
            }, status=status.HTTP_200_OK)
        # Auto-compute from device selection and oxygen requirement
        device_sel = DeviceSelection.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        o2_req = OxygenRequirement.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        return Response({
            "patient_id": patient_id,
            "therapy_type": "Controlled Oxygen Therapy",
            "flow_rate": 2.0, # Defaulting since lpm_required is removed
            "device": device_sel.device if device_sel else "venturi",
            "duration": "Continuous",
            "precautions": "Maintain SpO2 88–92%. Avoid high-flow oxygen in COPD patients.",
        }, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        therapy_type = request.data.get('therapy_type', 'Controlled Oxygen Therapy')
        flow_rate = request.data.get('flow_rate', 2.0)
        device = request.data.get('device', 'venturi')
        record = TherapyRecommendation.objects.create(
            patient_id=patient_id,
            therapy_type=therapy_type,
            flow_rate=float(flow_rate),
            device=device,
            duration=request.data.get('duration', 'Continuous'),
            precautions=request.data.get('precautions', 'Maintain SpO2 88–92%.'),
        )
        return Response({"message": "Therapy recommendation saved.", "patient_id": patient_id, "therapy_type": record.therapy_type}, status=status.HTTP_201_CREATED)


class NIVRecommendationAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/niv-recommendation/
    POST /api/patients/<patient_id>/niv-recommendation/
    Body: { mode, ipap, epap, indication }
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        latest = NIVRecommendation.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        if latest:
            # Even with saved record, check current ABG to determine if BiPAP is still indicated
            from patients.models import AbgEntry
            latest_abg = AbgEntry.objects.filter(patient_id=patient_id).order_by('-created_at').first()
            bipap_currently_indicated = False
            if latest_abg:
                bipap_currently_indicated = (latest_abg.ph < 7.35 and latest_abg.paco2 > 45)

            return Response({
                "patient_id": patient_id,
                "mode": latest.mode,
                "ipap": latest.ipap,
                "epap": latest.epap,
                "indication": latest.indication,
                "bipap_indicated": bipap_currently_indicated,
                "recorded_at": latest.created_at,
            }, status=status.HTTP_200_OK)

        # --- Dynamically calculate IPAP / EPAP from patient data ---
        # NIV/BiPAP is ONLY indicated when pH < 7.35 AND PaCO2 > 45
        # (Acute hypercapnic respiratory failure criteria for COPD)
        from patients.models import Vitals, AbgEntry
        latest_abg = AbgEntry.objects.filter(patient_id=patient_id).order_by('-created_at').first()

        # Base pressures
        ipap = 10.0
        epap = 4.0
        mode = "BiPAP"
        indication_parts = []
        bipap_indicated = False

        if latest_abg:
            ph_val = latest_abg.ph
            paco2_val = latest_abg.paco2

            # BiPAP is only indicated when BOTH conditions are met:
            # 1. pH < 7.35 (respiratory acidosis)
            # 2. PaCO2 > 45 (hypercapnia)
            if ph_val < 7.35 and paco2_val > 45:
                bipap_indicated = True

                # Adjust IPAP based on PaCO2 severity
                if paco2_val > 60:
                    ipap = 20.0
                    indication_parts.append("Severe hypercapnia (PaCO2 > 60 mmHg)")
                elif paco2_val > 50:
                    ipap = 16.0
                    indication_parts.append("Moderate hypercapnia (PaCO2 > 50 mmHg)")
                else:
                    ipap = 14.0
                    indication_parts.append("Mild hypercapnia (PaCO2 > 45 mmHg)")

                # Further bump IPAP if severe acidosis
                if ph_val < 7.25:
                    ipap = min(ipap + 4, 24.0)
                    epap = 6.0
                    indication_parts.append("Severe acidosis (pH < 7.25)")
                else:
                    ipap = min(ipap + 2, 24.0)
                    epap = 5.0
                    indication_parts.append("Respiratory acidosis (pH < 7.35)")

        if bipap_indicated:
            indication = "Acute hypercapnic respiratory failure with " + ", ".join(indication_parts) + "."
        else:
            indication = "BiPAP not indicated — requires both pH < 7.35 and PaCO2 > 45 mmHg."
            ipap = 10.0
            epap = 4.0

        return Response({
            "patient_id": patient_id,
            "mode": mode,
            "ipap": ipap,
            "epap": epap,
            "indication": indication,
            "bipap_indicated": bipap_indicated,
        }, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        mode = request.data.get('mode', 'BiPAP')
        ipap = request.data.get('ipap')
        epap = request.data.get('epap')
        if not ipap or not epap:
            return Response({"error": "ipap and epap are required."}, status=status.HTTP_400_BAD_REQUEST)
        record = NIVRecommendation.objects.create(
            patient_id=patient_id,
            mode=mode,
            ipap=float(ipap),
            epap=float(epap),
            indication=request.data.get('indication', ''),
        )
        return Response({"message": "NIV recommendation saved.", "patient_id": patient_id, "mode": record.mode}, status=status.HTTP_201_CREATED)


class EscalationCriteriaAPIView(APIView):
    """
    GET /api/patients/<patient_id>/escalation-criteria/
    Returns escalation assessment based on current clinical data.
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        from patients.models import Vitals, AbgEntry
        latest_vitals = Vitals.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        latest_abg = AbgEntry.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        escalation_triggers = []
        criteria_met = False

        if latest_vitals:
            if latest_vitals.spo2 < 85:
                escalation_triggers.append("SpO2 < 85% — Critical hypoxaemia")
                criteria_met = True
            if latest_vitals.respiratory_rate > 35:
                escalation_triggers.append("Respiratory rate > 35 breaths/min")
                criteria_met = True

        if latest_abg:
            if latest_abg.ph < 7.25:
                escalation_triggers.append("pH < 7.25 — Severe respiratory acidosis")
                criteria_met = True
            if latest_abg.paco2 > 70:
                escalation_triggers.append("PaCO2 > 70 mmHg — Severe hypercapnia")
                criteria_met = True

        try:
            EscalationCriteria.objects.create(
                patient_id=patient_id, criteria_met=criteria_met, details=str(escalation_triggers)
            )
        except Exception:
            pass  # Don't let DB save failure break the API response

        return Response({
            "patient_id": patient_id,
            "criteria_met": criteria_met,
            "escalation_triggers": escalation_triggers,
            "recommendation": "Consider ICU escalation / NIV." if criteria_met else "Continue current management.",
        }, status=status.HTTP_200_OK)


class ScheduleReassessmentAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/schedule-reassessment/
    POST /api/patients/<patient_id>/schedule-reassessment/
    Body: { interval: "30m"|"1h"|"2h"|"4h", reassessment_type: "SpO2"|"ABG" }
    """
    VALID_INTERVALS = ['30m', '1h', '2h', '4h']

    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        records = ScheduleReassessment.objects.filter(patient_id=patient_id).order_by('-created_at').values()
        return Response({"patient_id": patient_id, "reassessments": list(records)}, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        interval = request.data.get('interval')
        if interval not in self.VALID_INTERVALS:
            return Response({"error": f"interval must be one of: {', '.join(self.VALID_INTERVALS)}"}, status=status.HTTP_400_BAD_REQUEST)
        interval_map = {'30m': 30, '1h': 60, '2h': 120, '4h': 240}
        minutes = interval_map[interval]
        from datetime import timedelta
        scheduled_at = timezone.now() + timedelta(minutes=minutes)

        # Lookup patient info
        from patients.models import Patient
        try:
            p = Patient.objects.get(id=patient_id)
            p_name = p.full_name
            p_bed = p.bed_number or ''
            p_ward = p.ward or ''
        except Patient.DoesNotExist:
            p_name = ''
            p_bed = ''
            p_ward = ''

        reassessment_type = request.data.get('reassessment_type', 'SpO2')
        scheduled_by = request.data.get('scheduled_by', 'doctor')

        record = ScheduleReassessment.objects.create(
            patient_id=patient_id,
            patient_name=p_name,
            bed_no=p_bed,
            ward_no=p_ward,
            reassessment_type=reassessment_type,
            reassessment_minutes=minutes,
            scheduled_time=scheduled_at,
            status='pending',
            scheduled_by=scheduled_by,
        )
        return Response({
            "message": f"Reassessment scheduled in {interval}.",
            "patient_id": patient_id,
            "reassessment_type": reassessment_type,
            "scheduled_time": record.scheduled_time.strftime("%Y-%m-%d %H:%M:%S") if record.scheduled_time else None,
        }, status=status.HTTP_201_CREATED)



class UrgentActionAPIView(APIView):
    """
    GET  /api/patients/<patient_id>/urgent-action/
    POST /api/patients/<patient_id>/urgent-action/
    Body: { action_type, description }
    """
    def get(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        from patients.models import Vitals, AbgEntry
        latest_vitals = Vitals.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        latest_abg = AbgEntry.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        
        icu_triggers = []
        icu_required = False

        if latest_abg and latest_abg.ph < 7.25:
            icu_triggers.append("pH < 7.25 despite NIV")
            icu_required = True
        
        if latest_vitals:
            # Use severe distress markers
            if latest_vitals.respiratory_rate > 40:
                icu_triggers.append("Respiratory rate > 40 (Severe distress)")
                icu_required = True
            if latest_vitals.spo2 < 80:
                icu_triggers.append("SpO2 < 80% on high flow")
                icu_required = True

        return Response({
            "patient_id": patient_id, 
            "icu_required": icu_required,
            "triggers": icu_triggers
        }, status=status.HTTP_200_OK)

    def post(self, request, patient_id):
        patient, err = get_patient_or_404(patient_id)
        if err:
            return err
        action_type = request.data.get('action_type')
        if not action_type:
            return Response({"error": "action_type is required."}, status=status.HTTP_400_BAD_REQUEST)
        record = UrgentAction.objects.create(
            patient_id=patient_id,
            action_type=action_type,
            description=request.data.get('description', ''),
            status='pending',
        )
        return Response({
            "message": "Urgent action logged.",
            "patient_id": patient_id,
            "action_type": record.action_type,
            "status": record.status,
        }, status=status.HTTP_201_CREATED)


class AIDeviceRecommendationAPIView(APIView):
    """
    GET /api/patient/device-recommendation/<patient_id>/
    Uses trained ML model to recommend oxygen delivery device.
    Updated model uses 7 features: SpO2, pH, PaCO2, Respiratory_Rate, PaO2, HCO3, Heart_Rate
    """
    # Map from ML model output names to flow ranges
    DEVICE_FLOW_MAP = {
        'Venturi Mask': '24% - 60%',
        'Nasal Cannula': '1 - 4 L/min',
        'High-Flow Nasal Cannula (HFNC)': '30 - 60 L/min',
        'Non-Rebreather Mask (NRBM)': '60% - 90%',
    }

    # Map from ML model output names to normalized names used by the Android app
    DEVICE_NAME_NORMALIZE = {
        'Venturi Mask': 'Venturi Mask',
        'Nasal Cannula': 'Nasal Cannula',
        'High-Flow Nasal Cannula (HFNC)': 'High Flow Nasal Cannula',
        'Non-Rebreather Mask (NRBM)': 'Non-Rebreather Mask',
    }

    def get(self, request, patient_id):
        from patients.models import Patient, Vitals, AbgEntry
        import os, joblib, numpy as np

        # Validate patient
        try:
            patient = Patient.objects.get(id=patient_id)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        # Fetch latest vitals and ABG data
        vitals = Vitals.objects.filter(patient_id=patient_id).order_by('-created_at').first()
        abg = AbgEntry.objects.filter(patient_id=patient_id).order_by('-created_at').first()

        # Also check staff reassessment for latest SpO2
        spo2_val = None
        try:
            from staff.models import StaffChecklist
            latest_staff = StaffChecklist.objects.filter(patient_id=patient_id).order_by('-created_at').first()
            vitals_time = vitals.created_at if vitals else None
            staff_time = latest_staff.created_at if latest_staff else None
            if staff_time and (not vitals_time or staff_time > vitals_time):
                if latest_staff.spo2 is not None:
                    spo2_val = float(latest_staff.spo2)
        except (ImportError, Exception):
            pass

        # Vitals values (use latest available)
        if spo2_val is None:
            spo2_val = float(vitals.spo2) if vitals and vitals.spo2 is not None else 90.0
        rr = float(vitals.respiratory_rate) if vitals and vitals.respiratory_rate is not None else 20.0
        hr = float(vitals.heart_rate) if vitals and vitals.heart_rate is not None else 80.0

        # ABG values
        ph = float(abg.ph) if abg and abg.ph is not None else 7.38
        pao2 = float(abg.pao2) if abg and abg.pao2 is not None else 75.0
        paco2 = float(abg.paco2) if abg and abg.paco2 is not None else 42.0
        hco3 = float(abg.hco3) if abg and abg.hco3 is not None else 24.0

        # Load updated model and encoder
        # Path: therapy/views.py -> therapy/ -> copd/ -> CDSS COPD/ -> CDSS_COPD/ml_model/trained_model/
        model_dir = os.path.join(
            os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
            'CDSS_COPD', 'ml_model', 'trained_model'
        )

        # Rule-based fallback recommendation (always available, no ML needed)
        def get_clinical_recommendation(spo2, ph, paco2, rr):
            """Clinical guidelines-based device recommendation for COPD"""
            if spo2 < 85 or (ph < 7.25 and paco2 > 60):
                return 'Non-Rebreather Mask'
            elif spo2 < 88 or (ph < 7.35 and paco2 > 50):
                return 'High Flow Nasal Cannula'
            elif spo2 < 92 or paco2 > 45 or rr > 25:
                return 'Venturi Mask'
            else:
                return 'Nasal Cannula'

        # Try ML model first, fall back to clinical rules
        recommended_device = None
        confidence = 0.0

        try:
            model = joblib.load(os.path.join(model_dir, 'model.pkl'))
            le_device = joblib.load(os.path.join(model_dir, 'encoder.pkl'))

            # Build feature vector — 7 features matching the updated training data
            # Column order: SpO2, pH, PaCO2, Respiratory_Rate, PaO2, HCO3, Heart_Rate
            features = np.array([[
                spo2_val, ph, paco2, rr, pao2, hco3, hr
            ]])

            prediction = model.predict(features)[0]
            probabilities = model.predict_proba(features)[0]
            confidence = float(max(probabilities))
            raw_device_name = le_device.inverse_transform([prediction])[0]

            # Normalize the device name for the Android app
            recommended_device = self.DEVICE_NAME_NORMALIZE.get(raw_device_name, raw_device_name)
        except Exception as e:
            print(f"[DeviceRecommendation] ML model error: {e}, using clinical rules")
            recommended_device = None

        # Fall back to clinical rules if ML device is None or not recognized
        valid_devices = {'Venturi Mask', 'Nasal Cannula', 'High Flow Nasal Cannula', 'Non-Rebreather Mask'}
        if recommended_device is None or recommended_device not in valid_devices:
            recommended_device = get_clinical_recommendation(spo2_val, ph, paco2, rr)
            confidence = 0.85  # Clinical confidence

        flow_map = {
            'Venturi Mask': '24% - 60%',
            'Nasal Cannula': '1 - 4 L/min',
            'High Flow Nasal Cannula': '30 - 60 L/min',
            'Non-Rebreather Mask': '60% - 90%',
        }
        flow_range = flow_map.get(recommended_device, '')

        return Response({
            "patient_id": patient_id,
            "recommended_device": recommended_device,
            "target_spo2": "88-92",
            "flow_range": flow_range,
            "confidence_score": round(confidence, 2),
            "input_features": {
                "spo2": spo2_val,
                "respiratory_rate": rr,
                "heart_rate": hr,
                "ph": ph,
                "pao2": pao2,
                "paco2": paco2,
                "hco3": hco3,
            }
        }, status=status.HTTP_200_OK)


class CustomDeviceSelectionAPIView(APIView):
    """
    POST /api/patient/device-selection/
    Body: { "patient_id": 5, "selected_device": "Venturi Mask", "flow_range": "24% - 60%" }
    """
    def post(self, request):
        patient_id = request.data.get('patient_id')
        if not patient_id:
            return Response({"error": "patient_id is required."}, status=status.HTTP_400_BAD_REQUEST)

        try:
            from patients.models import Patient
            Patient.objects.get(id=patient_id)
        except Exception:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)

        device = request.data.get('selected_device', '')
        flow_range = request.data.get('flow_range', '')

        record = DeviceSelection.objects.create(
            patient_id=patient_id,
            device=device,
            flow_range=flow_range,
            rationale=f"AI recommended: {device}",
        )
        return Response({
            "message": "Device selection saved successfully",
            "patient_id": patient_id,
            "device": record.device
        }, status=status.HTTP_201_CREATED)

