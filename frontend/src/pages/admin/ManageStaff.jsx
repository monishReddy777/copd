import React, { useState, useEffect } from 'react';
import { getAdminStaff, toggleStaffStatus, deleteStaff } from '../../api/admin';
import { Search, HeartPulse, CheckCircle, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';

const ManageStaff = () => {
  const [staffList, setStaffList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(null);

  useEffect(() => {
    fetchStaff();
  }, []);

  const fetchStaff = async () => {
    try {
      const { data } = await getAdminStaff();
      setStaffList(data.results || data);
    } catch (error) {
      toast.error('Failed to load staff list');
      setStaffList([]);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (id, currentStatus) => {
    try {
      await toggleStaffStatus(id);
      setStaffList(staffList.map(staff =>
        staff.id === id ? { ...staff, is_active: !currentStatus } : staff
      ));
      toast.success(`Staff marked as ${!currentStatus ? 'Active' : 'Inactive'}`);
    } catch (error) {
      toast.error('Failed to update status');
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteStaff(id);
      setStaffList(staffList.filter(s => s.id !== id));
      setConfirmDelete(null);
      toast.success('Staff member removed successfully');
    } catch (error) {
      toast.error('Failed to remove staff member');
    }
  };

  const filteredStaff = staffList.filter(staff =>
    (staff.name || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (staff.email || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (staff.department && staff.department.toLowerCase().includes(searchTerm.toLowerCase()))
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
            <h3 style={{ marginBottom: '12px' }}>Confirm Remove Staff</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
              Are you sure you want to remove <strong>{confirmDelete.name}</strong>? This will revoke their login access.
            </p>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
              <button className="btn btn-outline" onClick={() => setConfirmDelete(null)}>Cancel</button>
              <button className="btn btn-primary" style={{ background: 'var(--status-critical)', borderColor: 'var(--status-critical)' }}
                onClick={() => handleDelete(confirmDelete.id)}>
                Remove Staff
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="page-header">
        <div>
          <h1>Manage Staff</h1>
          <p>View and manage hospital staff accounts</p>
        </div>
        <div className="page-header-actions">
          <div className="search-box">
            <Search className="search-icon" />
            <input
              type="text"
              placeholder="Search staff..."
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
            <h3>Registered Staff ({filteredStaff.length})</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>Staff Details</th>
                <th>Department / ID</th>
                <th>Approval Status</th>
                <th>Account Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredStaff.length > 0 ? (
                filteredStaff.map((staff) => (
                  <tr key={staff.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div className="sidebar-avatar" style={{ width: '40px', height: '40px', background: 'var(--gradient-purple)' }}>
                          <HeartPulse size={20} />
                        </div>
                        <div>
                          <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{staff.name}</div>
                          <div style={{ fontSize: '0.75rem' }}>{staff.email}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <div>{staff.department || 'N/A'}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{staff.license_id || 'N/A'}</div>
                    </td>
                    <td>
                      {staff.is_approved ? (
                        <span className="badge badge-success"><CheckCircle size={12} /> Approved</span>
                      ) : (
                        <span className="badge badge-warning">Pending</span>
                      )}
                    </td>
                    <td>
                      {staff.is_active ? (
                        <span className="badge badge-info">Active</span>
                      ) : (
                        <span className="badge" style={{ background: 'var(--bg-secondary)', color: 'var(--text-muted)' }}>Inactive</span>
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className={`btn btn-sm ${staff.is_active ? 'btn-outline' : 'btn-primary'}`}
                          onClick={() => handleToggleStatus(staff.id, staff.is_active)}
                          style={{ padding: '6px 12px' }}
                        >
                          {staff.is_active ? 'Deactivate' : 'Activate'}
                        </button>
                        <button
                          className="btn btn-sm"
                          onClick={() => setConfirmDelete(staff)}
                          style={{ padding: '6px 10px', background: 'rgba(239,68,68,0.1)', color: 'var(--status-critical)', border: '1px solid rgba(239,68,68,0.3)' }}
                          title="Remove Staff"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5">
                    <div className="empty-state">
                      <HeartPulse className="empty-state-icon" />
                      <h3>No staff found</h3>
                      <p>Adjust your search query to find registered staff members.</p>
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

export default ManageStaff;
