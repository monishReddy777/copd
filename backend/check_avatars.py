import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'backend.settings')
django.setup()

from api.models import CustomUser

print("Checking CustomUser model...")
for user in CustomUser.objects.all():
    print(f"User: {user.username}, Role: {user.role}, Profile Image: {user.profile_image.name if user.profile_image else 'None'}")
