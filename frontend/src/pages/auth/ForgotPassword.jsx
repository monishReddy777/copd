import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { doctorForgotPassword, staffForgotPassword } from '../../api/auth';
import { Mail, ArrowLeft, Activity } from 'lucide-react';
import toast from 'react-hot-toast';

const ForgotPassword = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const defaultRole = location.state?.role || 'doctor';
  
  const [role, setRole] = useState(defaultRole);
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (role === 'doctor') {
        await doctorForgotPassword({ email });
      } else {
        await staffForgotPassword({ email });
      }
      
      toast.success('OTP sent to your email');
      navigate('/verify-otp', { state: { email, role } });
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to send OTP. Account may not exist.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        
        <button 
          onClick={() => navigate(-1)} 
          className="btn btn-ghost"
          style={{ position: 'absolute', top: '16px', left: '16px', padding: '8px' }}
        >
          <ArrowLeft size={20} />
        </button>

        <div className="auth-logo">
          <div className="auth-logo-icon" style={{ background: 'var(--bg-secondary)', color: 'var(--text-primary)', boxShadow: 'none', border: '1px solid var(--border)' }}>
            <Mail size={24} />
          </div>
        </div>

        <h2 className="auth-title">Reset Password</h2>
        <p className="auth-subtitle">Enter your email to receive an OTP</p>

        <div className="tabs" style={{ marginBottom: '24px' }}>
          <button className={`tab ${role === 'doctor' ? 'active' : ''}`} onClick={() => setRole('doctor')} style={{ flex: 1 }}>Doctor</button>
          <button className={`tab ${role === 'staff' ? 'active' : ''}`} onClick={() => setRole('staff')} style={{ flex: 1 }}>Staff</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input 
              type="email" 
              className="form-input" 
              placeholder={`Enter your ${role} email`}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading || !email}>
            {loading ? 'Sending OTP...' : 'Send OTP'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          Remember your password? <Link to={`/login/${role}`} style={{ fontWeight: 600 }}>Sign in</Link>
        </div>

      </div>
    </div>
  );
};

export default ForgotPassword;
