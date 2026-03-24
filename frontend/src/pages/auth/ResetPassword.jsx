import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { resetPassword } from '../../api/auth';
import { Lock } from 'lucide-react';
import toast from 'react-hot-toast';

const ResetPassword = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { email, role, otp } = location.state || {};
  
  const [formData, setFormData] = useState({
    new_password: '',
    confirm_password: ''
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!email || !otp) {
      toast.error('Invalid reset session. Please start over.');
      navigate('/forgot-password');
    }
  }, [email, otp, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (formData.new_password !== formData.confirm_password) {
      toast.error('Passwords do not match');
      return;
    }

    if (formData.new_password.length < 8) {
      toast.error('Password must be at least 8 characters and meet complexity requirements');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        email,
        otp,
        new_password: formData.new_password
      };

      await resetPassword(payload);
      
      toast.success('Password reset successfully! Please login.');
      navigate(`/login/${role}`);
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to reset password. OTP may have expired.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">

        <div className="auth-logo">
          <div className="auth-logo-icon" style={{ background: 'var(--status-stable-bg)', color: 'var(--status-stable)', boxShadow: 'none', border: '1px solid var(--border)' }}>
            <Lock size={24} />
          </div>
        </div>

        <h2 className="auth-title">Create New Password</h2>
        <p className="auth-subtitle">Enter your new secure password below</p>

        <form onSubmit={handleSubmit}>
          
          <div className="form-group">
            <label className="form-label">New Password</label>
            <input 
              type="password" 
              className="form-input" 
              placeholder="••••••••"
              value={formData.new_password}
              onChange={(e) => setFormData({...formData, new_password: e.target.value})}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Confirm Password</label>
            <input 
              type="password" 
              className="form-input" 
              placeholder="••••••••"
              value={formData.confirm_password}
              onChange={(e) => setFormData({...formData, confirm_password: e.target.value})}
              required
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading} style={{ marginTop: '32px' }}>
            {loading ? 'Resetting...' : 'Reset Password'}
          </button>
        </form>

      </div>
    </div>
  );
};

export default ResetPassword;
