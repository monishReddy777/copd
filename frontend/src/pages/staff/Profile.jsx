import React, { useState, useEffect } from 'react';
import { getStaffProfile } from '../../api/admin';
import { updateProfile } from '../../api/auth';
import { useAuth } from '../../hooks/useAuth';
import { HeartPulse, User, Calendar, Phone } from 'lucide-react';
import toast from 'react-hot-toast';
import { getImageUrl } from '../../utils/imageUrl';

const StaffProfile = () => {
  const { user, role, login } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [profileImage, setProfileImage] = useState(null);
  const [previewImage, setPreviewImage] = useState(null);
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
      const stf = data.staff_profile || {};
      setFormData({ 
        name: stf.name || `${data.first_name} ${data.last_name}`.trim(), 
        email: data.email,
        phone_number: data.phone_number || stf.phone || '',
        department: stf.department || '',
        staff_id: stf.license_id || ''
      });
      if (data.profile_image) setPreviewImage(data.profile_image);
    } catch (error) {
      toast.error('Failed to load profile');
      setFormData({ name: user.name, email: user.email, phone_number: '', department: '', staff_id: '' });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.email.toLowerCase().endsWith('@gmail.com')) {
      toast.error('Only @gmail.com email addresses are allowed');
      return;
    }
    setSaving(true);
    try {
      const parts = formData.name.split(' ');
      const firstName = parts[0];
      const lastName = parts.slice(1).join(' ');
      
      const formDataPayload = new FormData();
      formDataPayload.append('name', formData.name);
      formDataPayload.append('email', formData.email);
      formDataPayload.append('phone_number', formData.phone_number);
      formDataPayload.append('department', formData.department);
      formDataPayload.append('first_name', firstName);
      formDataPayload.append('last_name', lastName);
      formDataPayload.append('license_id', formData.staff_id);
      formDataPayload.append('role', role);

      if (profileImage) {
        formDataPayload.append('profile_image', profileImage);
      }
      
      const { data } = await updateProfile(formDataPayload);
      toast.success('Profile updated successfully');
      
      const updatedUser = { ...user, name: formData.name, email: formData.email, ...data };
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
        <label htmlFor="profile-upload" style={{ cursor: 'pointer' }}>
          {previewImage ? (
            <img 
              src={getImageUrl(previewImage)} 
              alt="Avatar" 
              style={{ width: '80px', height: '80px', borderRadius: '50%', objectFit: 'cover' }} 
            />
          ) : (
            <div className="profile-avatar-lg" style={{ background: 'var(--status-stable-bg)', color: 'var(--status-stable)', boxShadow: '0 0 20px rgba(34,197,94,0.3)' }}>
              <HeartPulse size={36} />
            </div>
          )}
          <input id="profile-upload" type="file" style={{ display: 'none' }} accept="image/*" onChange={(e) => {
            if (e.target.files && e.target.files[0]) {
              setProfileImage(e.target.files[0]);
              setPreviewImage(URL.createObjectURL(e.target.files[0]));
            }
          }} />
        </label>
        <div className="profile-info">
          <h2>{formData.name}</h2>
          <p>{formData.department || 'Staff'} <span>• ID: {formData.staff_id || 'N/A'}</span></p>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>Click avatar to change</p>
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
