"""
COPD CDSS - Database Seeder
============================
Run this script AFTER running migrations to populate the database
with default users and sample patient data.

Usage:
    python seed_database.py
"""
import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'backend.settings')
django.setup()

from api.models import CustomUser, Doctor, Staff, Patient
from django.utils import timezone
from datetime import date

print("=" * 50)
print("  COPD CDSS - Database Seeder")
print("=" * 50)

# ─── 1. Create Admin User ───────────────────────────────
if not CustomUser.objects.filter(email='admin@cdss.com').exists():
    admin = CustomUser.objects.create_superuser('admin_user', 'admin@cdss.com', 'admin123')
    admin.role = 'admin'
    admin.is_approved = True
    admin.save()
    print("[+] Admin user created:   admin@cdss.com / admin123")
else:
    print("[=] Admin user already exists")

# ─── 2. Create Doctor User ──────────────────────────────
if not CustomUser.objects.filter(email='doctor@test.com').exists():
    doctor_auth = CustomUser.objects.create_user('doctor_test', 'doctor@test.com', 'doctor123')
    doctor_auth.role = 'doctor'
    doctor_auth.is_approved = True
    doctor_auth.is_active = True
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
    print("[+] Doctor user created:  doctor@test.com / doctor123")
else:
    print("[=] Doctor user already exists")

# ─── 3. Create Staff User ───────────────────────────────
if not CustomUser.objects.filter(email='staff@test.com').exists():
    staff_auth = CustomUser.objects.create_user('staff_test', 'staff@test.com', 'staff123')
    staff_auth.role = 'staff'
    staff_auth.is_approved = True
    staff_auth.is_active = True
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
    print("[+] Staff user created:   staff@test.com / staff123")
else:
    print("[=] Staff user already exists")

# ─── 4. Create Sample Patient ───────────────────────────
doctor_user = CustomUser.objects.filter(role='doctor', is_approved=True).first()
staff_user = CustomUser.objects.filter(role='staff', is_approved=True).first()

if not Patient.objects.filter(full_name='MONISH G V').exists():
    Patient.objects.create(
        full_name='MONISH G V',
        dob=date(2017, 6, 6),
        sex='Male',
        ward='icu',
        bed_number='32',
        status='warning',
        assigned_doctor=doctor_user,
        created_by=staff_user
    )
    print("[+] Sample patient created: MONISH G V (ICU, Bed 32)")
else:
    print("[=] Sample patient already exists")

if not Patient.objects.filter(full_name='John Doe').exists():
    Patient.objects.create(
        full_name='John Doe',
        dob=date(1965, 3, 15),
        sex='Male',
        ward='respiratory',
        bed_number='12',
        status='stable',
        assigned_doctor=doctor_user,
        created_by=staff_user
    )
    print("[+] Sample patient created: John Doe (Respiratory, Bed 12)")
else:
    print("[=] Sample patient John Doe already exists")

print("")
print("=" * 50)
print("  Database seeding complete!")
print("=" * 50)
print("")
print("  Login Credentials:")
print("  ─────────────────────────────────────")
print("  Admin:   admin@cdss.com    / admin123")
print("  Doctor:  doctor@test.com   / doctor123")
print("  Staff:   staff@test.com    / staff123")
print("  ─────────────────────────────────────")
print("")
