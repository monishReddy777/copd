import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { getPatients } from '../../api/patients';
import { getDoctorAlerts } from '../../api/alerts';
import { Users, AlertCircle, Activity, HeartPulse } from 'lucide-react';
import toast from 'react-hot-toast';

const DoctorDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [stats, setStats] = useState({ patient_count: 0, critical_alerts: 0, pending_reassessments: 0 });
  const [recentPatients, setRecentPatients] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [patientsRes, alertsRes] = await Promise.all([
        getPatients().catch(() => ({ data: [] })),
        getDoctorAlerts().catch(() => ({ data: [] }))
      ]);
      
      const patientsList = patientsRes.data?.results || patientsRes.data || [];
      
      // Calculate status based on latest SpO2
      const processedPatients = patientsList.map(p => {
        const spo2 = p.latest_vitals?.spo2;
        let pStatus = p.status || 'stable';
        
        if (spo2 !== undefined && spo2 !== null) {
          if (spo2 < 80) pStatus = 'critical';
          else if (spo2 < 88) pStatus = 'warning';
          else pStatus = 'stable';
        }
        return { ...p, displayStatus: pStatus, spo2 };
      });

      const criticalCount = processedPatients.filter(p => p.displayStatus === 'critical').length;
      const warningCount = processedPatients.filter(p => p.displayStatus === 'warning').length;
      
      setStats({ 
        patient_count: patientsList.length, 
        critical_alerts: criticalCount,
        warning_patients: warningCount
      });

      // Filter for those needing attention, fallback to recent if none
      const needingAttention = processedPatients.filter(p => p.displayStatus !== 'stable');
      setRecentPatients(needingAttention.length > 0 ? needingAttention.slice(0, 5) : processedPatients.slice(0, 5));
    } catch (error) {
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'critical': return <span className="badge badge-critical">Critical</span>;
      case 'warning': return <span className="badge badge-warning">Warning</span>;
      default: return <span className="badge badge-stable">Stable</span>;
    }
  };

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Doctor Dashboard</h1>
          <p>Welcome, {user?.name || 'Doctor'}</p>
        </div>
        <div className="page-header-actions">
          {/* Add patient removed for doctors */}
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card" onClick={() => navigate('/doctor/patients')} style={{ cursor: 'pointer' }}>
          <div className="stat-card-icon blue">
            <Users size={24} />
          </div>
          <div className="stat-card-value">{stats.patient_count}</div>
          <div className="stat-card-label">My Patients</div>
        </div>

        <div className="stat-card" onClick={() => navigate('/doctor/alerts')} style={{ cursor: 'pointer' }}>
          <div className="stat-card-icon red">
            <AlertCircle size={24} />
          </div>
          <div className="stat-card-value">{stats.critical_alerts}</div>
          <div className="stat-card-label">Critical Alerts</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon orange">
            <Activity size={24} />
          </div>
          <div className="stat-card-value">{stats.warning_patients}</div>
          <div className="stat-card-label">Patients with Warning</div>
        </div>
      </div>

      <div className="data-grid">
        <div className="card" style={{ gridColumn: 'span 2' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600 }}>Patients Needing Attention</h3>
            <button className="btn btn-ghost" onClick={() => navigate('/doctor/patients')}>View All</button>
          </div>
          
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Patient Name</th>
                  <th>Clinical Status</th>
                </tr>
              </thead>
              <tbody>
                {recentPatients.map(patient => (
                  <tr key={patient.id} onClick={() => navigate(`/patients/${patient.id}`)} style={{ cursor: 'pointer' }}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{patient.full_name}</div>
                      {patient.spo2 !== undefined && patient.spo2 !== null && (
                        <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '2px' }}>
                          SpO2: <span style={{ fontWeight: 600, color: patient.displayStatus === 'critical' ? 'var(--status-critical)' : patient.displayStatus === 'warning' ? 'var(--status-warning)' : 'var(--text-primary)' }}>{patient.spo2}%</span>
                        </div>
                      )}
                    </td>
                    <td>{getStatusBadge(patient.displayStatus)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px' }}>Quick Actions</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <button className="btn btn-outline" style={{ justifyContent: 'flex-start', color: 'var(--status-critical)', borderColor: 'rgba(239, 68, 68, 0.3)', background: 'rgba(239, 68, 68, 0.05)' }} onClick={() => navigate('/doctor/alerts')}>
              <AlertCircle size={18} /> Review Critical Alerts
            </button>
            <a href="/settings/guidelines" className="btn btn-outline" style={{ justifyContent: 'flex-start' }}>
              <HeartPulse size={18} /> COPD Clinical Guidelines
            </a>
          </div>
        </div>
      </div>
    </>
  );
};

export default DoctorDashboard;
