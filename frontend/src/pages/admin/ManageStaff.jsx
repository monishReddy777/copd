import React, { useState, useEffect } from 'react';
import { getAdminStaff, toggleStaffStatus } from '../../api/admin';
import { Search, HeartPulse, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const ManageStaff = () => {
  const [staffList, setStaffList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchStaff();
  }, []);

  const fetchStaff = async () => {
    try {
      const { data } = await getAdminStaff();
      setStaffList(data.results || data);
    } catch (error) {
      toast.error('Failed to load staff list');
      setStaffList([
        { id: 1, name: 'Sarah Nurse', email: 'sarah@hospital.com', department: 'ICU', staff_id: 'STF-001', is_active: true, is_approved: true },
        { id: 2, name: 'Mike Tech', email: 'mike@hospital.com', department: 'Emergency', staff_id: 'STF-002', is_active: false, is_approved: true }
      ]);
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

  const filteredStaff = staffList.filter(staff => 
    staff.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    staff.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (staff.department && staff.department.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <>
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
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{staff.staff_id || 'N/A'}</div>
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
                      <button 
                        className={`btn btn-sm ${staff.is_active ? 'btn-outline' : 'btn-primary'}`}
                        onClick={() => handleToggleStatus(staff.id, staff.is_active)}
                        style={{ padding: '6px 12px' }}
                      >
                        {staff.is_active ? 'Deactivate' : 'Activate'}
                      </button>
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
