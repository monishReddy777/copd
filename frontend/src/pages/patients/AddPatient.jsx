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
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData({ ...formData, [e.target.name]: value });
  };

  const handleSymptomsChange = (e) => {
    setFormData(prev => ({
      ...prev,
      symptoms: {
        ...prev.symptoms,
        [e.target.name]: e.target.type === 'checkbox' ? e.target.checked : e.target.value
      }
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const { data } = await addPatient(formData);
      // We can also submit symptoms separately if needed by backend
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

          <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)', marginTop: '24px', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Symptoms
          </h4>

          <div className="form-row">
             <div className="form-group" style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <input type="checkbox" name="cough" onChange={handleSymptomsChange} /> Cough
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <input type="checkbox" name="sputum" onChange={handleSymptomsChange} /> Sputum
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <input type="checkbox" name="wheezing" onChange={handleSymptomsChange} /> Wheezing
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <input type="checkbox" name="fever" onChange={handleSymptomsChange} /> Fever
                </label>
             </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">mMRC Grade</label>
              <select name="mmrc_grade" className="form-select" onChange={handleSymptomsChange}>
                <option value="0">Grade 0</option>
                <option value="1">Grade 1</option>
                <option value="2">Grade 2</option>
                <option value="3">Grade 3</option>
                <option value="4">Grade 4</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Has Previous Diagnosis?</label>
              <select name="has_previous_diagnosis" className="form-select" onChange={handleChange}>
                <option value="false">No</option>
                <option value="true">Yes</option>
              </select>
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
