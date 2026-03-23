import React, { useState, useEffect } from 'react';
import { getPatientVitals, addVitals } from '../../../api/patients';
import { Activity, Thermometer, Wind, Plus, Clock } from 'lucide-react';
import toast from 'react-hot-toast';

const VitalsTab = ({ patientId }) => {
  const [vitalsList, setVitalsList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    heart_rate: '',
    respiratory_rate: '',
    blood_pressure_sys: '',
    blood_pressure_dia: '',
    temperature: '',
    spo2: '',
    fio2: '21',
    level_of_consciousness: 'Alert'
  });

  useEffect(() => {
    fetchVitals();
  }, [patientId]);

  const fetchVitals = async () => {
    try {
      setLoading(true);
      const { data } = await getPatientVitals(patientId);
      setVitalsList(data);
    } catch (error) {
      toast.error('Failed to load vitals history');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const payload = {
        patient: patientId,
        heart_rate: parseInt(formData.heart_rate),
        respiratory_rate: parseInt(formData.respiratory_rate),
        systolic_bp: parseInt(formData.blood_pressure_sys),
        diastolic_bp: parseInt(formData.blood_pressure_dia),
        temperature: parseFloat(formData.temperature),
        spo2: parseFloat(formData.spo2),
        fio2: parseFloat(formData.fio2),
        consciousness_level: formData.level_of_consciousness
      };
      
      await addVitals(payload);
      toast.success('Vitals recorded successfully');
      
      fetchVitals(); // Refresh list from server
      setShowForm(false);
      setFormData({ heart_rate: '', respiratory_rate: '', blood_pressure_sys: '', blood_pressure_dia: '', temperature: '', spo2: '', fio2: '21', level_of_consciousness: 'Alert' });
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to record vitals');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Vitals & Clinical Data</h3>
        {!showForm && (
          <button className="btn btn-primary" onClick={() => setShowForm(true)}>
            <Plus size={16} /> Record Vitals
          </button>
        )}
      </div>

      {showForm && (
        <div style={{ background: 'var(--bg-secondary)', padding: '24px', borderRadius: 'var(--radius-md)', marginBottom: '24px', border: '1px solid var(--border)' }}>
          <h4 style={{ marginBottom: '16px', fontWeight: 600 }}>New Vitals Entry</h4>
          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">SpO2 (%)</label>
                <input type="number" step="0.1" name="spo2" className="form-input" value={formData.spo2} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">Heart Rate (bpm)</label>
                <input type="number" name="heart_rate" className="form-input" value={formData.heart_rate} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">Resp Rate (/min)</label>
                <input type="number" name="respiratory_rate" className="form-input" value={formData.respiratory_rate} onChange={handleChange} required />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">BP Systolic</label>
                <input type="number" name="blood_pressure_sys" className="form-input" value={formData.blood_pressure_sys} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">BP Diastolic</label>
                <input type="number" name="blood_pressure_dia" className="form-input" value={formData.blood_pressure_dia} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">Temp (°C)</label>
                <input type="number" step="0.1" name="temperature" className="form-input" value={formData.temperature} onChange={handleChange} required />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">FiO2 (%)</label>
                <input type="number" step="0.1" name="fio2" className="form-input" value={formData.fio2} onChange={handleChange} required />
              </div>
              <div className="form-group" style={{ flex: 2 }}>
                <label className="form-label">Level of Consciousness</label>
                <select name="level_of_consciousness" className="form-select" value={formData.level_of_consciousness} onChange={handleChange} required>
                  <option value="Alert">Alert (A)</option>
                  <option value="Voice">Responsive to Voice (V)</option>
                  <option value="Pain">Responsive to Pain (P)</option>
                  <option value="Unresponsive">Unresponsive (U)</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
              <button type="button" className="btn btn-outline" onClick={() => setShowForm(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving...' : 'Save Vitals'}
              </button>
            </div>
          </form>
        </div>
      )}

      {vitalsList.length === 0 ? (
        <div className="empty-state">
          <Activity className="empty-state-icon" />
          <h3>No vitals recorded</h3>
          <p>Record the first set of vitals for this patient.</p>
        </div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Date & Time</th>
                <th>SpO2 / FiO2</th>
                <th>HR / RR</th>
                <th>BP / Temp</th>
                <th>Consciousness</th>
                <th>Recorded By</th>
              </tr>
            </thead>
            <tbody>
              {vitalsList.map(vitals => (
                <tr key={vitals.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{new Date(vitals.created_at).toLocaleDateString()}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{new Date(vitals.created_at).toLocaleTimeString()}</div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600, color: vitals.spo2 < 88 ? 'var(--status-critical)' : 'inherit' }}>{vitals.spo2}%</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>FiO2: {vitals.fio2 || '--'}%</div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{vitals.heart_rate} bpm</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>RR: {vitals.resp_rate} /min</div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{vitals.bp}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{vitals.temperature}°C</div>
                  </td>
                  <td>{vitals.loc_alert || '--'}</td>
                  <td><div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>{vitals.recorded_by || 'Staff'}</div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default VitalsTab;
