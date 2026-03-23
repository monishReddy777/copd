import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { doctorSignup, staffSignup } from '../../api/auth';
import { Activity, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';

const Signup = () => {
  const navigate = useNavigate();
  const [role, setRole] = useState('doctor');
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    phone_number: '',
    specialization: '', // Doctor
    license_number: '', // Doctor
    department: '', // Staff
    staff_id: '', // Staff
    terms_accepted: false
  });

  const handleChange = (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData({ ...formData, [e.target.name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (formData.password !== formData.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }

    if (!formData.terms_accepted) {
      toast.error('Please accept the Terms & Conditions');
      return;
    }

    setLoading(true);
    try {
      const payload = { ...formData, role };
      if (role === 'doctor') {
        await doctorSignup(payload);
        toast.success('Registration successful. NOTE: Your account requires Admin approval.');
      } else {
        await staffSignup(payload);
        toast.success('Registration successful. NOTE: Your account requires Admin approval.');
      }
      navigate(`/login/${role}`);
    } catch (error) {
      toast.error(error.response?.data?.error || error.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper" style={{ padding: '40px 0' }}>
      <div className="auth-card wide">
        
        <button 
          onClick={() => navigate(-1)} 
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

        <h2 className="auth-title">Create an Account</h2>
        <p className="auth-subtitle">Join the CDSS COPD platform</p>

        <div className="tabs" style={{ marginBottom: '24px' }}>
          <button 
            className={`tab ${role === 'doctor' ? 'active' : ''}`}
            onClick={() => setRole('doctor')}
            style={{ flex: 1 }}
          >
            Doctor
          </button>
          <button 
            className={`tab ${role === 'staff' ? 'active' : ''}`}
            onClick={() => setRole('staff')}
            style={{ flex: 1 }}
          >
            Staff
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Full Name</label>
              <input 
                type="text" name="name" className="form-input" 
                placeholder="Dr. John Doe" value={formData.name} onChange={handleChange} required 
              />
            </div>
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input 
                type="email" name="email" className="form-input" 
                placeholder="john@hospital.com" value={formData.email} onChange={handleChange} required 
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Phone Number</label>
            <input 
              type="text" name="phone_number" className="form-input" 
              placeholder="+1 (555) 000-0000" value={formData.phone_number} onChange={handleChange} 
            />
          </div>

          {role === 'doctor' && (
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Specialization</label>
                <input 
                  type="text" name="specialization" className="form-input" 
                  placeholder="Pulmonology" value={formData.specialization} onChange={handleChange} required 
                />
              </div>
              <div className="form-group">
                <label className="form-label">License Number</label>
                <input 
                  type="text" name="license_number" className="form-input" 
                  placeholder="LIC-12345" value={formData.license_number} onChange={handleChange} required 
                />
              </div>
            </div>
          )}

          {role === 'staff' && (
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Department</label>
                <input 
                  type="text" name="department" className="form-input" 
                  placeholder="ICU / Ward B" value={formData.department} onChange={handleChange} required 
                />
              </div>
              <div className="form-group">
                <label className="form-label">Staff ID</label>
                <input 
                  type="text" name="staff_id" className="form-input" 
                  placeholder="STF-12345" value={formData.staff_id} onChange={handleChange} required 
                />
              </div>
            </div>
          )}

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Password</label>
              <input 
                type="password" name="password" className="form-input" 
                placeholder="••••••••" value={formData.password} onChange={handleChange} required 
              />
            </div>
            <div className="form-group">
              <label className="form-label">Confirm Password</label>
              <input 
                type="password" name="confirmPassword" className="form-input" 
                placeholder="••••••••" value={formData.confirmPassword} onChange={handleChange} required 
              />
            </div>
          </div>

          <label className="form-checkbox-group" style={{ marginBottom: '24px' }}>
            <input 
              type="checkbox" name="terms_accepted" 
              checked={formData.terms_accepted} onChange={handleChange}
            />
            <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              I agree to the Terms & Conditions and Privacy Policy
            </span>
          </label>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Submitting...' : 'Sign Up'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          Already have an account? <Link to={`/login/${role}`} style={{ fontWeight: 600 }}>Sign in</Link>
        </div>

      </div>
    </div>
  );
};

export default Signup;
