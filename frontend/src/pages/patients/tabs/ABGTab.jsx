import React, { useState, useEffect } from 'react';
import { getPatientABGs, addABG } from '../../../api/patients';
import { Droplets, Plus, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const ABGTab = ({ patientId }) => {
  const [abgList, setAbgList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    ph: '',
    paco2: '',
    pao2: '',
    hco3: '',
    lactate: '',
    base_excess: '',
    fio2: '21',
    sample_type: 'Arterial'
  });

  useEffect(() => {
    fetchABGs();
  }, [patientId]);

  const fetchABGs = async () => {
    try {
      setLoading(true);
      const { data } = await getPatientABGs(patientId);
      setAbgList(data);
    } catch (error) {
      toast.error('Failed to load ABG history');
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
        ph: parseFloat(formData.ph),
        paco2: parseFloat(formData.paco2),
        pao2: parseFloat(formData.pao2),
        hco3: parseFloat(formData.hco3),
        lactate: formData.lactate ? parseFloat(formData.lactate) : null,
        base_excess: formData.base_excess ? parseFloat(formData.base_excess) : null,
        fio2: parseFloat(formData.fio2),
        sample_type: formData.sample_type
      };
      
      await addABG(payload);
      toast.success('ABG results recorded successfully');
      
      fetchABGs(); // Refresh list from server
      setShowForm(false);
      setFormData({ ph: '', paco2: '', pao2: '', hco3: '', lactate: '', base_excess: '', fio2: '21', sample_type: 'Arterial' });
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to record ABG');
    } finally {
      setSubmitting(false);
    }
  };

  const getPHStatus = (ph) => {
    if (ph < 7.35) return <span style={{ color: 'var(--status-critical)', fontWeight: 600 }}>{ph} (Acidemia)</span>;
    if (ph > 7.45) return <span style={{ color: 'var(--status-warning)', fontWeight: 600 }}>{ph} (Alkalemia)</span>;
    return <span>{ph}</span>;
  };

  const getCO2Status = (co2) => {
    if (co2 > 45) return <span style={{ color: 'var(--status-critical)', fontWeight: 600 }}>{co2} (Hypercapnia)</span>;
    if (co2 < 35) return <span style={{ color: 'var(--status-warning)', fontWeight: 600 }}>{co2} (Hypocapnia)</span>;
    return <span>{co2}</span>;
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Arterial Blood Gas (ABG) History</h3>
        {!showForm && (
          <button className="btn btn-primary" onClick={() => setShowForm(true)}>
            <Plus size={16} /> New ABG Entry
          </button>
        )}
      </div>

      {showForm && (
        <div style={{ background: 'var(--bg-secondary)', padding: '24px', borderRadius: 'var(--radius-md)', marginBottom: '24px', border: '1px solid var(--border)' }}>
          <h4 style={{ marginBottom: '16px', fontWeight: 600 }}>Record ABG Results</h4>
          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">pH</label>
                <input type="number" step="0.01" name="ph" className="form-input" value={formData.ph} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">PaCO2 (mmHg)</label>
                <input type="number" step="0.1" name="paco2" className="form-input" value={formData.paco2} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">PaO2 (mmHg)</label>
                <input type="number" step="0.1" name="pao2" className="form-input" value={formData.pao2} onChange={handleChange} required />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">HCO3 (mEq/L)</label>
                <input type="number" step="0.1" name="hco3" className="form-input" value={formData.hco3} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">Lactate (mmol/L)</label>
                <input type="number" step="0.1" name="lactate" className="form-input" value={formData.lactate} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label className="form-label">Base Excess</label>
                <input type="number" step="0.1" name="base_excess" className="form-input" value={formData.base_excess} onChange={handleChange} />
              </div>
            </div>
            
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">FiO2 (%)</label>
                <input type="number" step="0.1" name="fio2" className="form-input" value={formData.fio2} onChange={handleChange} required />
              </div>
              <div className="form-group" style={{ flex: 2 }}>
                <label className="form-label">Sample Type</label>
                <select name="sample_type" className="form-select" value={formData.sample_type} onChange={handleChange} required>
                  <option value="Arterial">Arterial (ABG)</option>
                  <option value="Venous">Venous (VBG)</option>
                  <option value="Capillary">Capillary (CBG)</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
              <button type="button" className="btn btn-outline" onClick={() => setShowForm(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving...' : 'Save Results'}
              </button>
            </div>
          </form>
        </div>
      )}

      {abgList.length === 0 ? (
        <div className="empty-state">
          <Droplets className="empty-state-icon" />
          <h3>No ABG records</h3>
          <p>Add the first ABG analysis for this patient.</p>
        </div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Date & Time</th>
                <th>pH</th>
                <th>PaCO2</th>
                <th>PaO2 / FiO2</th>
                <th>HCO3 / BE</th>
                <th>Lactate</th>
              </tr>
            </thead>
            <tbody>
              {abgList.map(abg => (
                <tr key={abg.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{new Date(abg.recorded_at).toLocaleDateString()}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{new Date(abg.recorded_at).toLocaleTimeString()} ({abg.sample_type})</div>
                  </td>
                  <td>{getPHStatus(abg.ph)}</td>
                  <td>{getCO2Status(abg.paco2)}</td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{abg.pao2}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>FiO2: {abg.fio2}%</div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{abg.hco3}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>BE: {abg.base_excess || '--'}</div>
                  </td>
                  <td>{abg.lactate || '--'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default ABGTab;
