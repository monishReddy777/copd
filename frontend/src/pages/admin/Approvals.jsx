import React, { useState, useEffect } from 'react';
import { getApprovalRequests, approveUser, rejectUser } from '../../api/admin';
import { ShieldAlert, Check, X, User } from 'lucide-react';
import toast from 'react-hot-toast';

const Approvals = () => {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      const { data } = await getApprovalRequests();
      setRequests(data);
    } catch (error) {
      toast.error('Failed to load approval requests');
      setRequests([
        { id: 1, name: 'Dr. New User', email: 'newdoc@hospital.com', type: 'doctor', created_at: '2026-03-23T10:00:00Z', is_approved: false },
        { id: 2, name: 'New Staff', email: 'newstaff@hospital.com', type: 'staff', created_at: '2026-03-23T09:30:00Z', is_approved: false }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id, type) => {
    try {
      await approveUser({ user_id: id, user_type: type });
      setRequests(requests.filter(req => req.id !== id || req.type !== type));
      toast.success(`${type} request approved successfully`);
    } catch (error) {
      toast.error('Failed to approve request');
    }
  };

  const handleReject = async (id, type) => {
    try {
      await rejectUser({ user_id: id, user_type: type });
      setRequests(requests.filter(req => req.id !== id || req.type !== type));
      toast.success(`${type} request rejected`);
    } catch (error) {
      toast.error('Failed to reject request');
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Pending Approvals</h1>
          <p>Review and verify new doctor and staff registrations</p>
        </div>
      </div>

      {loading ? (
        <div className="loader-container"><div className="spinner"></div></div>
      ) : requests.length === 0 ? (
        <div className="card" style={{ padding: '60px' }}>
          <div className="empty-state">
            <ShieldAlert className="empty-state-icon" style={{ color: 'var(--status-stable)' }} />
            <h3>All caught up</h3>
            <p>There are no pending registration requests at this time.</p>
          </div>
        </div>
      ) : (
        <div className="data-grid">
          {requests.map((request, index) => (
            <div key={`${request.type}-${request.id}-${index}`} className="card" style={{ position: 'relative' }}>
              <div style={{ position: 'absolute', top: '24px', right: '24px' }}>
                <span className={`badge ${request.type === 'doctor' ? 'badge-info' : 'badge-purple'}`}>
                  {request.type}
                </span>
              </div>
              
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
                <div className="sidebar-avatar" style={{ width: '48px', height: '48px', fontSize: '1.25rem' }}>
                  <User size={24} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1.125rem', fontWeight: 600 }}>{request.name}</h3>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>{request.email}</p>
                </div>
              </div>

              <div style={{ padding: '16px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', marginBottom: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>Requested On</span>
                  <span style={{ fontSize: '0.8125rem', fontWeight: 500 }}>
                    {new Date(request.created_at).toLocaleDateString()}
                  </span>
                </div>
                {request.specialization && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>Specialization</span>
                    <span style={{ fontSize: '0.8125rem', fontWeight: 500 }}>{request.specialization}</span>
                  </div>
                )}
                {request.department && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>Department</span>
                    <span style={{ fontSize: '0.8125rem', fontWeight: 500 }}>{request.department}</span>
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', gap: '12px' }}>
                <button 
                  className="btn btn-outline" 
                  style={{ flex: 1, borderColor: 'var(--status-critical)', color: 'var(--status-critical)' }}
                  onClick={() => handleReject(request.id, request.type)}
                >
                  <X size={18} /> Reject
                </button>
                <button 
                  className="btn btn-primary" 
                  style={{ flex: 1, background: 'var(--status-stable)', boxShadow: '0 0 20px rgba(34,197,94,0.3)' }}
                  onClick={() => handleApprove(request.id, request.type)}
                >
                  <Check size={18} /> Approve
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
};

export default Approvals;
