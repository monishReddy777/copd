import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPatients } from '../../api/patients';
import { getStaffAlerts } from '../../api/alerts';
import { Bell, Plus, HeartPulse, Activity, FileText, Filter, Clock } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../../hooks/useAuth';

const StaffDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [patients, setPatients] = useState([]);
  const [filter, setFilter] = useState('All');
  const [loading, setLoading] = useState(true);
  const [alertCount, setAlertCount] = useState(0);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [patientsRes, alertsRes] = await Promise.all([
        getPatients(),
        getStaffAlerts()
      ]);
      const patientsList = patientsRes.data?.results || patientsRes.data || [];
      setPatients(patientsList);
      
      const alertsList = alertsRes.data?.results || alertsRes.data || [];
      setAlertCount(alertsList.length);
    } catch (error) {
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const filteredPatients = patients.filter(p => filter === 'All' || (p.status || '').toLowerCase() === filter.toLowerCase());

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', background: '#F8F9FA', minHeight: '100vh', padding: '24px', paddingBottom: '80px', fontFamily: 'Inter, sans-serif' }}>
      
      {/* Header */}
      <div style={{ padding: '24px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontSize: '28px', fontWeight: 'bold', color: '#1a1a1a', margin: 0 }}>CLINICAL STAFF DASHBOARD</h1>
          <p style={{ color: '#666', fontSize: '16px', margin: 0, marginTop: '4px' }}>Welcome back, {user?.name || 'Staff Member'}</p>
        </div>
        <div 
          onClick={() => navigate('/staff/alerts')}
          style={{ 
            width: '48px', height: '48px', background: '#fff', borderRadius: '12px', 
            display: 'flex', justifyContent: 'center', alignItems: 'center', 
            boxShadow: '0 4px 12px rgba(0,0,0,0.08)', position: 'relative', cursor: 'pointer' 
          }}
        >
          <Bell size={24} color="#333" />
          {alertCount > 0 && (
            <div style={{ 
              position: 'absolute', top: '12px', right: '12px', width: '10px', height: '10px', 
              background: '#EF4444', border: '2px solid #fff', borderRadius: '50%' 
            }} />
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div style={{ marginBottom: '40px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 'bold', color: '#1a1a1a', marginBottom: '20px' }}>Quick Actions</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '20px' }}>
          
          {/* Add Patient */}
          <div onClick={() => navigate('/staff/patients/add')} style={{ background: '#fff', borderRadius: '20px', padding: '32px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 4px 12px rgba(0,0,0,0.05)', cursor: 'pointer', transition: 'transform 0.2s' }}>
            <div style={{ width: '64px', height: '64px', borderRadius: '16px', background: '#F0F9F8', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
              <Plus size={28} color="#0D9488" />
            </div>
            <span style={{ fontWeight: 'bold', color: '#333', fontSize: '16px' }}>Add New Patient</span>
          </div>

          {/* Enter Vitals */}
          <div onClick={() => navigate('/staff/patients')} style={{ background: '#fff', borderRadius: '20px', padding: '32px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 4px 12px rgba(0,0,0,0.05)', cursor: 'pointer', transition: 'transform 0.2s' }}>
            <div style={{ width: '64px', height: '64px', borderRadius: '16px', background: '#EFF6FF', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
              <HeartPulse size={28} color="#3B82F6" />
            </div>
            <span style={{ fontWeight: 'bold', color: '#333', fontSize: '16px' }}>Record Vitals</span>
          </div>

          {/* Symptoms */}
          <div onClick={() => navigate('/staff/patients')} style={{ background: '#fff', borderRadius: '20px', padding: '32px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 4px 12px rgba(0,0,0,0.05)', cursor: 'pointer', transition: 'transform 0.2s' }}>
            <div style={{ width: '64px', height: '64px', borderRadius: '16px', background: '#F5F3FF', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
              <Activity size={28} color="#8B5CF6" />
            </div>
            <span style={{ fontWeight: 'bold', color: '#333', fontSize: '16px' }}>Symptoms Entry</span>
          </div>

          {/* Enter ABG */}
          <div onClick={() => navigate('/staff/patients')} style={{ background: '#fff', borderRadius: '20px', padding: '32px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 4px 12px rgba(0,0,0,0.05)', cursor: 'pointer', transition: 'transform 0.2s' }}>
            <div style={{ width: '64px', height: '64px', borderRadius: '16px', background: '#FFFBEB', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
              <FileText size={28} color="#D97706" />
            </div>
            <span style={{ fontWeight: 'bold', color: '#333', fontSize: '16px' }}>ABG Analysis</span>
          </div>

        </div>
      </div>

      {/* Reassessment Due */}
      <div style={{ background: '#fff', borderRadius: '24px', padding: '32px', boxShadow: '0 4px 20px rgba(0,0,0,0.04)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: 'bold', color: '#1a1a1a', margin: 0 }}>Pending Reassessments</h2>
            <div style={{ background: '#FEF2F2', color: '#EF4444', padding: '6px 14px', borderRadius: '12px', fontSize: '14px', fontWeight: 'bold' }}>
              {filteredPatients.length} Active
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', background: '#F3F4F6', padding: '8px 16px', borderRadius: '12px' }}>
            <Filter size={18} color="#666" />
            <select style={{ border: 'none', background: 'transparent', color: '#333', fontSize: '14px', fontWeight: 500, outline: 'none', cursor: 'pointer' }} value={filter} onChange={e => setFilter(e.target.value)}>
              <option value="All">All Statuses</option>
              <option value="Critical">Critical</option>
              <option value="Warning">Warning</option>
              <option value="Stable">Stable</option>
            </select>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '16px' }}>
          {filteredPatients.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '48px 16px', color: '#888', fontSize: '16px', gridColumn: '1 / -1' }}>
              No pending reassessments at this time.
            </div>
          ) : filteredPatients.map(patient => (
            <div key={patient.id} 
                 onClick={() => navigate(`/patients/${patient.id}?tab=vitals`)}
                 style={{ 
                   background: '#F9FAFB', borderRadius: '16px', padding: '20px', 
                   display: 'flex', alignItems: 'center', border: '1px solid #F3F4FB',
                   cursor: 'pointer', transition: 'all 0.2s' 
                 }}>
              
              <div style={{ width: '56px', height: '56px', borderRadius: '14px', background: '#fff', display: 'flex', justifyContent: 'center', alignItems: 'center', flexShrink: 0, boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
                <Clock size={28} color="#D97706" />
              </div>
              
              <div style={{ marginLeft: '20px', flex: 1 }}>
                <h3 style={{ margin: 0, fontSize: '17px', fontWeight: 'bold', color: '#333' }}>Clinical Check Due</h3>
                <p style={{ margin: '4px 0 0 0', fontSize: '15px', color: '#666' }}>{patient.full_name}</p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
                  <span style={{ fontSize: '13px', color: '#888' }}>{patient.ward} • Bed {patient.bed_number}</span>
                  <span style={{ fontSize: '13px', fontWeight: 'bold', color: '#EF4444' }}>
                    {patient.status === 'critical' ? 'Urgent Action' : 'Due Soon'}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
};

export default StaffDashboard;
