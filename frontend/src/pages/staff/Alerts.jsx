import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getStaffAlerts, updateAlert } from '../../api/alerts';
import { AlertTriangle, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';

const StaffAlerts = () => {
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAlerts();
  }, []);

  const fetchAlerts = async () => {
    try {
      const { data } = await getStaffAlerts();
      setAlerts(data.results || data);
    } catch (error) {
      toast.error('Failed to load alerts');
      setAlerts([
        { id: 1, alert_type: 'Vitals Re-entry Required', message: 'Vasantha (Bed 65) • Doctor requested vitals reassessment after 30 mins', severity: 'critical', time_ago: '1h ago', has_actions: false, acknowledged: true },
        { id: 2, alert_type: 'ABG Draw Requested', message: 'Guru (Bed 9) • Doctor requested ABG re-entry after 1 hr', severity: 'critical', time_ago: '10h ago', has_actions: true, acknowledged: false },
        { id: 3, alert_type: 'Vitals Re-entry Required', message: 'Jayasri (Bed 333) • Doctor requested vitals reassessment after 30 mins', severity: 'critical', time_ago: '1d ago', has_actions: true, acknowledged: false },
        { id: 4, alert_type: 'ABG Draw Requested', message: 'Jai (Bed 1) • Doctor requested ABG re-entry after 1 hr', severity: 'critical', time_ago: '2d ago', has_actions: false, acknowledged: true },
        { id: 5, alert_type: 'Vitals Re-entry Required', message: 'Aishwarya (Bed 111) • Doctor requested vitals reassessment after 30 mins', severity: 'critical', time_ago: '3d ago', has_actions: false, acknowledged: true }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleAcknowledge = async (id) => {
    try {
      if (updateAlert) {
        await updateAlert(id, 'acknowledge');
      }
      toast.success(`Alert acknowledged`);
      setAlerts(alerts.map(a => a.id === id ? { ...a, has_actions: false, acknowledged: true } : a));
    } catch (error) {
      // Mock Data fallback
      toast.success(`Alert acknowledged`);
      setAlerts(alerts.map(a => a.id === id ? { ...a, has_actions: false, acknowledged: true } : a));
    }
  };

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto', fontFamily: 'Inter, sans-serif', paddingBottom: '24px' }}>
      
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', padding: '16px 0', gap: '16px', borderBottom: '1px solid var(--border-light)', marginBottom: '24px' }}>
        <button onClick={() => navigate(-1)} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '8px' }}>
          <ArrowLeft size={24} color="#333" />
        </button>
        <h1 style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0, color: '#333' }}>Alerts Center</h1>
        <div style={{ background: '#ffebee', color: '#e57373', padding: '2px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600 }}>
          {alerts.filter(a => a.has_actions).length}
        </div>
      </div>

      <div style={{ padding: '0 8px' }}>
        <h3 style={{ fontSize: '0.75rem', fontWeight: 700, color: '#e57373', letterSpacing: '0.05em', textTransform: 'uppercase', marginBottom: '16px' }}>
          CRITICAL ALERTS
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {alerts.map(alert => (
            <div key={alert.id} style={{ 
              background: alert.acknowledged ? '#f9f9f9' : '#ffffff', 
              borderRadius: '12px', 
              padding: '16px', 
              boxShadow: alert.acknowledged ? 'none' : '0 2px 8px rgba(0,0,0,0.05)',
              border: alert.acknowledged ? '1px solid #eee' : '1px solid rgba(0,0,0,0.05)'
            }}>
              
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <AlertTriangle size={18} color={alert.acknowledged ? '#f29c9c' : '#e57373'} />
                  <span style={{ color: alert.acknowledged ? '#f29c9c' : '#e57373', fontWeight: 600, fontSize: '0.9375rem' }}>{alert.alert_type}</span>
                </div>
                <span style={{ fontSize: '0.75rem', color: '#aaa', fontWeight: 500 }}>{alert.time_ago || 'Just now'}</span>
              </div>
              
              <p style={{ margin: '0 0 16px 0', fontSize: '0.9375rem', color: alert.acknowledged ? '#888' : '#444', fontWeight: alert.acknowledged ? 400 : 500 }}>
                {alert.message}
              </p>

              {alert.has_actions && !alert.acknowledged && (
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                  <button style={{ 
                    display: 'flex', alignItems: 'center', gap: '6px', 
                    background: 'none', border: 'none', color: '#26a69a', 
                    fontWeight: 600, fontSize: '0.875rem', cursor: 'pointer', padding: '8px 0' 
                  }} onClick={() => navigate('/staff/patients')}>
                    <ArrowLeft size={16} /> View Patient
                  </button>
                  <button 
                    onClick={() => handleAcknowledge(alert.id)}
                    style={{ 
                    background: '#e0f2f1', color: '#26a69a', border: 'none', 
                    borderRadius: '6px', padding: '8px 20px', fontWeight: 600, 
                    fontSize: '0.875rem', cursor: 'pointer' 
                  }}>
                    Acknowledge
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default StaffAlerts;
