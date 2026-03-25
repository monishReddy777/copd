import React, { useState, useEffect } from 'react';
import { addCurrentSymptoms } from '../../../api/patients';
import { Stethoscope, Plus, History } from 'lucide-react';
import api from '../../../api/axios';
import toast from 'react-hot-toast';

const SymptomsTab = ({ patientId }) => {
  const [symptomsList, setSymptomsList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    mmrc_grade: 0,
    cough: false,
    sputum: false,
    wheezing: false,
    fever: false,
    chest_tightness: false
  });

  useEffect(() => {
    fetchSymptoms();
  }, [patientId]);

  const fetchSymptoms = async () => {
    try {
      setLoading(true);
      const { data } = await api.get(`/patients/${patientId}/symptoms/`);
      setSymptomsList(data.results || data);
    } catch (error) {
      toast.error('Failed to load symptoms history');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, type, checked, value } = e.target;
    setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await addCurrentSymptoms({ 
        patient: patientId, 
        ...formData,
        mmrc_grade: parseInt(formData.mmrc_grade)
      });
      toast.success('Symptoms recorded successfully');
      fetchSymptoms();
      setShowForm(false);
      setFormData({ mmrc_grade: 0, cough: false, sputum: false, wheezing: false, fever: false, chest_tightness: false });
    } catch (error) {
      toast.error('Failed to record symptoms');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Current Symptoms</h3>
          {!showForm && (
            <button className="btn btn-primary" onClick={() => setShowForm(true)}>
              <Plus size={16} /> Record Symptoms
            </button>
          )}
        </div>

        {showForm && (
          <div style={{ background: 'var(--bg-secondary)', padding: '24px', borderRadius: 'var(--radius-md)', marginBottom: '24px', border: '1px solid var(--border)' }}>
            <form onSubmit={handleSubmit}>
              <div className="form-group" style={{ marginBottom: '20px' }}>
                <label className="form-label">mMRC Dyspnea Grade (0-4)</label>
                <input type="number" min="0" max="4" name="mmrc_grade" className="form-input" value={formData.mmrc_grade} onChange={handleChange} required />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '12px', marginBottom: '20px' }}>
                {[
                  { name: 'cough', label: 'Cough' },
                  { name: 'sputum', label: 'Sputum Production' },
                  { name: 'wheezing', label: 'Wheezing' },
                  { name: 'fever', label: 'Fever' },
                  { name: 'chest_tightness', label: 'Chest Tightness' }
                ].map(item => (
                  <label key={item.name} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '12px', background: 'var(--bg-card)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border)', cursor: 'pointer' }}>
                    <input type="checkbox" name={item.name} checked={formData[item.name]} onChange={handleChange} />
                    <span>{item.label}</span>
                  </label>
                ))}
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Saving...' : 'Save Symptoms'}
                </button>
              </div>
            </form>
          </div>
        )}

        {symptomsList.length === 0 ? (
          <div className="empty-state">
            <Stethoscope className="empty-state-icon" />
            <h3>No symptoms recorded</h3>
            <p>Add the first symptom assessment for this patient.</p>
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Date & Time</th>
                  <th>mMRC</th>
                  <th>Symptoms</th>
                </tr>
              </thead>
              <tbody>
                {symptomsList.map(item => (
                  <tr key={item.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{new Date(item.created_at).toLocaleDateString()}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{new Date(item.created_at).toLocaleTimeString()}</div>
                    </td>
                    <td>Grade {item.mmrc_grade}</td>
                    <td>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                        {item.cough && <span className="badge badge-stable" style={{ fontSize: '0.7rem' }}>Cough</span>}
                        {item.sputum && <span className="badge badge-stable" style={{ fontSize: '0.7rem' }}>Sputum</span>}
                        {item.wheezing && <span className="badge badge-stable" style={{ fontSize: '0.7rem' }}>Wheezing</span>}
                        {item.fever && <span className="badge badge-stable" style={{ fontSize: '0.7rem' }}>Fever</span>}
                        {item.chest_tightness && <span className="badge badge-stable" style={{ fontSize: '0.7rem' }}>Chest Tightness</span>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default SymptomsTab;
