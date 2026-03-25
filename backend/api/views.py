from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import authenticate
from .models import *
from .serializers import *
from django.contrib.auth.models import User
from django.contrib.auth import get_user_model
from .ai_utils import get_ai_prediction, analyze_trends
from .email_utils import send_otp_email
import random
from datetime import timedelta
from django.utils import timezone

def get_tokens_for_user(user):
    refresh = RefreshToken.for_user(user)
    return {
        'refresh': str(refresh),
        'access': str(refresh.access_token),
    }

class AdminProfileDetailsAPIView(APIView):
    permission_classes = [AllowAny]
    
    def get(self, request):
        User = get_user_model()
        try:
            admin = User.objects.get(id=7)
            return Response({
                "admin_id": admin.id,
                "username": admin.username,
                "name": admin.username,
                "email": admin.email,
                "role": "Super Admin",
                "permissions": "Full System Access"
            }, status=status.HTTP_200_OK)
        except User.DoesNotExist:
            return Response({"error": "Admin not found"}, status=status.HTTP_404_NOT_FOUND)

# --- AUTHENTICATION & USERS ---
class SignupAPIView(APIView):
    permission_classes = [AllowAny]
    def post(self, request):
        print(f"DEBUG Signup Request Data: {request.data}")
        serializer = SignupSerializer(data=request.data)
        if serializer.is_valid():
            user = serializer.save()
            print(f"DEBUG Signup Success: User {user.email} created with role {user.role}")
            return Response({"message": "Signup successful. Waiting for admin approval."}, status=status.HTTP_201_CREATED)
        
        # Pull out custom error messages if they hit validate_email logic
        if 'email' in serializer.errors and isinstance(serializer.errors['email'], list):
            return Response({"message": serializer.errors['email'][0]}, status=status.HTTP_400_BAD_REQUEST)
            
        print(f"DEBUG Signup Errors: {serializer.errors}")
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class LoginAPIView(APIView):
    permission_classes = [AllowAny]
    def post(self, request):
        print(f"DEBUG Login Request Data: {request.data}")
        # Accept both 'username' and 'email' as login identifiers
        username = request.data.get('username')
        password = request.data.get('password')

        if not username or not password:
            print("ERROR Login: Missing username or password")
            return Response({"error": "Username and password are required"}, status=status.HTTP_400_BAD_REQUEST)

        # Strictly authenticate by username
        user = authenticate(username=username, password=password)
        if user is None:
            print(f"ERROR Login: Invalid credentials for user {username}")
            return Response({"error": "Invalid credentials"}, status=status.HTTP_401_UNAUTHORIZED)
            
        if not user.is_active and user.role != 'admin':
            print(f"ERROR Login: Account disabled for user {username}")
            return Response({"error": "Your account is waiting for admin approval"}, status=status.HTTP_403_FORBIDDEN)
            
        if not user.is_approved and user.role != 'admin':
            print(f"ERROR Login: Account not approved for user {username}")
            return Response({"error": "Your account is waiting for admin approval"}, status=status.HTTP_403_FORBIDDEN)

        # Check if doctor/staff account is disabled by admin after approval
        if user.role == 'doctor':
            try:
                doc_profile = user.doctor_profile
                if doc_profile.status == 'disabled':
                    return Response({"error": "Can't able to access. Your account is disabled by admin."}, status=status.HTTP_403_FORBIDDEN)
            except Exception:
                pass
        elif user.role == 'staff':
            try:
                staff_profile = user.staff_profile
                if staff_profile.status == 'disabled':
                    return Response({"error": "Can't able to access. Your account is disabled by admin."}, status=status.HTTP_403_FORBIDDEN)
            except Exception:
                pass
            
        tokens = get_tokens_for_user(user)
        
        # Admin login or subsequent logins (where last_login is already set) skip OTP
        if user.role == 'admin' or user.last_login is not None:
            user.last_login = timezone.now()
            user.save(update_fields=['last_login'])
            print(f"SUCCESS Login: {user.role} {username} authenticated (No OTP required)")
            return Response({
                "access": tokens['access'],
                "refresh": tokens['refresh'],
                "role": user.role,
                "user_id": user.id
            })
            
        # First-time login for Doctor and Staff require OTP
        otp = str(random.randint(100000, 999999))
        expires_at = timezone.now() + timedelta(minutes=10)
        
        # Save OTP for login
        EmailOTP.objects.filter(email=user.email, purpose='login').delete() # Clear old ones
        EmailOTP.objects.create(
            email=user.email,
            otp=otp,
            purpose='login',
            expires_at=expires_at
        )
        
        if send_otp_email(user.email, otp, 'login'):
            print(f"SUCCESS Login Step 1: OTP sent to {user.email}")
            return Response({
                "otp_required": True,
                "email": user.email,
                "message": "OTP sent to your email. Please verify to complete login."
            })
        else:
            return Response({"error": "Failed to send OTP. Please try again."}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

class RequestOTPAPIView(APIView):
    permission_classes = [AllowAny]
    
    def post(self, request):
        # Can provide either 'email' or 'username'
        identifier = request.data.get('email') or request.data.get('username')
        purpose = request.data.get('purpose', 'signup')

        if not identifier:
            return Response({"error": "Email or Username is required"}, status=status.HTTP_400_BAD_REQUEST)

        # Resolve email from username if needed
        email = identifier
        if '@' not in identifier:
            user = CustomUser.objects.filter(username=identifier).first()
            if user:
                email = user.email
            else:
                return Response({"error": "User with this username not found"}, status=status.HTTP_404_NOT_FOUND)
        
        # For signup, check if user already exists
        if purpose == 'signup':
            if CustomUser.objects.filter(email=email).exists():
                return Response({"error": "Email already registered"}, status=400)
        
        otp = str(random.randint(100000, 999999))
        expires_at = timezone.now() + timedelta(minutes=10)
        
        EmailOTP.objects.filter(email=email, purpose=purpose).delete()
        EmailOTP.objects.create(
            email=email,
            otp=otp,
            purpose=purpose,
            expires_at=expires_at
        )
        
        if send_otp_email(email, otp, purpose):
            return Response({"message": f"OTP sent to {email}"})
        else:
            return Response({"error": "Failed to send OTP"}, status=500)

class VerifyOTPAPIView(APIView):
    permission_classes = [AllowAny]
    
    def post(self, request):
        email = request.data.get('email')
        username = request.data.get('username')
        otp = request.data.get('otp')
        purpose = request.data.get('purpose', 'signup')
        
        # Resolve email from username if missing (for login flow)
        if not email and username:
            user = CustomUser.objects.filter(username=username).first()
            if user:
                email = user.email
            else:
                return Response({"error": "User with this username not found"}, status=status.HTTP_404_NOT_FOUND)

        if not email or not otp:
            return Response({"error": "Email and OTP are required"}, status=status.HTTP_400_BAD_REQUEST)
            
        otp_record = EmailOTP.objects.filter(
            email=email, 
            otp=otp, 
            purpose=purpose,
            expires_at__gt=timezone.now()
        ).first()
        
        if not otp_record:
            return Response({"error": "Invalid or expired OTP"}, status=400)
            
        otp_record.is_verified = True
        otp_record.save()
        
        if purpose == 'login':
            # Return tokens for login
            try:
                user = CustomUser.objects.get(email=email)
                user.last_login = timezone.now()
                user.save(update_fields=['last_login'])
                tokens = get_tokens_for_user(user)
                return Response({
                    "access": tokens['access'],
                    "refresh": tokens['refresh'],
                    "role": user.role,
                    "user_id": user.id,
                    "message": "Login successful"
                })
            except CustomUser.DoesNotExist:
                return Response({"error": "User not found"}, status=404)
                
        return Response({"message": "Email verified successfully"})

class ProfileAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        serializer = CustomUserSerializer(request.user, context={'request': request})
        return Response(serializer.data)
        
    def put(self, request):
        user = request.user
        role = user.role
        data = request.data
        
        # Update CustomUser fields
        if 'email' in data:
            email = data['email']
            if not email.lower().endswith('@gmail.com'):
                return Response({"error": "Only @gmail.com email addresses are allowed."}, status=400)
            user.email = email
        if 'first_name' in data: user.first_name = data['first_name']
        if 'last_name' in data: user.last_name = data['last_name']
        if 'phone_number' in data: user.phone_number = data['phone_number']
        
        # Handle profile image upload
        print(f"DEBUG: Profile update data: {data}")
        print(f"DEBUG: Profile request FILES: {request.FILES}")
        if 'profile_image' in request.FILES:
            image_file = request.FILES['profile_image']
            print(f"DEBUG: Received profile_image: {image_file.name}, size: {image_file.size}")
            user.profile_image = image_file
        else:
            print("DEBUG: No profile_image found in request.FILES")
            
        user.save()
        print(f"DEBUG: User saved. profile_image path: {user.profile_image.name if user.profile_image else 'None'}")
        
        # Update Role-specific profile
        if role == 'doctor':
            try:
                doc = user.doctor_profile
                if 'name' in data: doc.name = data['name']
                if 'specialization' in data: doc.specialization = data['specialization']
                if 'license_number' in data: doc.license_number = data['license_number']
                if 'phone' in data: doc.phone = data['phone']
                doc.save()
            except Doctor.DoesNotExist: pass
        elif role == 'staff':
            try:
                stf = user.staff_profile
                if 'name' in data: stf.name = data['name']
                if 'department' in data: stf.department = data['department']
                if 'license_id' in data: stf.license_id = data['license_id']
                if 'phone' in data: stf.phone = data['phone']
                stf.save()
            except Staff.DoesNotExist: pass
            
        return Response(CustomUserSerializer(user, context={'request': request}).data)

# --- ADMIN MODULE ---
class AdminDashboardAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        if request.user.role != 'admin': return Response(status=403)
        return Response({
            "total_doctors": CustomUser.objects.filter(role='doctor').count(),
            "total_staff": CustomUser.objects.filter(role='staff').count(),
            "pending_approvals": CustomUser.objects.filter(is_approved=False).exclude(role='admin').count()
        })

class AdminDoctorListAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        if request.user.role != 'admin': return Response(status=403)
        doctors = CustomUser.objects.filter(role='doctor')
        return Response(CustomUserSerializer(doctors, many=True).data)

class AdminDoctorDetailAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        if request.user.role != 'admin': return Response(status=403)
        try:
            doc = CustomUser.objects.get(id=pk, role='doctor')
            return Response(CustomUserSerializer(doc).data)
        except CustomUser.DoesNotExist:
            return Response(status=404)
            
    def delete(self, request, pk):
        if request.user.role != 'admin': return Response(status=403)
        try:
            doc_user = CustomUser.objects.get(id=pk, role='doctor')
            # Deleting the user will cascade delete the doctor profile if set to CASCADE, 
            # or we handle it based on on_delete in models.
            doc_user.delete()
            return Response({"message": "Doctor removed successfully"}, status=204)
        except CustomUser.DoesNotExist:
            return Response(status=404)

class AdminStaffListAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        if request.user.role != 'admin': return Response(status=403)
        staff = CustomUser.objects.filter(role='staff')
        return Response(CustomUserSerializer(staff, many=True).data)

class AdminStaffDetailAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        if request.user.role != 'admin': return Response(status=403)
        try:
            staff = CustomUser.objects.get(id=pk, role='staff')
            return Response(CustomUserSerializer(staff).data)
        except CustomUser.DoesNotExist:
            return Response(status=404)
            
    def delete(self, request, pk):
        if request.user.role != 'admin': return Response(status=403)
        try:
            staff_user = CustomUser.objects.get(id=pk, role='staff')
            staff_user.delete()
            return Response({"message": "Staff removed successfully"}, status=204)
        except CustomUser.DoesNotExist:
            return Response(status=404)

class ApprovalRequestsAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        if request.user.role != 'admin': return Response(status=403)
        # Return all unapproved users (including is_active=False newly signed-up ones)
        pending_users = CustomUser.objects.filter(is_approved=False).exclude(role='admin')
        data = []
        for u in pending_users:
            profile_name = u.username
            license_val = "N/A"
            if u.role == 'doctor':
                try: 
                    doc = u.doctor_profile
                    profile_name = doc.name
                    license_val = doc.license_number
                except: pass
            elif u.role == 'staff':
                try: 
                    stf = u.staff_profile
                    profile_name = stf.name
                    license_val = stf.license_id
                except: pass
            
            data.append({
                "id": u.id,
                "name": profile_name,
                "email": u.email,
                "role": u.role,
                "user_type": u.role,
                "license": license_val,
                "status": "pending"
            })
        return Response(data)

class AdminApproveUserAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request):
        if request.user.role != 'admin': return Response(status=403)
        user_id = request.data.get('user_id')
        try:
            u = CustomUser.objects.get(id=user_id)
            u.is_approved = True
            u.is_active = True   # Activate so they can log in
            u.save()
            
            if u.role == 'doctor':
                try:
                    doc = u.doctor_profile
                    if doc:
                        doc.status = 'active'
                        doc.is_active = True
                        doc.save()
                    else:
                        raise AttributeError # Trigger creation
                except (getattr(CustomUser, 'doctor_profile').RelatedObjectDoesNotExist, AttributeError):
                    Doctor.objects.create(
                        user=u, name=u.first_name + " " + u.last_name, email=u.email,
                        status='active', is_active=True, license_number=f'APPROVED-{u.id}'
                    )
            elif u.role == 'staff':
                try:
                    staff = u.staff_profile
                    if staff:
                        staff.status = 'active'
                        staff.save()
                    else:
                        raise AttributeError
                except (getattr(CustomUser, 'staff_profile').RelatedObjectDoesNotExist, AttributeError):
                    Staff.objects.create(
                        user=u, name=u.first_name + " " + u.last_name, email=u.email,
                        status='active', license_id=f'APPROVED-{u.id}'
                    )
            
            return Response({"message": "Approved"})
        except CustomUser.DoesNotExist:
            return Response({"error": "User not found"}, status=404)

class AdminRejectUserAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request):
        if request.user.role != 'admin': 
            return Response({"error": "Admin access required"}, status=status.HTTP_403_FORBIDDEN)
        
        user_id = request.data.get('user_id')
        if not user_id:
            return Response({"error": "user_id is required"}, status=status.HTTP_400_BAD_REQUEST)
            
        try:
            # Handle potential string-to-int conversion
            try:
                target_id = int(user_id)
            except ValueError:
                return Response({"error": "Invalid user_id format"}, status=status.HTTP_400_BAD_REQUEST)
                
            u = CustomUser.objects.get(id=target_id)
            u.is_active = False # Rejecting effectively disables the registration
            u.is_approved = False # Ensure it stays unapproved
            u.save()
            
            # Explicitly mark profile as rejected if it exists
            if u.role == 'doctor' and hasattr(u, 'doctor_profile'):
                u.doctor_profile.status = 'rejected'
                u.doctor_profile.is_active = False
                u.doctor_profile.save()
            elif u.role == 'staff' and hasattr(u, 'staff_profile'):
                u.staff_profile.status = 'rejected'
                u.staff_profile.save()
                
            return Response({"message": "Rejected", "user_id": target_id}, status=status.HTTP_200_OK)
        except CustomUser.DoesNotExist:
            return Response({"error": f"User with ID {user_id} not found"}, status=status.HTTP_404_NOT_FOUND)
        except Exception as e:
            return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

# --- PATIENTS MODULE (STAFF & DOCTOR) ---
class PatientListCreateAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        patients = Patient.objects.all()
        return Response(PatientSerializer(patients, many=True).data)

    def post(self, request):
        serializer = PatientSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(created_by=request.user)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class PatientDetailAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            return Response(PatientSerializer(p).data)
        except Patient.DoesNotExist:
            return Response(status=404)

    def delete(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            p.delete()
            return Response({"message": "Patient deleted successfully"}, status=200)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found"}, status=404)

# --- CLINICAL DATA & AI EXECUTORS ---
class VitalsAPIView(APIView):
    permission_classes = [IsAuthenticated]
    
    def get(self, request, pk):
        try: p = Patient.objects.get(pk=pk)
        except: return Response(status=404)
        vitals = Vitals.objects.filter(patient=p).order_by('-created_at')
        return Response(VitalsSerializer(vitals, many=True).data)

    def post(self, request, pk):
        try: p = Patient.objects.get(pk=pk)
        except: return Response(status=404)
        
        serializer = VitalsSerializer(data=request.data)
        if serializer.is_valid():
            vitals = serializer.save(patient=p)
            
            # AI LOGIC: SpO2
            spo2 = vitals.spo2
            if spo2 < 80:
                Alert.objects.create(patient=p, severity='critical', message=f'Critical SpO2 Drop: {spo2}% for patient {p.full_name}')
                p.status = 'critical'
                # Notify Doctors
                for doc in CustomUser.objects.filter(role='doctor'):
                    Notification.objects.create(user=doc, title='Critical SpO2 Alert', message=f'Patient {p.full_name} SpO2 dropped below 80% ({spo2}%)')
            elif spo2 < 88:
                Alert.objects.create(patient=p, severity='warning', message=f'Warning SpO2: {spo2}% for patient {p.full_name}')
                p.status = 'warning'
                # Notify Doctors
                for doc in CustomUser.objects.filter(role='doctor'):
                    Notification.objects.create(user=doc, title='Warning SpO2 Alert', message=f'Patient {p.full_name} SpO2 dropped to {spo2}%')
            else:
                p.status = 'stable'
            p.save()

            # TRIGGER AI RECOMMENDATION FOR THERAPY
            try:
                generate_ai_recommendation(p)
            except Exception as ai_err:
                print(f"DEBUG: AI Recommendation failed for patient {p.id}: {str(ai_err)}")
                # We don't fail the whole request if only AI fails
            
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class ABGDataAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        try: p = Patient.objects.get(pk=pk)
        except: return Response(status=404)
        abg = ABGData.objects.filter(patient=p).order_by('-created_at')
        return Response(ABGDataSerializer(abg, many=True).data)

    def post(self, request, pk):
        try: p = Patient.objects.get(pk=pk)
        except: return Response(status=404)
        
        serializer = ABGDataSerializer(data=request.data)
        if serializer.is_valid():
            abg = serializer.save(patient=p)
            
            # AI LOGIC: PaCO2 > 45 -> NIV
            if abg.paco2 > 45:
                Recommendation.objects.create(
                    patient=p, 
                    rec_type='niv', 
                    content='PaCO2 > 45 mmHg detected. Recommend considering NIV.'
                )
                for doc in CustomUser.objects.filter(role='doctor'):
                    Notification.objects.create(user=doc, title='NIV Recommendation', message=f'NIV Recommended for {p.full_name}')

            # TRIGGER AI RECOMMENDATION FOR THERAPY
            generate_ai_recommendation(p)

            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class SpirometryAPIView(APIView):
    permission_classes = [IsAuthenticated]
    
    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            data = SpirometryData.objects.filter(patient=p).order_by('-created_at')
            return Response(SpirometryDataSerializer(data, many=True).data)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found"}, status=404)

    def post(self, request, pk):
        try: p = Patient.objects.get(pk=pk)
        except: return Response(status=404)
        
        serializer = SpirometryDataSerializer(data=request.data)
        if serializer.is_valid():
            fev1 = serializer.validated_data.get('fev1', 0)
            fev1_fvc = serializer.validated_data.get('fev1_fvc', 1.0)
            
            gold_stage = None
            # AI LOGIC: GOLD Classification
            # COPD is typically diagnosed when FEV1/FVC < 0.7
            if fev1_fvc < 0.7:
                if fev1 >= 80: gold_stage = 1
                elif 50 <= fev1 <= 79: gold_stage = 2
                elif 30 <= fev1 <= 49: gold_stage = 3
                else: gold_stage = 4
            else:
                gold_stage = 0 # Non-COPD or Normal by GOLD standards
            
            serializer.save(patient=p, gold_stage=gold_stage)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

# Basic CRUD implementations for remaining Models
class SymptomsAPIView(APIView):
    permission_classes = [IsAuthenticated]
    
    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            symptoms = Symptoms.objects.filter(patient=p).order_by('-created_at')
            return Response(SymptomsSerializer(symptoms, many=True).data)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found"}, status=404)

    def post(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            serializer = SymptomsSerializer(data=request.data)
            if serializer.is_valid():
                serializer.save(patient=p)
                return Response(serializer.data, status=201)
            return Response(serializer.errors, status=400)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found"}, status=404)

class BaselineDetailsAPIView(APIView):
    permission_classes = [IsAuthenticated]
    
    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            baseline = getattr(p, 'baseline', None)
            if baseline:
                return Response(BaselineDetailsSerializer(baseline).data)
            return Response({"error": "Baseline details not found"}, status=404)
        except Patient.DoesNotExist:
            return Response({"error": "Patient not found"}, status=404)

    def post(self, request, pk):
        p = Patient.objects.get(pk=pk)
        serializer = BaselineDetailsSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(patient=p)
            return Response(serializer.data, status=201)
        return Response(serializer.errors, status=400)

class AlertListAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        alerts = Alert.objects.all().order_by('-created_at')
        return Response(AlertSerializer(alerts, many=True).data)

class RecommendationAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        p = Patient.objects.get(pk=pk)
        recs = Recommendation.objects.filter(patient=p)
        return Response(RecommendationSerializer(recs, many=True).data)

# Doctor Overrides
class HandleRecommendationAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request, rec_id):
        try:
            rec = Recommendation.objects.get(id=rec_id)
            action = request.data.get('action') # "accept" or "override"
            if action == 'override':
                rec.status = 'overridden'
                rec.override_reason = request.data.get('reason')
            else:
                rec.status = 'accepted'
            rec.save()
            return Response({"message": "Recommendation updated."})
        except Recommendation.DoesNotExist:
            return Response(status=404)

class OxygenRequirementAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request, pk):
        p = Patient.objects.get(pk=pk)
        serializer = OxygenRequirementSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(patient=p)
            return Response(serializer.data, status=201)
        return Response(serializer.errors, status=400)
        
class ReassessmentAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request, pk):
        p = Patient.objects.get(pk=pk)
        serializer = ReassessmentChecklistSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(patient=p)
            return Response(serializer.data, status=201)
        return Response(serializer.errors, status=400)

class NotificationAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        notifs = Notification.objects.filter(user=request.user)
        return Response(NotificationSerializer(notifs, many=True).data)

# --- ADMIN MANAGEMENT: DOCTOR TABLE APIs ---
class ManageDoctorListAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        # Show all approved doctors (both active and disabled) so admin can manage their status
        doctors = Doctor.objects.filter(user__is_approved=True)
        return Response(DoctorSerializer(doctors, many=True).data)

class ManageDoctorDetailAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        try:
            doc = Doctor.objects.get(id=pk)
            return Response(DoctorSerializer(doc).data)
        except Doctor.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)
            
    def delete(self, request, pk):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        try:
            doc = Doctor.objects.get(id=pk)
            user = doc.user
            doc.delete() # Hard delete profile
            if user:
                user.delete() # Hard delete user so counts are real-time
            return Response({"message": "Doctor removed successfully"})
        except Doctor.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)

class ToggleDoctorStatusAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def patch(self, request, pk):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        try:
            doc = Doctor.objects.get(id=pk)
            if doc.status == 'active':
                doc.status = 'disabled'
                doc.is_active = False
            else:
                doc.status = 'active'
                doc.is_active = True
            doc.save()
            
            # Sync with auth_user
            if doc.user:
                doc.user.is_active = (doc.status == 'active')
                doc.user.save()
                
            return Response({"message": "Doctor status updated successfully"})
        except Doctor.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)

# POST /api/admin/doctors/toggle-status/  — accepts {doctor_id, is_active}
class ToggleDoctorStatusByIdAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        doctor_id = request.data.get('doctor_id')
        is_active = request.data.get('is_active')
        if doctor_id is None or is_active is None:
            return Response({"error": "doctor_id and is_active are required"}, status=400)
        try:
            doc = Doctor.objects.get(id=doctor_id)
            doc.status = 'active' if is_active else 'disabled'
            doc.is_active = bool(is_active)
            doc.save()
            
            # Sync with auth user
            if doc.user:
                doc.user.is_active = bool(is_active)
                doc.user.save()
            return Response({"message": "Doctor status updated successfully"})
        except Doctor.DoesNotExist:
            return Response({"error": "Doctor not found"}, status=404)

# --- ADMIN MANAGEMENT: STAFF TABLE APIs ---
class ManageStaffListAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        # Show all approved staff (active + disabled) so admin can manage status
        staff = Staff.objects.filter(user__is_approved=True)
        return Response(StaffSerializer(staff, many=True).data)

class ManageStaffDetailAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        try:
            staff = Staff.objects.get(id=pk)
            return Response(StaffSerializer(staff).data)
        except Staff.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)
            
    def delete(self, request, pk):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        try:
            staff = Staff.objects.get(id=pk)
            user = staff.user
            staff.delete() # Hard delete profile
            if user:
                user.delete() # Hard delete user so counts are real-time
            return Response({"message": "Staff removed successfully"})
        except Staff.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)

class ToggleStaffStatusAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def patch(self, request, pk):
        if request.user.role != 'admin': return Response(status=status.HTTP_403_FORBIDDEN)
        try:
            staff = Staff.objects.get(id=pk)
            if staff.status == 'active':
                staff.status = 'disabled'
            else:
                staff.status = 'active'
            staff.save()
            
            # Sync with auth_user
            if staff.user:
                staff.user.is_active = (staff.status == 'active')
                staff.user.save()
                
            return Response({"message": "Staff status updated successfully"})
        except Staff.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)

# --- ADMIN PANEL: Real-time Dashboard Metrics ---
def generate_ai_recommendation(patient):
    """Helper to generate and save a recommendation if needed."""
    from .ai_utils import get_ai_prediction
    
    # 1. Get latest data
    vitals = Vitals.objects.filter(patient=patient).order_by('-created_at').first()
    abg = ABGData.objects.filter(patient=patient).order_by('-created_at').first()
    
    data = {
        "spo2": vitals.spo2 if vitals else 95,
        "resp_rate": vitals.resp_rate if vitals else 16,
        "heart_rate": vitals.heart_rate if vitals else 75,
        "ph": abg.ph if abg else 7.4,
        "paco2": abg.paco2 if abg else 40,
        "pao2": abg.pao2 if abg else 85,
        "hco3": abg.hco3 if abg else 24
    }
    
    prediction = get_ai_prediction(data)
    rec_content = f"AI recommends {prediction.get('recommended_device')} (Flow: {prediction.get('flow_rate')}) based on latest clinical data."
    
    # Check for existing pending recommendation with same content
    existing = Recommendation.objects.filter(patient=patient, status='pending', content=rec_content).exists()
    if not existing:
        Recommendation.objects.create(
            patient=patient,
            rec_type='therapy',
            content=rec_content,
            status='pending'
        )

class AdminDashboardStatsAPIView(APIView):
    permission_classes = [AllowAny]
    def get(self, request):
        try:
            # 1. Total Doctors from 'doctor' table
            total_doctors = Doctor.objects.filter(status='active').count()
            
            # 2. Total Staff from 'staff' table
            total_staff = Staff.objects.filter(status='active').count()
            
            # 3. Pending Approvals (Users where is_approved=False)
            # Excluding admin to count only registration requests
            pending_requests = CustomUser.objects.filter(
                is_approved=False
            ).exclude(role='admin').count()

            data = {
                "total_doctors": total_doctors,
                "total_staff": total_staff,
                "pending_requests": pending_requests
            }
            print(f"DEBUG Dashboard: {data}")
            return Response(data, status=status.HTTP_200_OK)
        except Exception as e:
            print(f"ERROR Dashboard: {str(e)}")
            return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


# --- AI & DECISION SUPPORT MODULE ---

class AIRiskAPIView(APIView):
    permission_classes = [IsAuthenticated]
    
    def get_data(self, p):
        # Get latest vitals & ABG for prediction
        vitals = Vitals.objects.filter(patient=p).order_by('-created_at').first()
        abg = ABGData.objects.filter(patient=p).order_by('-created_at').first()
        
        data = {}
        if vitals:
            data.update({
                "spo2": vitals.spo2, 
                "resp_rate": vitals.resp_rate, 
                "heart_rate": vitals.heart_rate
            })
        else:
            data.update({"spo2": 95, "resp_rate": 16, "heart_rate": 75})

        if abg:
            data.update({
                "ph": abg.ph, 
                "paco2": abg.paco2, 
                "pao2": abg.pao2, 
                "hco3": abg.hco3
            })
        else:
            data.update({"ph": 7.4, "paco2": 40, "pao2": 85, "hco3": 24})
        return data, vitals, abg

    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            data, vitals, abg = self.get_data(p)
            prediction = get_ai_prediction(data)
            
            # Formulate key factors for the UI
            key_factors = []
            if vitals and vitals.spo2 < 90:
                key_factors.append({"factor": "Low SpO2", "value": f"{vitals.spo2}%", "severity": "high"})
            if abg and abg.paco2 > 45:
                key_factors.append({"factor": "Hypercapnia", "value": f"{abg.paco2} mmHg", "severity": "high"})
            
            return Response({
                "risk_level": prediction.get('risk_level', 'MODERATE'),
                "confidence_score": prediction.get('confidence_score', 75),
                "key_factors": key_factors,
                "recommended_device": prediction.get('recommended_device'),
                "flow_rate": prediction.get('flow_rate')
            })
        except Patient.DoesNotExist:
            return Response(status=404)

    def post(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            data, vitals, abg = self.get_data(p)
            prediction = get_ai_prediction(data)
            
            # Persist the prediction as a recommendation
            Recommendation.objects.create(
                patient=p,
                rec_type='therapy',
                content=f"AI recommends {prediction.get('recommended_device')} (Flow: {prediction.get('flow_rate')}) with {prediction.get('confidence_score')}% confidence based on latest clinical data.",
                status='pending'
            )
            
            return Response({"message": "AI Analysis completed and recommendation saved.", "prediction": prediction})
        except Patient.DoesNotExist:
            return Response(status=404)

class AcceptTherapyAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            # Only Doctors can approve therapy
            if request.user.role != 'doctor':
                return Response({"error": "Only doctors can approve therapy"}, status=403)
            
            rec_id = request.data.get('recommendation_id')
            rec = Recommendation.objects.get(id=rec_id, patient=p)
            
            # Parse device and flow from content if not explicitly stored
            # (In a more robust system, we'd have fields on Recommendation)
            content = rec.content
            # AI recommends [Device] (Flow: [Flow]) ...
            import re
            device_match = re.search(r"AI recommends (.*?) \(Flow: (.*?)\)", content)
            
            if device_match:
                p.current_device = device_match.group(1)
                p.current_flow_rate = device_match.group(2)
            else:
                # Fallback parsing
                p.current_device = content.split("AI recommends ")[1].split(" with")[0]
                p.current_flow_rate = "As recommended"

            p.therapy_approved_at = timezone.now()
            p.save()
            
            rec.status = 'accepted'
            rec.save()
            
            return Response({
                "message": "Therapy approved successfully",
                "current_device": p.current_device,
                "current_flow_rate": p.current_flow_rate
            })
        except (Patient.DoesNotExist, Recommendation.DoesNotExist):
            return Response({"error": "Patient or Recommendation not found"}, status=404)
        except Exception as e:
            return Response({"error": str(e)}, status=500)

class TrendAnalysisAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            vitals = list(Vitals.objects.filter(patient=p).order_by('-created_at')[:2])
            abgs = list(ABGData.objects.filter(patient=p).order_by('-created_at')[:2])
            trends = analyze_trends(vitals, abgs)
            return Response(trends)
        except Patient.DoesNotExist:
            return Response(status=404)

class DecisionSupportAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            # 1. Get Risk Analysis (same logic as AIRiskAPIView.get_data)
            risk_view = AIRiskAPIView()
            analysis_data, vitals, abg = risk_view.get_data(p)
            prediction = get_ai_prediction(analysis_data)
            
            # 2. Trigger recommendation if none exists (or data refreshed)
            generate_ai_recommendation(p)
            
            # 3. Get Recommendations
            recs = Recommendation.objects.filter(patient=p).order_by('-created_at')
            
            return Response({
                "has_data": True if (vitals or abg) else False,
                "risk_level": prediction.get('risk_level', 'MODERATE'),
                "confidence_score": prediction.get('confidence_score', 75),
                "action_level": prediction.get('risk_level', 'WARNING'),
                "recommendation": f"Consider {prediction.get('recommended_device')} at {prediction.get('flow_rate')}.",
                "overall_status": p.status,
                "paco2_status": "Rising" if (abg and abg.paco2 > 45) else "Normal",
                "ph_status": "Dropping" if (abg and abg.ph < 7.35) else "Normal",
                "spo2_status": "Low" if (vitals and vitals.spo2 < 90) else "Stable",
                "acidosis": 1 if (abg and abg.ph < 7.35) else 0,
                "hypercapnia": 1 if (abg and abg.paco2 > 45) else 0,
                "recommendations": RecommendationSerializer(recs, many=True).data
            })
        except Patient.DoesNotExist:
            return Response(status=404)

class ClinicalReviewAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        return Response({"status": "Patient review completed", "next_steps": "Monitor SpO2 hourly"})

class ClinicalTherapyAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        return Response({"therapy_plan": "Continue current oxygen therapy", "last_update": "2 hours ago"})

class ClinicalReassessmentAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, pk):
        return Response({"reassessment_needed": True, "time_left": "45 mins"})

# ─── Direct DB Login Views (read from sandhiya.staff / sandhiya.doctor) ─────

import random
from django.db import connection
from django.contrib.auth.hashers import check_password as dj_check_password
from django.core.mail import send_mail
from django.conf import settings


class StaffDirectLoginAPIView(APIView):
    """
    POST /api/staff/login/
    SELECT * FROM sandhiya.staff WHERE email = <email>;
    """
    permission_classes = [AllowAny]

    def post(self, request):
        email    = request.data.get("email")
        password = request.data.get("password")

        if not email or not password:
            return Response({"error": "Email and password are required"}, status=status.HTTP_400_BAD_REQUEST)

        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT id, name, email, password, is_approved, is_active FROM staff WHERE email = %s LIMIT 1",
                [email]
            )
            row = cursor.fetchone()

        if row is None:
            return Response({"error": "Invalid email or password"}, status=status.HTTP_401_UNAUTHORIZED)

        staff_id, name, staff_email, hashed_pw, is_approved, is_active = row

        if not dj_check_password(password, hashed_pw):
            return Response({"error": "Invalid email or password"}, status=status.HTTP_401_UNAUTHORIZED)

        if not is_approved:
            return Response({"error": "Waiting for admin approval"}, status=status.HTTP_403_FORBIDDEN)

        if not is_active:
            return Response({"error": "Your account is disabled by admin"}, status=status.HTTP_403_FORBIDDEN)

        otp = str(random.randint(100000, 999999))
        with connection.cursor() as cursor:
            cursor.execute(
                "INSERT INTO staff_otp (email, otp, is_used, created_at) VALUES (%s, %s, 0, NOW())",
                [staff_email, otp]
            )

        try:
            send_mail(
                subject="Staff Login OTP",
                message=(
                    f"Dear {name},\n\n"
                    f"Your OTP for login is {otp}.\n"
                    f"It will expire in 5 minutes.\n\n"
                    f"CDSS COPD Team"
                ),
                from_email=settings.EMAIL_HOST_USER,
                recipient_list=[staff_email],
                fail_silently=False,
            )
        except Exception as e:
            return Response({"error": f"OTP generated but email could not be sent: {str(e)}"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        return Response({"message": "Login successful. OTP sent to email", "staff_email": staff_email, "name": name}, status=status.HTTP_200_OK)


class DoctorDirectLoginAPIView(APIView):
    """
    POST /api/doctor/login/
    SELECT * FROM sandhiya.doctor WHERE email = <email>;
    """
    permission_classes = [AllowAny]

    def post(self, request):
        email    = request.data.get("email")
        password = request.data.get("password")

        if not email or not password:
            return Response({"error": "Email and password are required"}, status=status.HTTP_400_BAD_REQUEST)

        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT id, name, email, password, is_approved, is_active FROM doctor WHERE email = %s LIMIT 1",
                [email]
            )
            row = cursor.fetchone()

        if row is None:
            return Response({"error": "Invalid email or password"}, status=status.HTTP_401_UNAUTHORIZED)

        doctor_id, name, doctor_email, hashed_pw, is_approved, is_active = row

        if not dj_check_password(password, hashed_pw):
            return Response({"error": "Invalid email or password"}, status=status.HTTP_401_UNAUTHORIZED)

        if not is_approved:
            return Response({"error": "Waiting for admin approval"}, status=status.HTTP_403_FORBIDDEN)

        if not is_active:
            return Response({"error": "Your account is disabled by admin"}, status=status.HTTP_403_FORBIDDEN)

        otp = str(random.randint(100000, 999999))
        with connection.cursor() as cursor:
            cursor.execute(
                "INSERT INTO doctor_otp (email, otp, is_used, created_at) VALUES (%s, %s, 0, NOW())",
                [doctor_email, otp]
            )

        try:
            send_mail(
                subject="Doctor Login OTP",
                message=(
                    f"Dear Dr. {name},\n\n"
                    f"Your OTP for login is {otp}.\n"
                    f"It will expire in 5 minutes.\n\n"
                    f"CDSS COPD Team"
                ),
                from_email=settings.EMAIL_HOST_USER,
                recipient_list=[doctor_email],
                fail_silently=False,
            )
        except Exception as e:
            return Response({"error": f"OTP generated but email could not be sent: {str(e)}"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        return Response({"message": "OTP sent to registered email", "doctor_email": doctor_email, "name": name}, status=status.HTTP_200_OK)


# ─── Forgot Password & Reset Password (Doctor + Staff) ──────────────────────

import secrets
from django.utils import timezone
from datetime import timedelta
from django.contrib.auth.hashers import make_password
from .serializers import SignupSerializer # Import SignupSerializer here for use in ResetPasswordAPIView


class ForgotPasswordAPIView(APIView):
    """
    POST /api/forgot-password/
    Body: { "email": "..." }
    """
    permission_classes = [AllowAny]

    def post(self, request):
        email = request.data.get("email", "").strip().lower()
        if not email:
            return Response({"error": "Email is required"}, status=status.HTTP_400_BAD_REQUEST)

        user = CustomUser.objects.filter(email=email).first()
        if not user or user.role == 'admin':
            # Same message for security
            return Response({"message": "If that email exists, an OTP has been sent."})

        otp = str(random.randint(100000, 999999))
        expires_at = timezone.now() + timedelta(minutes=10)

        EmailOTP.objects.filter(email=email, purpose='forgot_password').delete()
        EmailOTP.objects.create(
            email=email,
            otp=otp,
            purpose='forgot_password',
            expires_at=expires_at
        )

        if send_otp_email(email, otp, 'password reset'):
            return Response({"message": "If that email exists, an OTP has been sent."})
        else:
            return Response({"error": "Failed to send OTP"}, status=500)

class ResetPasswordAPIView(APIView):
    """
    POST /api/reset-password/
    Body: { "email": "...", "otp": "...", "new_password": "..." }
    """
    permission_classes = [AllowAny]

    def post(self, request):
        email = request.data.get("email")
        otp = request.data.get("otp")
        new_password = request.data.get("new_password")

        if not all([email, otp, new_password]):
            return Response({"error": "Email, OTP and new password are required"}, status=400)

        otp_record = EmailOTP.objects.filter(
            email=email, 
            otp=otp, 
            purpose='forgot_password',
            expires_at__gt=timezone.now()
        ).first()

        if not otp_record:
            return Response({"error": "Invalid or expired OTP"}, status=400)

        user = CustomUser.objects.filter(email=email).first()
        if not user:
            return Response({"error": "User not found"}, status=404)

        # Validate password complexity
        try:
            # We can use the serializer's validation logic
            SignupSerializer().validate_password(new_password)
        except serializers.ValidationError as e:
            return Response({"error": e.detail[0]}, status=400)

        user.set_password(new_password)
        user.save()
        
        otp_record.is_verified = True
        otp_record.save()

        return Response({"message": "Password reset successfully"})

# --- UPDATE PROFILE & OTHER ---


class UpdateProfileAPIView(ProfileAPIView):
    """
    POST /api/update-profile/
    Securely updates profile of current authenticated user.
    """
    def post(self, request, *args, **kwargs):
        # Compatibility for POST: treat as PUT
        return self.put(request, *args, **kwargs)


# --- ADDITIONAL FRONTEND-REQUIRED VIEWS ---

class OxygenStatusAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
        except Patient.DoesNotExist:
            return Response(status=404)

        # Build oxygen status from latest vitals
        latest_vitals = p.vitals.order_by('-created_at').first()
        
        # Check for oxygen requirement record
        oxy_req = p.oxygen_req.order_by('-created_at').first()

        # LOGGING FOR DEBUGGING
        print(f"DEBUG: OxygenStatus for patient {pk}, vitals={latest_vitals}, current_device={p.current_device}")

        if not latest_vitals and not p.current_device:
            return Response({
                'spo2': '--',
                'current_spo2': '--',
                'target_spo2': '88-92',
                'device': 'Not Set',
                'flow_rate': '--',
                'fio2': '--',
                'status': 'no_data'
            })

        spo2 = latest_vitals.spo2 if latest_vitals else '--'
        fio2 = (latest_vitals.fio2 if latest_vitals else None) or 21
        
        status_val = 'stable'
        if isinstance(spo2, int):
            if spo2 < 88: status_val = 'below_target'
            elif spo2 > 92: status_val = 'above_target'
            else: status_val = 'within_target'

        return Response({
            'spo2': spo2,
            'current_spo2': spo2,
            'target_spo2': '88-92',
            'device': p.current_device or ('Nasal Cannula' if oxy_req else 'Not Set'),
            'flow_rate': p.current_flow_rate or (f'{oxy_req.lpm_required} L/min' if oxy_req else '--'),
            'fio2': fio2,
            'status': status_val
        })


class ReassessmentChecklistAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
        except Patient.DoesNotExist:
            return Response(status=404)

        checklists = ReassessmentChecklist.objects.filter(patient=p).order_by('-created_at')
        return Response(ReassessmentChecklistSerializer(checklists, many=True).data)

    def post(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
        except Patient.DoesNotExist:
            return Response(status=404)

        serializer = ReassessmentChecklistSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(patient=p)
            return Response(serializer.data, status=201)
        return Response(serializer.errors, status=400)

class ReassessmentScheduleAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
        except Patient.DoesNotExist:
            return Response(status=404)
        
        schedules = ReassessmentSchedule.objects.filter(patient=p).order_by('-scheduled_at')
        return Response(ReassessmentScheduleSerializer(schedules, many=True).data)

    def post(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
        except Patient.DoesNotExist:
            return Response(status=404)

        data = request.data.copy()
        data['patient'] = p.id
        # Calculate scheduled_at based on interval
        interval = int(data.get('interval_minutes', 60))
        data['scheduled_at'] = timezone.now() + timedelta(minutes=interval)

        serializer = ReassessmentScheduleSerializer(data=data)
        if serializer.is_valid():
            schedule = serializer.save()
            
            # Notify Assigned Staff
            assigned_staff_id = data.get('assigned_staff')
            if assigned_staff_id:
                try:
                    staff_member = Staff.objects.get(id=assigned_staff_id)
                    staff_user = staff_member.user
                    if staff_user:
                        Notification.objects.create(
                            user=staff_user,
                            title='Reassessment Scheduled',
                            message=f'Dr. {request.user.username} scheduled a clinical reassessment for {p.full_name} in {interval} minutes.'
                        )
                except (Staff.DoesNotExist, Exception):
                    pass
                    
            return Response(serializer.data, status=201)
        return Response(serializer.errors, status=400)

class StaffListAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        # Doctors need to list staff to assign reassessments
        staff = Staff.objects.filter(status='active')
        return Response(StaffSerializer(staff, many=True).data)

class AcceptTherapyAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
            rec_id = request.data.get('recommendation_id')
            rec = Recommendation.objects.get(id=rec_id, patient=p)
            
            # Logic to extract device and flow from recommendation content
            # Or use specific fields if added to Recommendation model.
            # For now, we assume simple status update and patient field updates.
            rec.status = 'accepted'
            
            # If selected_device was provided (doctor selection/override)
            selected_device = request.data.get('selected_device')
            flow_rate = request.data.get('flow_rate')
            is_override = request.data.get('is_override', False)
            override_reason = request.data.get('override_reason')
            
            if selected_device:
                p.current_device = selected_device
                p.current_flow_rate = flow_rate or p.current_flow_rate
                if is_override:
                    rec.status = 'overridden'
                    rec.override_reason = override_reason
            else:
                # Fallback to old regex/string matching if not explicitly provided
                if 'HFNC' in rec.content:
                    p.current_device = 'High-Flow Nasal Cannula (HFNC)'
                    p.current_flow_rate = '40L/min, 40%'
                elif 'Venturi' in rec.content:
                    p.current_device = 'Venturi Mask'
                    p.current_flow_rate = '35%'
                elif 'Nasal Cannula' in rec.content:
                    p.current_device = 'Nasal Cannula'
                    p.current_flow_rate = '2L/min'
                elif 'Non-Rebreather' in rec.content:
                    p.current_device = 'Non-Rebreather Mask'
                    p.current_flow_rate = '15L/min'
            
            rec.save()
            p.therapy_approved_at = timezone.now()
            p.save()
            
            Notification.objects.create(
                user=request.user,
                title="Therapy Approved",
                message=f"Therapy for {p.full_name} set to {p.current_device}."
            )
            
            return Response({"message": "Therapy approved and applied."})
        except (Patient.DoesNotExist, Recommendation.DoesNotExist):
            return Response(status=404)


class ABGTrendsAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            p = Patient.objects.get(pk=pk)
        except Patient.DoesNotExist:
            return Response(status=404)

        # Get records from both ABGData and Vitals
        abg_records = ABGData.objects.filter(patient=p).order_by('created_at')
        vitals_records = Vitals.objects.filter(patient=p).order_by('created_at')
        
        trends = []
        # Merge and sort by time
        for r in abg_records:
            trends.append({
                "timestamp": r.created_at,
                "ph": r.ph,
                "paco2": r.paco2,
                "pao2": r.pao2,
                "hco3": r.hco3,
                "type": "abg"
            })
        
        # Add SpO2 from vitals to the trend
        for v in vitals_records:
            # Simple matching or just include all
            trends.append({
                "timestamp": v.created_at,
                "spo2": v.spo2,
                "hr": v.heart_rate,
                "type": "vitals"
            })
            
        trends.sort(key=lambda x: x['timestamp'])
        return Response({"trends": trends})
        # Add ABG records
        for a in abg_records:
            trends.append({
                'type': 'abg',
                'timestamp': a.created_at.isoformat(),
                'ph': a.ph,
                'paco2': a.paco2,
                'pao2': a.pao2,
                'hco3': a.hco3,
                'spo2': None # Will try to match from vitals if possible
            })
            
        # Add Vitals records (primarily for SpO2 trends)
        for v in vitals_records:
            # Check if we already have a record around this time (to avoid duplicates)
            # For simplicity, we just add them all and let frontend handle sorting/merging
            trends.append({
                'type': 'vitals',
                'timestamp': v.created_at.isoformat(),
                'ph': None,
                'paco2': None,
                'pao2': None,
                'hco3': None,
                'fio2': v.fio2 or 21,
                'spo2': v.spo2,
                'hr': v.heart_rate,
                'rr': v.resp_rate
            })
            
        # Sort combined trends by timestamp
        trends.sort(key=lambda x: x['timestamp'])
        
        return Response({
            'trends': trends,
            'count': len(trends)
        })

class AlertUpdateAPIView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request, pk):
        try:
            alert = Alert.objects.get(pk=pk)
            action = request.data.get('action') # 'complete', 'dismiss', 'read'
            if action == 'complete' or action == 'read' or action == 'dismiss':
                alert.is_read = True
                alert.save()
            return Response({"message": f"Alert {action} marked successfully."})
        except Alert.DoesNotExist:
            return Response({"error": "Alert not found"}, status=404)
