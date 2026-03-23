import React, { useState, useEffect } from 'react';
import { getStaffProfile } from '../../api/admin';
import { updateProfile } from '../../api/auth';
import { useAuth } from '../../hooks/useAuth';
import { HeartPulse, User, Calendar, Phone } from 'lucide-react';
import toast from 'react-hot-toast';

const StaffProfile = () => {
  const { user, role, login } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone_number: '',
    department: '',
    staff_id: ''
  });

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const { data } = await getStaffProfile();
      setProfile(data);
      setFormData({ 
        name: data.name || user.name, 
        email: data.email || user.email,
        phone_number: data.phone_number || '',
        department: data.department || '',
        staff_id: data.staff_id || ''
      });
    } catch (error) {
      toast.error('Failed to load profile');
      setFormData({ name: user.name, email: user.email, phone_number: '', department: '', staff_id: '' });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await updateProfile({ ...formData, role });
      toast.success('Profile updated successfully');
      
      const updatedUser = { ...user, name: formData.name, email: formData.email };
      const token = localStorage.getItem('token');
      if (token) login(token, updatedUser, role);
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1>Staff Profile</h1>
          <p>Manage your account settings</p>
        </div>
      </div>

      <div className="profile-header">
        <div className="profile-avatar-lg" style={{ background: 'var(--status-stable-bg)', color: 'var(--status-stable)', boxShadow: '0 0 20px rgba(34,197,94,0.3)' }}>
          <HeartPulse size={36} />
        </div>
        <div className="profile-info">
          <h2>{formData.name}</h2>
          <p>{formData.department || 'Staff'} <span>• ID: {formData.staff_id || 'N/A'}</span></p>
        </div>
      </div>

      <div className="card">
        <h3 className="section-title"><User size={20} /> Personal Information</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Full Name</label>
              <input type="text" className="form-input" value={formData.name} onChange={(e) => setFormData({...formData, name: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input type="email" className="form-input" value={formData.email} onChange={(e) => setFormData({...formData, email: e.target.value})} required />
            </div>
          </div>
          
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Phone Number</label>
              <input type="text" className="form-input" value={formData.phone_number} onChange={(e) => setFormData({...formData, phone_number: e.target.value})} />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Department</label>
              <input type="text" className="form-input" value={formData.department} onChange={(e) => setFormData({...formData, department: e.target.value})} />
            </div>
            <div className="form-group">
              <label className="form-label">Staff ID</label>
              <input type="text" className="form-input" value={formData.staff_id} onChange={(e) => setFormData({...formData, staff_id: e.target.value})} />
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: '20px', borderTop: '1px solid var(--border)', marginTop: '20px' }}>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default StaffProfile;
