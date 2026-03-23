import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Stethoscope, HeartPulse, Shield, Activity } from 'lucide-react';

const RoleSelect = () => {
  const navigate = useNavigate();

  return (
    <div className="auth-wrapper">
      <div className="page-container" style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        
        <div className="auth-logo" style={{ marginBottom: '40px' }}>
          <div className="auth-logo-icon">
            <Activity size={28} color="#fff" />
          </div>
          <h1 style={{ fontSize: '2rem' }}>CDSS <span>COPD</span></h1>
        </div>

        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <h2 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '8px' }}>Select Your Role</h2>
          <p style={{ color: 'var(--text-secondary)' }}>Choose your profile to access the portal</p>
        </div>

        <div className="role-grid">
          
          <div className="role-card" onClick={() => navigate('/login/doctor')} style={{ position: 'relative', zIndex: 10, cursor: 'pointer' }}>
            <div className="role-card-icon" style={{ background: 'var(--status-info-bg)', color: 'var(--accent-primary)' }}>
              <Stethoscope size={32} />
            </div>
            <h3>Doctor</h3>
            <p>Access patient cases, AI analysis, and manage therapy recommendations.</p>
          </div>

          <div className="role-card" onClick={() => navigate('/login/staff')} style={{ position: 'relative', zIndex: 10, cursor: 'pointer' }}>
            <div className="role-card-icon" style={{ background: 'var(--status-stable-bg)', color: 'var(--status-stable)' }}>
              <HeartPulse size={32} />
            </div>
            <h3>Staff</h3>
            <p>Enter patient vitals, ABG data, and monitor real-time alerts.</p>
          </div>

          <div className="role-card" onClick={() => navigate('/login/admin')} style={{ position: 'relative', zIndex: 10, cursor: 'pointer' }}>
            <div className="role-card-icon" style={{ background: 'var(--accent-purple-glow)', color: 'var(--accent-purple)' }}>
              <Shield size={32} />
            </div>
            <h3>Admin</h3>
            <p>Manage users, handle approval requests, and view system statistics.</p>
          </div>

        </div>
      </div>
    </div>
  );
};

export default RoleSelect;
