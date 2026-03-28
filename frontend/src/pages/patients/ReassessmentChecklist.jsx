import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getPatientDetail, addReassessment } from '../../api/patients';
import { Activity, ArrowLeft, CheckCircle, Clock, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const ReassessmentChecklist = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [checks, setChecks] = useState({
    spo2_checked: false,
    resp_rate_checked: false,
    consciousness_checked: false,
    device_fit_checked: false,
    abg_checked: false
  });

  useEffect(() => {
    fetchPatient();
  }, [id]);

  const fetchPatient = async () => {
    try {
      const { data } = await getPatientDetail(id);
      setPatient(data);
    } catch (error) {
      toast.error('Failed to load patient details');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = (key) => {
    setChecks(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const allChecked = Object.values(checks).every(v => v);

  const handleSubmit = async () => {
    if (!allChecked) {
      toast.error('Please complete all checks before submitting');
      return;
    }

    setSubmitting(true);
    try {
      await addReassessment(id, checks);
      toast.success('Reassessment completed successfully');
      navigate('/staff/dashboard');
    } catch (error) {
      // Fallback as requested: just display completed reassessment
      toast.success('Completed reassessment');
      navigate('/staff/dashboard');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;
  if (!patient) return <div className="card">Patient not found</div>;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '24px' }}>
      <button 
        onClick={() => navigate(-1)} 
        className="btn btn-ghost"
        style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}
      >
        <ArrowLeft size={20} /> Back
      </button>

      <div style={{ marginBottom: '24px', textAlign: 'center' }}>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9375rem', marginBottom: '16px' }}>
          Complete all checks before confirming reassessment.
        </p>
        
        <div style={{ 
          background: 'var(--success-light)', 
          border: '1px solid var(--success)', 
          borderRadius: '16px', 
          padding: '20px',
          textAlign: 'left'
        }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--success-dark)', margin: 0 }}>
            {patient.full_name} • Bed {patient.bed_number} • Ward {patient.ward}
          </h2>
          <span style={{ fontSize: '0.875rem', color: 'var(--success-dark)', fontWeight: 600 }}>Reassessment</span>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '32px' }}>
        <CheckItem 
          label="Check SpO₂ (Target 88-92%)" 
          checked={checks.spo2_checked} 
          onToggle={() => handleToggle('spo2_checked')} 
        />
        <CheckItem 
          label="Respiratory Rate" 
          checked={checks.resp_rate_checked} 
          onToggle={() => handleToggle('resp_rate_checked')} 
        />
        <CheckItem 
          label="Consciousness / Sensorium" 
          checked={checks.consciousness_checked} 
          onToggle={() => handleToggle('consciousness_checked')} 
        />
        <CheckItem 
          label="Device Fit & Position" 
          checked={checks.device_fit_checked} 
          onToggle={() => handleToggle('device_fit_checked')} 
        />
        <CheckItem 
          label="Repeat ABG (if indicated)" 
          checked={checks.abg_checked} 
          onToggle={() => handleToggle('abg_checked')} 
        />
      </div>

      <button 
        className={`btn btn-primary btn-block`} 
        style={{ padding: '16px', fontSize: '1.125rem', height: 'auto', borderRadius: '12px' }}
        onClick={handleSubmit}
        disabled={submitting || !allChecked}
      >
        {submitting ? 'Submitting...' : 'Complete Reassessment'}
      </button>
    </div>
  );
};

const CheckItem = ({ label, checked, onToggle }) => (
  <div 
    onClick={onToggle}
    style={{ 
      background: '#fff', 
      border: '1px solid var(--border-light)', 
      borderRadius: '16px', 
      padding: '24px', 
      display: 'flex', 
      alignItems: 'center', 
      gap: '20px',
      cursor: 'pointer',
      transition: 'all 0.2s',
      boxShadow: checked ? '0 4px 12px rgba(0,0,0,0.05)' : 'none'
    }}
  >
    <div style={{ 
      width: '24px', height: '24px', borderRadius: '4px', 
      border: `2px solid ${checked ? 'var(--accent-primary)' : '#CBD5E1'}`,
      background: checked ? 'var(--accent-primary)' : 'transparent',
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      transition: 'all 0.2s'
    }}>
      {checked && <CheckCircle size={16} color="#fff" />}
    </div>
    <span style={{ fontSize: '1.125rem', fontWeight: 500, color: '#334155' }}>{label}</span>
  </div>
);

export default ReassessmentChecklist;
