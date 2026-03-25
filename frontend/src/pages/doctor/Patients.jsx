import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPatients } from '../../api/patients';
import { Search, HeartPulse, Filter, Clock, Trash2 } from 'lucide-react';
import api from '../../api/axios';
import toast from 'react-hot-toast';

const DoctorPatients = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');

  useEffect(() => {
    fetchPatients();
  }, []);

  const fetchPatients = async () => {
    try {
      const { data } = await getPatients();
      setPatients(data.results || data);
    } catch (error) {
      toast.error('Failed to load patients list');
      setPatients([
        { id: 1, full_name: 'Robert Fox', ward: 'ICU', bed_number: 'A-12', sex: 'Male', dob: '1954-05-12', status: 'critical', updated_at: new Date().toISOString() },
        { id: 2, full_name: 'Jane Cooper', ward: 'Ward B', bed_number: 'B-04', sex: 'Female', dob: '1962-11-23', status: 'warning', updated_at: new Date().toISOString() },
        { id: 3, full_name: 'Guy Hawkins', ward: 'Ward A', bed_number: 'A-01', sex: 'Male', dob: '1950-02-15', status: 'stable', updated_at: new Date().toISOString() },
        { id: 4, full_name: 'Esther Howard', ward: 'Ward B', bed_number: 'B-02', sex: 'Female', dob: '1958-08-30', status: 'stable', updated_at: new Date().toISOString() }
      ]);
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

  const handleDeletePatient = async (e, id) => {
    e.stopPropagation();
    if (window.confirm('Are you sure you want to remove this patient?')) {
      try {
        await api.delete(`/patients/${id}/`);
        toast.success('Patient removed successfully');
        fetchPatients();
      } catch (error) {
        toast.error('Failed to remove patient');
      }
    }
  };

  const filteredPatients = patients.filter(patient => {
    const matchesSearch = patient.full_name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          patient.ward.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = filterStatus === 'all' || patient.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Patients</h1>
          <p>Monitor and manage your assigned COPD patients</p>
        </div>
        <div className="page-header-actions">
          {/* Add patient removed */}
        </div>
      </div>

      <div className="card" style={{ marginBottom: '24px', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
        <div className="search-box" style={{ flex: 1, minWidth: '250px' }}>
          <Search className="search-icon" />
          <input 
            type="text" 
            placeholder="Search by name or ward..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        
        <div style={{ display: 'flex', gap: '12px' }}>
          <select 
            className="form-select" 
            style={{ width: '180px' }}
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
          >
            <option value="all">All Statuses</option>
            <option value="critical">Critical Only</option>
            <option value="warning">Warning Only</option>
            <option value="stable">Stable Only</option>
          </select>
          <button className="btn btn-outline" style={{ padding: '12px' }}><Filter size={18} /></button>
        </div>
      </div>

      {loading ? (
        <div className="loader-container"><div className="spinner"></div></div>
      ) : filteredPatients.length === 0 ? (
        <div className="card" style={{ padding: '60px' }}>
          <div className="empty-state">
            <HeartPulse className="empty-state-icon" />
            <h3>No patients found</h3>
            <p>Try adjusting your search criteria or add a new patient.</p>
          </div>
        </div>
      ) : (
        <div className="data-grid">
          {filteredPatients.map(patient => {
            const age = new Date().getFullYear() - new Date(patient.dob || '1960-01-01').getFullYear();
            
            return (
              <div 
                key={patient.id} 
                className="patient-card"
                onClick={() => navigate(`/patients/${patient.id}`)}
              >
                <div className="patient-card-header">
                  <div className="patient-card-name">{patient.full_name}</div>
                  {getStatusBadge(patient.status)}
                </div>
                
                <div className="patient-card-meta" style={{ marginBottom: '16px' }}>
                  <span>{patient.sex} • {age} yrs</span>
                  <span>{patient.ward} - Bed {patient.bed_number}</span>
                </div>
                
                <div style={{ display: 'flex', gap: '8px', borderTop: '1px solid var(--border-light)', paddingTop: '16px' }}>
                  <button className="btn btn-sm btn-outline" style={{ flex: 1 }} onClick={(e) => { e.stopPropagation(); navigate(`/patients/${patient.id}?tab=vitals`); }}>
                    Vitals
                  </button>
                  <button className="btn btn-sm btn-outline" style={{ flex: 1 }} onClick={(e) => { e.stopPropagation(); navigate(`/patients/${patient.id}?tab=abg`); }}>
                    ABG
                  </button>
                  <button className="btn btn-sm btn-primary" style={{ flex: 1 }} onClick={(e) => { e.stopPropagation(); navigate(`/patients/${patient.id}?tab=therapy`); }}>
                    Therapy
                  </button>
                  <button className="btn btn-sm btn-outline" style={{ padding: '0 8px', color: 'var(--status-critical)', borderColor: 'rgba(239, 68, 68, 0.3)' }} onClick={(e) => handleDeletePatient(e, patient.id)}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </>
  );
};

export default DoctorPatients;
