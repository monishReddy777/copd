import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'CDSS_COPD.settings')
django.setup()

from django.contrib.auth import get_user_model
User = get_user_model()

try:
    user1 = User.objects.get(email='monishgv28@gmail.com')
    user1.set_password('Monish')
    user1.is_active = True
    user1.save()
    print('monishgv28@gmail.com password reset to Monish and activated.')
except Exception as e:
    print('Failed user1:', e)

try:
    user2 = User.objects.get(email='doctor@test.com')
    user2.set_password('doctor123')
    user2.is_active = True
    user2.save()
    print('doctor@test.com password reset to doctor123 and activated.')
except Exception as e:
    print('Failed user2:', e)
