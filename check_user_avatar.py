import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'backend.settings')
django.setup()

from api.models import CustomUser

user = CustomUser.objects.filter(email='admin@gmail.com').first()
if user:
    print(f"User: {user.username}, Email: {user.email}, Profile Image: {user.profile_image.name if user.profile_image else 'None'}")
else:
    print("User admin@gmail.com not found")
