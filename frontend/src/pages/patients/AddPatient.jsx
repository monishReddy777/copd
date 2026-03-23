import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { addPatient } from '../../api/patients';
import { UserPlus, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';

const AddPatient = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    full_name: '',
    age: '',
    dob: '',
    sex: 'Male',
    height_cm: '',
    weight_kg: '',
    ward: '',
    bed_number: '',
    admission_date: new Date().toISOString().split('T')[0]
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const calculateBMI = () => {
    if (formData.height_cm && formData.weight_kg) {
      const heightInMeters = formData.height_cm / 100;
      const bmi = formData.weight_kg / (heightInMeters * heightInMeters);
      return bmi.toFixed(1);
    }
    return '--';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const { data } = await addPatient(formData);
      toast.success('Patient added successfully');
      navigate(`/patients/${data.id}`);
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to add patient');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <button onClick={() => navigate(-1)} className="btn btn-ghost" style={{ padding: '0 0 12px 0' }}>
            <ArrowLeft size={16} /> Back
          </button>
          <h1>New Patient Admission</h1>
          <p>Enter patient demographics and baseline details</p>
        </div>
      </div>

      <div className="card">
        <h3 className="section-title"><UserPlus size={20} /> Patient Information</h3>
        <form onSubmit={handleSubmit}>
          
          <div className="form-row">
            <div className="form-group" style={{ flex: 2 }}>
              <label className="form-label">Full Name</label>
              <input type="text" name="full_name" className="form-input" value={formData.full_name} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Sex</label>
              <select name="sex" className="form-select" value={formData.sex} onChange={handleChange}>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Date of Birth</label>
              <input type="date" name="dob" className="form-input" value={formData.dob} onChange={handleChange} required />
            </div>
          </div>

          <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)', marginTop: '24px', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Physical Statistics
          </h4>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Height (cm)</label>
              <input type="number" name="height_cm" className="form-input" value={formData.height_cm} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Weight (kg)</label>
              <input type="number" name="weight_kg" className="form-input" value={formData.weight_kg} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">BMI</label>
              <input type="text" className="form-input" value={calculateBMI()} disabled style={{ background: 'var(--bg-secondary)', fontWeight: 600 }} />
            </div>
          </div>

          <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)', marginTop: '24px', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Location & Admission
          </h4>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Ward / Department</label>
              <input type="text" name="ward" className="form-input" placeholder="e.g., ICU, Ward B" value={formData.ward} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Bed Number</label>
              <input type="text" name="bed_number" className="form-input" value={formData.bed_number} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Admission Date</label>
              <input type="date" name="admission_date" className="form-input" value={formData.admission_date} onChange={handleChange} required />
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: '24px', borderTop: '1px solid var(--border)', marginTop: '32px', gap: '12px' }}>
            <button type="button" className="btn btn-outline" onClick={() => navigate(-1)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Adding...' : 'Add Patient'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddPatient;
