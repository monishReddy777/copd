import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPatients } from '../../api/patients';
import { getStaffAlerts } from '../../api/alerts';
import { HeartPulse, Clock, Activity, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const StaffDashboard = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState([]);
  const [stats, setStats] = useState({ patient_count: 0, pending_vitals: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPatients();
  }, []);

  const fetchPatients = async () => {
    try {
      const patientsRes = await getPatients();
      const patientsList = patientsRes.data?.results || patientsRes.data || [];
      setPatients(patientsList.slice(0, 5));
      setStats({
        patient_count: patientsList.length,
        pending_vitals: patientsList.filter(p => p.status === 'warning' || p.status === 'critical').length,
      });
    } catch (error) {
      toast.error('Failed to load pending tasks');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Staff Dashboard</h1>
          <p>Your shift overview and pending patient reassessments</p>
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card" onClick={() => navigate('/staff/patients')} style={{ cursor: 'pointer' }}>
          <div className="stat-card-icon blue">
            <Activity size={24} />
          </div>
          <div className="stat-card-value">{stats.patient_count}</div>
          <div className="stat-card-label">Patients in Ward</div>
        </div>

        <div className="stat-card" onClick={() => navigate('/staff/patients')}>
          <div className="stat-card-icon orange">
            <Clock size={24} />
          </div>
          <div className="stat-card-value">{stats.pending_vitals}</div>
          <div className="stat-card-label">Pending Vitals</div>
        </div>

        <div className="stat-card" onClick={() => navigate('/staff/alerts')}>
          <div className="stat-card-icon purple">
            <AlertCircle size={24} />
          </div>
          <div className="stat-card-value" id="staff-alerts-count">—</div>
          <div className="stat-card-label">Active Alerts</div>
        </div>
      </div>

      <div className="data-grid">
        <div className="card" style={{ gridColumn: 'span 2' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px' }}>Task List: Pending Reassessments</h3>
          
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Admit Date</th>
                  <th>Patient Name</th>
                  <th>Location</th>
                  <th>Task</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {patients.length === 0 ? (
                  <tr><td colSpan="5" style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>No patients assigned</td></tr>
                ) : patients.map(patient => (
                  <tr key={patient.id}>
                    <td>
                      <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>
                        {patient.created_at ? new Date(patient.created_at).toLocaleDateString() : 'N/A'}
                      </span>
                    </td>
                    <td><div style={{ fontWeight: 600 }}>{patient.full_name}</div></td>
                    <td>{patient.ward} • {patient.bed_number}</td>
                    <td>
                      <span className="badge" style={{ background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}>
                        Vitals Entry
                      </span>
                    </td>
                    <td>
                      <button 
                        className="btn btn-sm btn-primary" 
                        onClick={() => navigate(`/patients/${patient.id}?tab=vitals`)}
                      >
                        Enter Vitals
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px' }}>Quick Actions</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <button className="btn btn-outline" style={{ justifyContent: 'flex-start' }} onClick={() => navigate('/staff/patients')}>
              <Activity size={18} /> View All Patients
            </button>
            <button className="btn btn-outline" style={{ justifyContent: 'flex-start' }} onClick={() => navigate('/staff/alerts')}>
              <AlertCircle size={18} /> Shift Alerts
            </button>
          </div>
        </div>
      </div>
    </>
  );
};

export default StaffDashboard;
