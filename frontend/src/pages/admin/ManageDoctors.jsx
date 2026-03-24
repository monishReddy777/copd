import React, { useState, useEffect } from 'react';
import { getAdminDoctors, toggleDoctorStatus, deleteDoctor } from '../../api/admin';
import { Search, Stethoscope, CheckCircle, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';

const ManageDoctors = () => {
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(null);

  useEffect(() => {
    fetchDoctors();
  }, []);

  const fetchDoctors = async () => {
    try {
      const { data } = await getAdminDoctors();
      setDoctors(data.results || data);
    } catch (error) {
      toast.error('Failed to load doctors list');
      setDoctors([]);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (id, currentStatus) => {
    try {
      await toggleDoctorStatus(id);
      setDoctors(doctors.map(doc =>
        doc.id === id ? { ...doc, is_active: !currentStatus } : doc
      ));
      toast.success(`Doctor marked as ${!currentStatus ? 'Active' : 'Inactive'}`);
    } catch (error) {
      toast.error('Failed to update status');
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteDoctor(id);
      setDoctors(doctors.filter(doc => doc.id !== id));
      setConfirmDelete(null);
      toast.success('Doctor removed successfully');
    } catch (error) {
      toast.error('Failed to remove doctor');
    }
  };

  const filteredDoctors = doctors.filter(doctor =>
    (doctor.name || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (doctor.email || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <>
      {/* Confirm Delete Modal */}
      {confirmDelete && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999
        }}>
          <div className="card" style={{ maxWidth: '400px', width: '90%' }}>
            <h3 style={{ marginBottom: '12px' }}>Confirm Remove Doctor</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
              Are you sure you want to remove <strong>{confirmDelete.name}</strong>? This will revoke their login access.
            </p>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
              <button className="btn btn-outline" onClick={() => setConfirmDelete(null)}>Cancel</button>
              <button className="btn btn-primary" style={{ background: 'var(--status-critical)', borderColor: 'var(--status-critical)' }}
                onClick={() => handleDelete(confirmDelete.id)}>
                Remove Doctor
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="page-header">
        <div>
          <h1>Manage Doctors</h1>
          <p>View and manage doctor accounts</p>
        </div>
        <div className="page-header-actions">
          <div className="search-box">
            <Search className="search-icon" />
            <input
              type="text"
              placeholder="Search doctors..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>
      </div>

      {loading ? (
        <div className="loader-container"><div className="spinner"></div></div>
      ) : (
        <div className="table-container">
          <div className="table-header">
            <h3>Registered Doctors ({filteredDoctors.length})</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>Doctor Details</th>
                <th>Specialization</th>
                <th>License No.</th>
                <th>Approval Status</th>
                <th>Account Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredDoctors.length > 0 ? (
                filteredDoctors.map((doctor) => (
                  <tr key={doctor.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div className="sidebar-avatar" style={{ width: '40px', height: '40px' }}>
                          <Stethoscope size={20} />
                        </div>
                        <div>
                          <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{doctor.name}</div>
                          <div style={{ fontSize: '0.75rem' }}>{doctor.email}</div>
                        </div>
                      </div>
                    </td>
                    <td>{doctor.specialization || 'N/A'}</td>
                    <td style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>{doctor.license_number || 'N/A'}</td>
                    <td>
                      {doctor.is_approved ? (
                        <span className="badge badge-success"><CheckCircle size={12} /> Approved</span>
                      ) : (
                        <span className="badge badge-warning">Pending</span>
                      )}
                    </td>
                    <td>
                      {doctor.is_active ? (
                        <span className="badge badge-info">Active</span>
                      ) : (
                        <span className="badge" style={{ background: 'var(--bg-secondary)', color: 'var(--text-muted)' }}>Inactive</span>
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className={`btn btn-sm ${doctor.is_active ? 'btn-outline' : 'btn-primary'}`}
                          onClick={() => handleToggleStatus(doctor.id, doctor.is_active)}
                          style={{ padding: '6px 12px' }}
                        >
                          {doctor.is_active ? 'Deactivate' : 'Activate'}
                        </button>
                        <button
                          className="btn btn-sm"
                          onClick={() => setConfirmDelete(doctor)}
                          style={{ padding: '6px 10px', background: 'rgba(239,68,68,0.1)', color: 'var(--status-critical)', border: '1px solid rgba(239,68,68,0.3)' }}
                          title="Remove Doctor"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6">
                    <div className="empty-state">
                      <Stethoscope className="empty-state-icon" />
                      <h3>No doctors found</h3>
                      <p>Adjust your search query to find registered doctors.</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
};

export default ManageDoctors;
