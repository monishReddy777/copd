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
    <div style={{ maxWidth: '600px', margin: '0 auto', background: '#F8F9FA', minHeight: '100vh', paddingBottom: '80px', fontFamily: 'Inter, sans-serif' }}>
      
      {/* Header */}
      <div style={{ padding: '32px 24px 16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 'bold', color: '#1a1a1a', margin: 0 }}>CLINICAL STAFF</h1>
          <p style={{ color: '#666', fontSize: '14px', margin: 0, marginTop: '4px' }}>{user?.name || 'Staff Member'}</p>
        </div>
        <div 
          onClick={() => navigate('/staff/alerts')}
          style={{ 
            width: '44px', height: '44px', background: '#fff', borderRadius: '22px', 
            display: 'flex', justifyContent: 'center', alignItems: 'center', 
            boxShadow: '0 2px 8px rgba(0,0,0,0.05)', position: 'relative', cursor: 'pointer' 
          }}
        >
          <Bell size={20} color="#333" />
          {alertCount > 0 && (
            <div style={{ 
              position: 'absolute', top: '10px', right: '10px', width: '8px', height: '8px', 
              background: '#EF4444', borderRadius: '50%' 
            }} />
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div style={{ padding: '24px 24px 8px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: 'bold', color: '#1a1a1a', margin: 0 }}>Quick Actions</h2>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', padding: '0 16px' }}>
        
        {/* Add Patient */}
        <div onClick={() => navigate('/staff/patients/add')} style={{ background: '#fff', borderRadius: '24px', padding: '24px 16px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.04)', cursor: 'pointer' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '28px', background: '#F0F9F8', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
            <Plus size={24} color="#0D9488" />
          </div>
          <span style={{ fontWeight: 'bold', color: '#333', fontSize: '14px' }}>Add Patient</span>
        </div>

        {/* Enter Vitals */}
        <div onClick={() => navigate('/staff/patients')} style={{ background: '#fff', borderRadius: '24px', padding: '24px 16px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.04)', cursor: 'pointer' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '28px', background: '#EFF6FF', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
            <HeartPulse size={24} color="#3B82F6" />
          </div>
          <span style={{ fontWeight: 'bold', color: '#333', fontSize: '14px' }}>Enter Vitals</span>
        </div>

        {/* Symptoms */}
        <div onClick={() => navigate('/staff/patients')} style={{ background: '#fff', borderRadius: '24px', padding: '24px 16px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.04)', cursor: 'pointer' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '28px', background: '#F5F3FF', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
            <Activity size={24} color="#8B5CF6" />
          </div>
          <span style={{ fontWeight: 'bold', color: '#333', fontSize: '14px' }}>Symptoms</span>
        </div>

        {/* Enter ABG */}
        <div onClick={() => navigate('/staff/patients')} style={{ background: '#fff', borderRadius: '24px', padding: '24px 16px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.04)', cursor: 'pointer' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '28px', background: '#FFFBEB', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
            <FileText size={24} color="#D97706" />
          </div>
          <span style={{ fontWeight: 'bold', color: '#333', fontSize: '14px' }}>Enter ABG</span>
        </div>

      </div>

      {/* Reassessment Due */}
      <div style={{ padding: '32px 24px 16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 'bold', color: '#1a1a1a', margin: 0 }}>Reassessment Due</h2>
          <div style={{ background: '#FEF2F2', color: '#EF4444', padding: '4px 12px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold' }}>
            {filteredPatients.length} Due
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          <Filter size={16} color="#666" />
          <select style={{ border: 'none', background: 'transparent', color: '#666', fontSize: '14px', outline: 'none', cursor: 'pointer' }} value={filter} onChange={e => setFilter(e.target.value)}>
            <option value="All">All</option>
            <option value="Critical">Critical</option>
            <option value="Warning">Warning</option>
            <option value="Stable">Stable</option>
          </select>
        </div>
      </div>

      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {filteredPatients.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '32px 16px', color: '#888', fontSize: '14px' }}>
            No pending reassessments
          </div>
        ) : filteredPatients.map(patient => (
          <div key={patient.id} 
               onClick={() => navigate(`/patients/${patient.id}?tab=vitals`)}
               style={{ background: '#fff', borderRadius: '20px', padding: '16px', display: 'flex', alignItems: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.04)', cursor: 'pointer' }}>
            
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: '#FFFBEB', display: 'flex', justifyContent: 'center', alignItems: 'center', flexShrink: 0 }}>
              <Clock size={24} color="#D97706" />
            </div>
            
            <div style={{ marginLeft: '16px', flex: 1 }}>
              <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold', color: '#333' }}>Vitals / ABG Check Due</h3>
              <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: '#666' }}>{patient.full_name} • {patient.ward} {patient.bed_number}</p>
              <p style={{ margin: '4px 0 0 0', fontSize: '12px', fontWeight: 'bold', color: '#EF4444' }}>
                {patient.status === 'critical' ? 'Immediate Action' : 'Due in 30 mins'}
              </p>
            </div>
          </div>
        ))}
      </div>

    </div>
  );
};

export default StaffDashboard;
