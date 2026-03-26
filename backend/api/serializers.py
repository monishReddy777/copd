from rest_framework import serializers
from django.utils import timezone
from datetime import timedelta
from .models import *
import re
from django.core.exceptions import ValidationError as DjangoValidationError


class SignupSerializer(serializers.Serializer):
    name = serializers.CharField(required=False)
    full_name = serializers.CharField(required=False)
    username = serializers.CharField(required=True)
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)
    role = serializers.CharField()
    phone_number = serializers.CharField(required=False, allow_blank=True)
    specialization = serializers.CharField(required=False, allow_blank=True)
    license_number = serializers.CharField(required=False, allow_blank=True)
    department = serializers.CharField(required=False, allow_blank=True)
    staff_id = serializers.CharField(required=False, allow_blank=True)

    def validate_password(self, value):
        if len(value) < 8:
            raise serializers.ValidationError("Password must be at least 8 characters long.")
        if not re.search(r'[A-Z]', value):
            raise serializers.ValidationError("Password must contain at least one uppercase letter.")
        if not re.search(r'[a-z]', value):
            raise serializers.ValidationError("Password must contain at least one lowercase letter.")
        if not re.search(r'\d', value):
            raise serializers.ValidationError("Password must contain at least one digit.")
        if not re.search(r'[!@#$%^&*(),.?":{}|<>+=-]', value):
            raise serializers.ValidationError("Password must contain at least one special character.")
        return value

    def validate_username(self, value):
        if CustomUser.objects.filter(username=value).exists():
            raise serializers.ValidationError("Username is already taken.")
        return value

    def validate_email(self, value):
        user_exists = CustomUser.objects.filter(email=value).first()
        if user_exists:
            if not user_exists.is_approved:
                raise serializers.ValidationError("Your account is waiting for admin approval")
            
            # If user exists and is approved:
            role = self.initial_data.get('role', 'staff').lower()
            if 'staff' in role or 'clinical' in role:
                raise serializers.ValidationError("Email already registered. Please login or use another email.")
            else:
                raise serializers.ValidationError("Email already registered.")
        
        # Check if email is verified via OTP
        otp_record = EmailOTP.objects.filter(
            email=value, 
            purpose='signup', 
            is_verified=True,
            expires_at__gt=timezone.now()
        ).first()
        
        if not otp_record:
            raise serializers.ValidationError("Email not verified. Please verify your email with OTP first.")
            
        if not value.lower().endswith('@gmail.com'):
            raise serializers.ValidationError("Only @gmail.com email addresses are allowed.")

        return value

    def create(self, validated_data):
        # Resolve name
        name = validated_data.get('name') or validated_data.get('full_name', '')
        email = validated_data['email']
        password = validated_data['password']

        # Normalize role
        raw_role = validated_data.get('role', 'staff').lower()
        if 'staff' in raw_role or 'clinical' in raw_role:
            role = 'staff'
        elif 'doctor' in raw_role or 'physician' in raw_role:
            role = 'doctor'
        else:
            role = 'staff'

        # Split full name
        parts = name.split(' ', 1)
        first_name = parts[0]
        last_name = parts[1] if len(parts) > 1 else ''

        # 1. Create auth user in users table
        user = CustomUser.objects.create_user(
            username=validated_data['username'],
            email=email,
            password=password,
            first_name=first_name,
            last_name=last_name,
            role=role,
            phone_number=validated_data.get('phone_number', ''),
            is_approved=False,
            is_active=False
        )

        # 2. Also insert into doctor or staff table
        if role == 'doctor':
            Doctor.objects.create(
                user=user,
                name=name,
                email=email,
                specialization=validated_data.get('specialization', 'General'),
                license_number=validated_data.get('license_number', f'PENDING-{user.id}'),
                phone=validated_data.get('phone_number', ''),
                status='pending'
            )
        elif role == 'staff':
            Staff.objects.create(
                user=user,
                name=name,
                email=email,
                department=validated_data.get('department', 'General'),
                license_id=validated_data.get('staff_id', f'PENDING-{user.id}'),
                phone=validated_data.get('phone_number', ''),
                status='pending'
            )

        return user

class DoctorSerializer(serializers.ModelSerializer):
    is_active = serializers.BooleanField(source='user.is_active', read_only=True)
    is_approved = serializers.BooleanField(source='user.is_approved', read_only=True)
    class Meta:
        model = Doctor
        fields = ('id', 'name', 'email', 'specialization', 'license_number', 'phone', 'status', 'is_active', 'is_approved')

class StaffSerializer(serializers.ModelSerializer):
    is_active = serializers.BooleanField(source='user.is_active', read_only=True)
    is_approved = serializers.BooleanField(source='user.is_approved', read_only=True)
    class Meta:
        model = Staff
        fields = ('id', 'name', 'email', 'department', 'license_id', 'phone', 'status', 'is_active', 'is_approved')

class CustomUserSerializer(serializers.ModelSerializer):
    doctor_profile = DoctorSerializer(read_only=True)
    staff_profile = StaffSerializer(read_only=True)
    class Meta:
        model = CustomUser
        fields = ('id', 'username', 'email', 'first_name', 'last_name', 'role', 'phone_number', 'department', 'is_approved', 'is_active', 'doctor_profile', 'staff_profile', 'profile_image')

class PatientSerializer(serializers.ModelSerializer):
    latest_vitals = serializers.SerializerMethodField()
    latest_abg = serializers.SerializerMethodField()
    baseline_data = serializers.SerializerMethodField()
    latest_symptoms = serializers.SerializerMethodField()
    latest_spirometry = serializers.SerializerMethodField()
    next_reassessment_time = serializers.SerializerMethodField()
    reassessment_type = serializers.SerializerMethodField()

    class Meta:
        model = Patient
        fields = '__all__'

    def get_next_reassessment_time(self, obj):
        # 1. Look for explicit incomplete schedule
        s = obj.schedules.filter(completed=False).order_by('scheduled_at').first()
        if s:
            return s.scheduled_at
        
        # 2. Fallback: 60 minutes after the last vital check
        v = obj.vitals.order_by('-created_at').first()
        if v:
            return v.created_at + timedelta(minutes=60)
            
        # 3. Last fallback: 60 minutes after patient was created
        return obj.created_at + timedelta(minutes=60)

    def get_reassessment_type(self, obj):
        # Default to SpO2 Check Due as shown in the requirement
        s = obj.schedules.filter(completed=False).order_by('scheduled_at').first()
        if s:
            # In a more complex system, we might have a type field on Schedule
            return "SpO2 Check Due"
        return "SpO2 Check Due" 

    def get_baseline_data(self, obj):
        try:
            b = obj.baseline
            return BaselineDetailsSerializer(b).data
        except:
            return None

    def get_latest_symptoms(self, obj):
        s = obj.symptoms.order_by('-created_at').first()
        if s:
            return SymptomsSerializer(s).data
        return None

    def get_latest_spirometry(self, obj):
        s = obj.spirometry.order_by('-created_at').first()
        if s:
            return SpirometryDataSerializer(s).data
        return None

    def get_latest_vitals(self, obj):
        v = obj.vitals.order_by('-created_at').first()
        if v:
            return {
                'hr': v.heart_rate,
                'rr': v.resp_rate,
                'bp': v.bp,
                'temp': v.temperature,
                'spo2': v.spo2,
                'fio2': v.fio2,
                'loc': v.loc_alert
            }
        return None

    def get_latest_abg(self, obj):
        a = ABGData.objects.filter(patient=obj).order_by('-created_at').first()
        if a:
            return {
                'ph': a.ph,
                'paco2': a.paco2,
                'pao2': a.pao2,
                'hco3': a.hco3
            }
        return None

class SymptomsSerializer(serializers.ModelSerializer):
    class Meta:
        model = Symptoms
        fields = '__all__'

class BaselineDetailsSerializer(serializers.ModelSerializer):
    class Meta:
        model = BaselineDetails
        fields = '__all__'

class SpirometryDataSerializer(serializers.ModelSerializer):
    class Meta:
        model = SpirometryData
        fields = '__all__'
        read_only_fields = ['patient', 'gold_stage']

class ABGDataSerializer(serializers.ModelSerializer):
    class Meta:
        model = ABGData
        fields = '__all__'

class VitalsSerializer(serializers.ModelSerializer):
    class Meta:
        model = Vitals
        fields = '__all__'

    def to_internal_value(self, data):
        # Map frontend fields to model fields
        internal_data = data.copy()
        
        if 'respiratory_rate' in internal_data:
            internal_data['resp_rate'] = internal_data.pop('respiratory_rate')
            
        if 'systolic_bp' in internal_data and 'diastolic_bp' in internal_data:
            sys = internal_data.pop('systolic_bp')
            dia = internal_data.pop('diastolic_bp')
            internal_data['bp'] = f"{sys}/{dia}"
            
        return super().to_internal_value(internal_data)

class OxygenRequirementSerializer(serializers.ModelSerializer):
    class Meta:
        model = OxygenRequirement
        fields = '__all__'

class RecommendationSerializer(serializers.ModelSerializer):
    class Meta:
        model = Recommendation
        fields = '__all__'

class DeviceSelectionSerializer(serializers.ModelSerializer):
    class Meta:
        model = DeviceSelection
        fields = '__all__'

class AlertSerializer(serializers.ModelSerializer):
    class Meta:
        model = Alert
        fields = '__all__'

class ReassessmentScheduleSerializer(serializers.ModelSerializer):
    class Meta:
        model = ReassessmentSchedule
        fields = ['id', 'patient', 'interval_minutes', 'scheduled_at', 'completed', 'created_at']

class ReassessmentChecklistSerializer(serializers.ModelSerializer):
    class Meta:
        model = ReassessmentChecklist
        fields = '__all__'

class NotificationSerializer(serializers.ModelSerializer):
    class Meta:
        model = Notification
        fields = '__all__'
