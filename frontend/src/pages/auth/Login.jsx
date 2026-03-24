import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { doctorLogin, staffLogin, adminLogin, verifyEmailOTP } from '../../api/auth';
import { Activity, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';

const Login = () => {
  const { role: urlRole } = useParams();
  const navigate = useNavigate();
  const { login, user } = useAuth();
  
  // Default to doctor if no role provided or invalid role
  const [role, setRole] = useState(urlRole || 'doctor');
  const [formData, setFormData] = useState({ email: '', password: '', otp: '' });
  const [loading, setLoading] = useState(false);
  const [showOtpInput, setShowOtpInput] = useState(false);

  // If already logged in, redirect to appropriate dashboard
  useEffect(() => {
    if (user) {
      if (role === 'admin') navigate('/admin/dashboard');
      else if (role === 'doctor') navigate('/doctor/dashboard');
      else if (role === 'staff') navigate('/staff/dashboard');
    }
  }, [user, role, navigate]);

  // Ensure role is valid
  useEffect(() => {
    if (urlRole && !['doctor', 'staff', 'admin'].includes(urlRole)) {
      navigate('/login/doctor', { replace: true });
    } else if (urlRole) {
      setRole(urlRole);
    }
  }, [urlRole, navigate]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      let response;
      if (role === 'doctor') {
        response = await doctorLogin(formData);
      } else if (role === 'staff') {
        response = await staffLogin(formData);
      } else if (role === 'admin') {
        response = await adminLogin(formData);
      }

      if (response.data?.otp_required) {
        setShowOtpInput(true);
        toast.success(response.data.message || 'OTP sent to your email');
        return;
      }

      if (response.data?.access || response.data?.token) {
        completeLogin(response.data);
      } else {
        toast.error('Invalid response from server');
      }
    } catch (error) {
      console.error('Login error:', error);
      toast.error(error.response?.data?.error || error.response?.data?.detail || error.response?.data?.message || 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    if (!formData.otp) {
      toast.error('Please enter the OTP');
      return;
    }
    setLoading(true);
    try {
      const response = await verifyEmailOTP({ 
        email: formData.email, 
        otp: formData.otp, 
        purpose: 'login' 
      });
      
      if (response.data?.access) {
        completeLogin(response.data);
      } else {
        toast.error('Invalid OTP or verification failed');
      }
    } catch (error) {
      toast.error(error.response?.data?.error || 'Verification failed');
    } finally {
      setLoading(false);
    }
  };

  const completeLogin = (data) => {
    const token = data.access || data.token;
    const userData = data.user || data.doctor || data.staff || data.admin || { name: 'User', email: formData.email };
    login(token, userData, role);
    toast.success(`Welcome back, ${userData.name || 'User'}!`);
    navigate(`/${role}/dashboard`);
  };

  const handleRoleChange = (newRole) => {
    setRole(newRole);
    navigate(`/login/${newRole}`, { replace: true });
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        
        <button 
          onClick={() => navigate('/')} 
          className="btn btn-ghost"
          style={{ position: 'absolute', top: '16px', left: '16px', padding: '8px' }}
        >
          <ArrowLeft size={20} />
        </button>

        <div className="auth-logo">
          <div className="auth-logo-icon">
            <Activity size={24} color="#fff" />
          </div>
          <h1>CDSS <span>COPD</span></h1>
        </div>

        <h2 className="auth-title">Welcome Back</h2>
        <p className="auth-subtitle">Sign in to your account</p>

        <div className="tabs" style={{ marginBottom: '28px' }}>
          <button 
            className={`tab ${role === 'doctor' ? 'active' : ''}`}
            onClick={() => handleRoleChange('doctor')}
            style={{ flex: 1 }}
          >
            Doctor
          </button>
          <button 
            className={`tab ${role === 'staff' ? 'active' : ''}`}
            onClick={() => handleRoleChange('staff')}
            style={{ flex: 1 }}
          >
            Staff
          </button>
          <button 
            className={`tab ${role === 'admin' ? 'active' : ''}`}
            onClick={() => handleRoleChange('admin')}
            style={{ flex: 1 }}
          >
            Admin
          </button>
        </div>

        <form onSubmit={showOtpInput ? handleOtpSubmit : handleSubmit}>
          {!showOtpInput ? (
            <>
              <div className="form-group">
                <label className="form-label">Email Address</label>
                <input 
                  type="email" 
                  name="email"
                  className="form-input" 
                  placeholder={`Enter your ${role} email`}
                  value={formData.email}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <label className="form-label" style={{ marginBottom: 0 }}>Password</label>
                  {role !== 'admin' && (
                    <Link to="/forgot-password" style={{ fontSize: '0.8125rem', fontWeight: 500 }}>
                      Forgot password?
                    </Link>
                  )}
                </div>
                <input 
                  type="password" 
                  name="password"
                  className="form-input" 
                  placeholder="Enter your password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                />
              </div>
            </>
          ) : (
            <div className="form-group animate-in">
              <label className="form-label">Enter OTP Sent to {formData.email}</label>
              <input 
                type="text" 
                name="otp"
                className="form-input" 
                placeholder="6-digit code"
                value={formData.otp}
                onChange={handleChange}
                maxLength={6}
                required
                autoFocus
              />
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '8px' }}>
                Didn't receive code? <button type="button" onClick={() => setShowOtpInput(false)} className="btn-link">Go back</button>
              </p>
            </div>
          )}

          <button 
            type="submit" 
            className="btn btn-primary btn-block" 
            style={{ marginTop: '32px' }}
            disabled={loading}
          >
            {loading ? (
              <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span className="spinner" style={{ width: '16px', height: '16px', borderWidth: '2px' }}></span> 
                {showOtpInput ? 'Verifying...' : 'Signing in...'}
              </span>
            ) : (showOtpInput ? 'Verify & Login' : 'Sign In')}
          </button>
        </form>

        {role !== 'admin' && (
          <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Don't have an account? <Link to="/signup" style={{ fontWeight: 600 }}>Create an account</Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default Login;
