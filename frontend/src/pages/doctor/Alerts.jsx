import React, { useState, useEffect } from 'react';
import { getDoctorAlerts, updateAlert } from '../../api/alerts';
import { AlertCircle, AlertTriangle, Info, Clock } from 'lucide-react';
import toast from 'react-hot-toast';

const DoctorAlerts = () => {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all'); // all, critical, warning, info

  useEffect(() => {
    fetchAlerts();
  }, []);

  const fetchAlerts = async () => {
    try {
      const { data } = await getDoctorAlerts();
      setAlerts(data.results || data);
    } catch (error) {
      toast.error('Failed to load alerts');
      setAlerts([
        { id: 1, alert_type: 'SpO2 Drop', message: 'Patient Robert Fox SpO2 dropped below 88%', severity: 'critical', created_at: new Date().toISOString() },
        { id: 2, alert_type: 'Escalation', message: 'Jane Cooper meets escalation criteria for NIV', severity: 'warning', created_at: new Date(Date.now() - 3600000).toISOString() },
        { id: 3, alert_type: 'Reassessment Due', message: 'ABG reassessment due for Guy Hawkins in 15 mins', severity: 'info', created_at: new Date(Date.now() - 7200000).toISOString() }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleAlertAction = async (id, action) => {
    try {
      await updateAlert(id, action);
      toast.success(`Alert ${action}ed`);
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

  const filteredAlerts = filter === 'all' ? alerts : alerts.filter(a => a.severity === filter);

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1>Clinical Alerts</h1>
          <p>Real-time notifications for patient status changes</p>
        </div>
      </div>

      <div className="page-header" style={{ marginBottom: '24px' }}>
        <div>
          <h1>Clinical Alerts</h1>
          <p>Real-time notifications for patient status changes</p>
        </div>
      </div>

      {loading ? (
        <div className="loader-container"><div className="spinner"></div></div>
      ) : filteredAlerts.length === 0 ? (
        <div className="card" style={{ padding: '60px' }}>
          <div className="empty-state">
            <AlertCircle className="empty-state-icon" style={{ color: 'var(--status-stable)' }} />
            <h3>No alerts found</h3>
            <p>There are no {filter !== 'all' ? filter : ''} clinical alerts at this time.</p>
          </div>
        </div>
      ) : (
        <div>
          {filteredAlerts.map(alert => (
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
                  {alert.severity === 'critical' && (
                    <button className="btn btn-sm btn-primary" 
                      style={{ background: 'var(--status-critical)', boxShadow: '0 0 20px rgba(239, 68, 68, 0.3)' }}
                      onClick={() => handleAlertAction(alert.id, 'action')}
                    >
                      Take Action
                    </button>
                  )}
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

export default DoctorAlerts;
