import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'CDSS_COPD.settings')
django.setup()

from django.contrib.auth import get_user_model
from api.models import Staff

User = get_user_model()

try:
    user = User.objects.get(email='staff@test.com')
    user.set_password('staff123')
    user.is_active = True
    user.role = 'staff'
    user.save()
    print('User updated successfully.')

    staff_profile, _ = Staff.objects.get_or_create(user=user, defaults={'email': user.email, 'name': 'Test Staff'})
    staff_profile.is_approved = True
    staff_profile.is_active = True
    staff_profile.save()
    print('Staff profile approved successfully.')
except Exception as e:
    print('Error:', e)
