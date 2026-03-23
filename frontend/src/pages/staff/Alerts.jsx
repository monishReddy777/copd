import React, { useState, useEffect } from 'react';
import { getStaffAlerts, updateAlert } from '../../api/alerts';
import { AlertCircle, AlertTriangle, Info, Clock } from 'lucide-react';
import toast from 'react-hot-toast';

const StaffAlerts = () => {
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
        { id: 1, alert_type: 'Reassessment Due', message: 'Vitals needed for Robert Fox (ICU bed A-12)', severity: 'warning', created_at: new Date().toISOString() },
        { id: 2, alert_type: 'Action Required', message: 'ABG draw requested by Dr. Smith for Jane Cooper', severity: 'critical', created_at: new Date(Date.now() - 1800000).toISOString() },
        { id: 3, alert_type: 'System Note', message: 'Oxygen tank replacement required in Ward B', severity: 'info', created_at: new Date(Date.now() - 7200000).toISOString() }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleAlertAction = async (id, action) => {
    try {
      await updateAlert(id, action);
      toast.success(`Alert marked as ${action}`);
      setAlerts(alerts.filter(a => a.id !== id));
    } catch (error) {
      toast.error('Failed to update alert');
    }
  };

  const getAlertIcon = (severity) => {
    switch(severity) {
      case 'critical': return <AlertCircle size={20} />;
      case 'warning': return <AlertTriangle size={20} />;
      default: return <Info size={20} />;
    }
  };

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1>Shift Alerts</h1>
          <p>Tasks, notifications, and reminders for your shift</p>
        </div>
      </div>

      {alerts.length === 0 ? (
        <div className="card" style={{ padding: '60px' }}>
          <div className="empty-state">
            <AlertCircle className="empty-state-icon" style={{ color: 'var(--status-stable)' }} />
            <h3>No pending alerts</h3>
            <p>You are all caught up on your tasks.</p>
          </div>
        </div>
      ) : (
        <div>
          {alerts.map(alert => (
            <div key={alert.id} className={`alert-card ${alert.severity}`}>
              <div className={`alert-icon ${alert.severity === 'critical' ? 'red' : alert.severity === 'warning' ? 'orange' : 'blue'}`}
                   style={{ 
                     background: alert.severity === 'critical' ? 'var(--status-critical-bg)' : 
                                 alert.severity === 'warning' ? 'var(--status-warning-bg)' : 
                                 'var(--status-info-bg)',
                     color: alert.severity === 'critical' ? 'var(--status-critical)' : 
                            alert.severity === 'warning' ? 'var(--status-warning)' : 
                            'var(--status-info)'
                   }}
              >
                {getAlertIcon(alert.severity)}
              </div>
              <div className="alert-content" style={{ flex: 1 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <h4>{alert.alert_type}</h4>
                  <span className="alert-time" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Clock size={12} />
                    {new Date(alert.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
                <p>{alert.message}</p>
                <div style={{ marginTop: '12px', display: 'flex', gap: '8px' }}>
                  <button className="btn btn-sm btn-primary" onClick={() => handleAlertAction(alert.id, 'complete')}>Mark Complete</button>
                  <button className="btn btn-sm btn-outline" onClick={() => handleAlertAction(alert.id, 'dismiss')}>Dismiss</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default StaffAlerts;
