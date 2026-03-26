import React, { useState, useEffect } from 'react';
import { getAdminProfile } from '../../api/admin';
import { updateProfile } from '../../api/auth';
import { useAuth } from '../../hooks/useAuth';
import { Shield, User, Mail, Calendar } from 'lucide-react';
import toast from 'react-hot-toast';
import { getImageUrl } from '../../utils/imageUrl';
import ImageCropper from '../../components/common/ImageCropper';

const Profile = () => {
  const { user, role, login } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [profileImage, setProfileImage] = useState(null);
  const [previewImage, setPreviewImage] = useState(null);
  const [selectedImage, setSelectedImage] = useState(null);
  const [showCropper, setShowCropper] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: ''
  });

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const { data } = await getAdminProfile();
      setProfile(data);
      setFormData({ name: data.name || user.name, email: data.email || user.email });
      if (data.profile_image) setPreviewImage(data.profile_image);
    } catch (error) {
      toast.error('Failed to load profile');
      setFormData({ name: user.name, email: user.email });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const parts = formData.name.split(' ');
      const firstName = parts[0];
      const lastName = parts.slice(1).join(' ');
      
      const formDataPayload = new FormData();
      formDataPayload.append('name', formData.name);
      formDataPayload.append('email', formData.email);
      formDataPayload.append('first_name', firstName);
      formDataPayload.append('last_name', lastName);
      formDataPayload.append('role', role);

      if (profileImage) {
        formDataPayload.append('profile_image', profileImage);
      }
      
      const { data } = await updateProfile(formDataPayload);
      toast.success('Profile updated successfully');
      
      if (data.profile_image) {
        setPreviewImage(data.profile_image);
        setProfileImage(null);
      }

      // Update AuthContext
      const updatedUser = { ...user, ...data };
      const token = localStorage.getItem('token');
      if (token) {
        login(token, updatedUser, role);
      }
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const onCropComplete = (croppedBlob) => {
    const file = new File([croppedBlob], 'profile.jpg', { type: 'image/jpeg' });
    setProfileImage(file);
    setPreviewImage(URL.createObjectURL(croppedBlob));
    setShowCropper(false);
  };

  if (loading) {
    return <div className="loader-container"><div className="spinner"></div></div>;
  }

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1>Admin Profile</h1>
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
            <div className="profile-avatar-lg" style={{ background: 'var(--gradient-purple)', boxShadow: '0 0 20px rgba(139,92,246,0.3)' }}>
              <Shield size={36} />
            </div>
          )}
          <input id="profile-upload" type="file" style={{ display: 'none' }} accept="image/*" onChange={(e) => {
            if (e.target.files && e.target.files[0]) {
              setSelectedImage(URL.createObjectURL(e.target.files[0]));
              setShowCropper(true);
            }
          }} />
        </label>

        {showCropper && (
          <ImageCropper 
            image={selectedImage} 
            onCropComplete={onCropComplete} 
            onCancel={() => setShowCropper(false)} 
          />
        )}
        <div className="profile-info">
          <h2>{formData.name}</h2>
          <p>{role.toUpperCase()} <span>• Platform Administrator</span></p>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>Click avatar to change</p>
        </div>
      </div>

      <div className="card">
        <h3 className="section-title"><User size={20} /> Personal Information</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Full Name</label>
              <input 
                type="text" 
                className="form-input" 
                value={formData.name} 
                onChange={(e) => setFormData({...formData, name: e.target.value})}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input 
                type="email" 
                className="form-input" 
                value={formData.email} 
                onChange={(e) => setFormData({...formData, email: e.target.value})}
                required
              />
            </div>
          </div>
          
          <div className="form-row" style={{ marginTop: '20px', marginBottom: '32px' }}>
            <div className="detail-item">
              <div className="detail-item-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}><Shield size={14}/> Role</div>
              <div className="detail-item-value" style={{ textTransform: 'capitalize' }}>{role}</div>
            </div>
            <div className="detail-item">
              <div className="detail-item-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}><Calendar size={14}/> Account Created</div>
              <div className="detail-item-value">{profile?.created_at ? new Date(profile.created_at).toLocaleDateString() : 'N/A'}</div>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: '20px', borderTop: '1px solid var(--border)' }}>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Profile;
