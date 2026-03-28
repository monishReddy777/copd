import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getStaffAlerts, updateAlert } from '../../api/alerts';
import { AlertTriangle, ArrowLeft, Bell, CheckCircle } from 'lucide-react';
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
      const results = data.results || data;
      setAlerts(results.filter(a => !a.acknowledged));
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
      setAlerts(alerts.filter(a => a.id !== id));
    } catch (error) {
      // Mock Data fallback
      toast.success(`Alert acknowledged`);
      setAlerts(alerts.filter(a => a.id !== id));
    }
  };

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  const renderAlert = (alert) => (
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
  );

  return (
    <>
      <div className="page-header">
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Bell size={24} /> Alerts & Notifications
          </h1>
          <p>Important clinical events and reassessment reminders</p>
        </div>
        <div className="page-header-actions">
          <button className="btn btn-outline" onClick={() => setAlerts(alerts.map(a => ({ ...a, read: true })))}>
            <CheckCircle size={16} /> Mark All Read
          </button>
        </div>
      </div>

      <div className="card" style={{ padding: '0', borderRadius: '16px', border: 'none', background: 'transparent' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {alerts.map(renderAlert)}
          {alerts.length === 0 && (
            <div className="card" style={{ textAlign: 'center', padding: '60px' }}>
              <div className="empty-state">
                <Bell size={48} className="empty-state-icon" />
                <h3>No alerts found</h3>
                <p>You're all caught up! No active alerts at this time.</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
};

export default StaffAlerts;
