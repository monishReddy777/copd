import React, { useState, useEffect } from 'react';
import { getDoctorProfile } from '../../api/admin';
import { updateProfile } from '../../api/auth';
import { useAuth } from '../../hooks/useAuth';
import { Stethoscope, User } from 'lucide-react';
import toast from 'react-hot-toast';
import ImageCropper from '../../components/common/ImageCropper';
import { getImageUrl } from '../../utils/imageUrl';

const DoctorProfile = () => {
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
    email: '',
    phone_number: '',
    specialization: '',
    license_number: ''
  });

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const { data } = await getDoctorProfile();
      setProfile(data);
      const doc = data.doctor_profile || {};
      setFormData({
        name: doc.name || `${data.first_name} ${data.last_name}`.trim(),
        email: data.email,
        phone_number: data.phone_number || doc.phone || '',
        specialization: doc.specialization || '',
        license_number: doc.license_number || ''
      });
      if (data.profile_image) setPreviewImage(data.profile_image);
    } catch (error) {
      toast.error('Failed to load profile');
      setFormData({ name: user?.name || '', email: user?.email || '', phone_number: '', specialization: '', license_number: '' });
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
      formDataPayload.append('specialization', formData.specialization);
      formDataPayload.append('license_number', formData.license_number);
      formDataPayload.append('first_name', firstName);
      formDataPayload.append('last_name', lastName);
      formDataPayload.append('role', role);

      if (profileImage) {
        formDataPayload.append('profile_image', profileImage);
      }

      const { data } = await updateProfile(formDataPayload);
      toast.success('Profile updated successfully');

      // Update local preview with the final URL from server
      if (data.profile_image) {
        setPreviewImage(data.profile_image);
        setProfileImage(null); // Clear selected file
      }

      const updatedUser = { ...user, ...data };
      const token = localStorage.getItem('token');
      if (token) login(token, updatedUser, role);
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

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1>Doctor Profile</h1>
          <p>Manage your clinical account settings</p>
        </div>
      </div>

      <div className="profile-header">
        <label htmlFor="doctor-profile-upload" style={{ cursor: 'pointer' }}>
          {previewImage ? (
            <img
              src={getImageUrl(previewImage)}
              alt="Avatar"
              style={{ width: '80px', height: '80px', borderRadius: '50%', objectFit: 'cover' }}
            />
          ) : (
            <div className="profile-avatar-lg">
              <Stethoscope size={36} />
            </div>
          )}
          <input id="doctor-profile-upload" type="file" style={{ display: 'none' }} accept="image/*" onChange={(e) => {
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
          <p>{formData.specialization || 'Doctor'} <span>• {formData.license_number || 'No License Added'}</span></p>
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
            <div className="form-group">
              <label className="form-label">Specialization</label>
              <input type="text" className="form-input" value={formData.specialization} onChange={(e) => setFormData({...formData, specialization: e.target.value})} />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">License Number</label>
            <input type="text" className="form-input" value={formData.license_number} onChange={(e) => setFormData({...formData, license_number: e.target.value})} />
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

export default DoctorProfile;
