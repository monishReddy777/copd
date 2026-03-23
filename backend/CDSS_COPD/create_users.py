import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'backend.settings')
django.setup()

from api.models import CustomUser, Doctor, Staff

# Create Admin User
if not CustomUser.objects.filter(email='admin@cdss.com').exists():
    admin = CustomUser.objects.create_superuser('admin_user', 'admin@cdss.com', 'admin123')
    admin.role = 'admin'
    admin.is_approved = True
    admin.save()
    print("Admin user created: admin@cdss.com / admin123")

# Create Doctor User
if not CustomUser.objects.filter(email='doctor@test.com').exists():
    doctor_auth = CustomUser.objects.create_user('doctor_test', 'doctor@test.com', 'doctor123')
    doctor_auth.role = 'doctor'
    doctor_auth.is_approved = True
    doctor_auth.save()
    Doctor.objects.create(
        user=doctor_auth,
        name='Dr. Smith',
        email='doctor@test.com',
        specialization='Pulmonology',
        license_number='DOC-001',
        phone='555-0101',
        status='approved',
        is_active=True
    )
    print("Doctor user created: doctor@test.com / doctor123")

# Create Staff User
if not CustomUser.objects.filter(email='staff@test.com').exists():
    staff_auth = CustomUser.objects.create_user('staff_test', 'staff@test.com', 'staff123')
    staff_auth.role = 'staff'
    staff_auth.is_approved = True
    staff_auth.save()
    Staff.objects.create(
        user=staff_auth,
        name='Nurse Jane',
        email='staff@test.com',
        department='ICU',
        license_id='STF-001',
        phone='555-0102',
        status='active'
    )
    print("Staff user created: staff@test.com / staff123")
