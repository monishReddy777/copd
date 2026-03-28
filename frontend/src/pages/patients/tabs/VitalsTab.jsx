import React, { useState, useEffect } from 'react';
import { getPatientVitals, addVitals } from '../../../api/patients';
import { useAuth } from '../../../hooks/useAuth';
import { Activity, Plus } from 'lucide-react';
import toast from 'react-hot-toast';

const VitalsTab = ({ patientId }) => {
  const { role } = useAuth();
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

    // Validation
    const spo2 = parseFloat(formData.spo2);
    const heart_rate = parseInt(formData.heart_rate);
    const respiratory_rate = parseInt(formData.respiratory_rate);
    const blood_pressure_sys = parseInt(formData.blood_pressure_sys);
    const blood_pressure_dia = parseInt(formData.blood_pressure_dia);
    const temperature = parseFloat(formData.temperature);

    if (spo2 < 70 || spo2 > 100) return toast.error('Invalid SpO2. Range: 70 - 100%');
    if (heart_rate < 40 || heart_rate > 130) return toast.error('Invalid Heart Rate. Range: 40 - 130 bpm');
    if (respiratory_rate < 8 || respiratory_rate > 30) return toast.error('Invalid Respiratory Rate. Range: 8 - 30 bpm');
    if (blood_pressure_sys < 90 || blood_pressure_sys > 180) return toast.error('Invalid Systolic BP. Range: 90 - 180 mmHg');
    if (blood_pressure_dia < 60 || blood_pressure_dia > 120) return toast.error('Invalid Diastolic BP. Range: 60 - 120 mmHg');
    if (temperature < 34.0 || temperature > 41.0) return toast.error('Invalid Temperature. Range: 34.0 - 41.0 °C');

    setSubmitting(true);
    try {
      const payload = {
        patient: patientId,
        heart_rate,
        respiratory_rate,
        systolic_bp: blood_pressure_sys,
        diastolic_bp: blood_pressure_dia,
        temperature,
        spo2,
        fio2: 21, // default room air
        loc_alert: 'Alert' // default
      };
      
      await addVitals(payload);
      toast.success('Vitals recorded successfully');
      fetchVitals();
      setShowForm(false);
      setFormData({ heart_rate: '', respiratory_rate: '', blood_pressure_sys: '', blood_pressure_dia: '', temperature: '', spo2: '' });
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
        <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Vitals Entry</h3>
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
                <label className="form-label">SpO2 (70 - 100%)</label>
                <input type="number" step="0.1" name="spo2" className="form-input" value={formData.spo2} onChange={handleChange} required placeholder="70 - 100" min="70" max="100" />
              </div>
              <div className="form-group">
                <label className="form-label">Heart Rate (40 - 130 bpm)</label>
                <input type="number" name="heart_rate" className="form-input" value={formData.heart_rate} onChange={handleChange} required placeholder="40 - 130" min="40" max="130" />
              </div>
              <div className="form-group">
                <label className="form-label">Resp Rate (8 - 30 /min)</label>
                <input type="number" name="respiratory_rate" className="form-input" value={formData.respiratory_rate} onChange={handleChange} required placeholder="8 - 30" min="8" max="30" />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">BP Systolic (90 - 180 mmHg)</label>
                <input type="number" name="blood_pressure_sys" className="form-input" value={formData.blood_pressure_sys} onChange={handleChange} required placeholder="90 - 180" min="90" max="180" />
              </div>
              <div className="form-group">
                <label className="form-label">BP Diastolic (60 - 120 mmHg)</label>
                <input type="number" name="blood_pressure_dia" className="form-input" value={formData.blood_pressure_dia} onChange={handleChange} required placeholder="60 - 120" min="60" max="120" />
              </div>
              <div className="form-group">
                <label className="form-label">Temperature (34 - 41 °C)</label>
                <input type="number" step="0.1" name="temperature" className="form-input" value={formData.temperature} onChange={handleChange} required placeholder="34.0 - 41.0" min="34.0" max="41.0" />
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
                <th>SpO2</th>
                <th>HR / RR</th>
                <th>BP</th>
                <th>Temp</th>
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
                    <div style={{ fontWeight: 600, color: vitals.spo2 < 88 ? 'var(--status-critical)' : 'inherit' }}>
                      {vitals.spo2}%
                    </div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{vitals.heart_rate} bpm</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>RR: {vitals.resp_rate} /min</div>
                  </td>
                  <td style={{ fontWeight: 600 }}>{vitals.bp}</td>
                  <td>{vitals.temperature}°C</td>
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
