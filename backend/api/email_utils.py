from django.core.mail import send_mail
from django.conf import settings

def send_otp_email(email, otp, purpose):
    subject = f"Your CDSS COPD {purpose.capitalize()} Verification Code"
    message = f"Your verification code is: {otp}. This code will expire in 10 minutes."
    from_email = settings.DEFAULT_FROM_EMAIL
    recipient_list = [email]
    
    try:
        send_mail(subject, message, from_email, recipient_list)
        return True
    except Exception as e:
        print(f"Error sending email: {e}")
        return False
