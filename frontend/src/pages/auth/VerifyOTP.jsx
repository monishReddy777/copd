import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { doctorVerifyOTP, staffVerifyOTP, doctorForgotPassword, staffForgotPassword } from '../../api/auth';
import { KeyRound, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';

const VerifyOTP = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { email, role } = location.state || { email: '', role: 'doctor' };
  
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);

  useEffect(() => {
    if (!email) {
      navigate('/forgot-password');
    }
  }, [email, navigate]);

  const handleChange = (element, index) => {
    if (isNaN(element.value)) return;
    
    // Auto advance focus
    const newOtp = [...otp];
    newOtp[index] = element.value;
    setOtp(newOtp);

    if (element.nextSibling && element.value) {
      element.nextSibling.focus();
    }
  };

  const handleKeyDown = (e, index) => {
    if (e.key === 'Backspace' && !otp[index] && e.target.previousSibling) {
      e.target.previousSibling.focus();
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const otpString = otp.join('');
    
    if (otpString.length !== 6) {
      toast.error('Please enter a valid 6-digit OTP');
      return;
    }

    setLoading(true);
    try {
      let response;
      if (role === 'doctor') {
        response = await doctorVerifyOTP({ email, otp: otpString });
      } else {
        response = await staffVerifyOTP({ email, otp: otpString });
      }
      
      // Navigate to reset password page with token
      navigate('/reset-password', { 
        state: { 
          email, 
          role, 
          token: response.data.reset_token || response.data.token 
        } 
      });
      toast.success('OTP verified successfully');
    } catch (error) {
      toast.error(error.response?.data?.error || 'Invalid OTP. Please try again.');
      setOtp(['', '', '', '', '', '']); // Clear
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setResending(true);
    try {
      if (role === 'doctor') {
        await doctorForgotPassword({ email });
      } else {
        await staffForgotPassword({ email });
      }
      toast.success('OTP resent to your email');
    } catch (error) {
      toast.error('Failed to resend OTP');
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        
        <button 
          onClick={() => navigate('/forgot-password', { state: { role } })} 
          className="btn btn-ghost"
          style={{ position: 'absolute', top: '16px', left: '16px', padding: '8px' }}
        >
          <ArrowLeft size={20} />
        </button>

        <div className="auth-logo">
          <div className="auth-logo-icon" style={{ background: 'var(--bg-secondary)', color: 'var(--text-primary)', boxShadow: 'none', border: '1px solid var(--border)' }}>
            <KeyRound size={24} />
          </div>
        </div>

        <h2 className="auth-title">Verify OTP</h2>
        <p className="auth-subtitle">Enter the 6-digit code sent to <br/><strong style={{ color: 'var(--text-primary)' }}>{email}</strong></p>

        <form onSubmit={handleSubmit}>
          
          <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', marginBottom: '32px' }}>
            {otp.map((data, index) => (
              <input
                key={index}
                type="text"
                maxLength="1"
                value={data}
                className="form-input"
                style={{ width: '45px', height: '50px', textAlign: 'center', fontSize: '1.25rem', fontWeight: 600, padding: 0 }}
                onChange={e => handleChange(e.target, index)}
                onKeyDown={e => handleKeyDown(e, index)}
                onFocus={e => e.target.select()}
                required
              />
            ))}
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Verifying...' : 'Verify OTP'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          Didn't receive the code?{' '}
          <button 
            type="button" 
            onClick={handleResend} 
            disabled={resending}
            style={{ background: 'none', border: 'none', color: 'var(--accent-primary)', fontWeight: 600, cursor: 'pointer', padding: 0 }}
          >
            {resending ? 'Resending...' : 'Resend'}
          </button>
        </div>

      </div>
    </div>
  );
};

export default VerifyOTP;
