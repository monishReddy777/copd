import React, { useState, useEffect } from 'react';
import { getPatientABGs, addABG, addSpirometry } from '../../../api/patients';
import { useAuth } from '../../../hooks/useAuth';
import { Droplets, Plus, AlertCircle, RefreshCw } from 'lucide-react';
import api from '../../../api/axios';
import toast from 'react-hot-toast';

const ABGTab = ({ patientId }) => {
  const { role } = useAuth();
  const [abgList, setAbgList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [showGoldForm, setShowGoldForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [goldSubmitting, setGoldSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    ph: '',
    paco2: '',
    pao2: '',
    hco3: '',
  });
  const [goldData, setGoldData] = useState({ fev1: '', fev1_fvc: '' });
  const [savedGoldData, setSavedGoldData] = useState(null);

  useEffect(() => {
    fetchABGs();
    fetchSpirometry();
  }, [patientId]);

  const fetchSpirometry = async () => {
    try {
      const { data } = await api.get(`/patients/${patientId}/spirometry/`);
      // It might be an array or a single object. Get the latest.
      const latest = Array.isArray(data) ? data[0] : data;
      if (latest && latest.fev1) {
        setSavedGoldData(latest);
      }
    } catch (e) {
      // no spirometry data yet
    }
  };

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
    
    // Validation
    const ph = parseFloat(formData.ph);
    const paco2 = parseFloat(formData.paco2);
    const pao2 = parseFloat(formData.pao2);
    const hco3 = parseFloat(formData.hco3);

    if (ph < 6.8 || ph > 7.8) return toast.error('Invalid pH. Range: 6.8 - 7.8');
    if (paco2 < 20 || paco2 > 80) return toast.error('Invalid PaCO₂. Range: 20 - 80 mmHg');
    if (pao2 < 30 || pao2 > 120) return toast.error('Invalid PaO₂. Range: 30 - 120 mmHg');
    if (hco3 < 10 || hco3 > 40) return toast.error('Invalid HCO₃. Range: 10 - 40 mmol/L');

    setSubmitting(true);
    try {
      const payload = {
        patient: patientId,
        ph,
        paco2,
        pao2,
        hco3,
      };
      
      await addABG(payload);
      toast.success('ABG results recorded successfully');
      fetchABGs();
      setShowForm(false);
      setFormData({ ph: '', paco2: '', pao2: '', hco3: '' });
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to record ABG');
    } finally {
      setSubmitting(false);
    }
  };

  const handleGoldSubmit = async (e) => {
    e.preventDefault();
    setGoldSubmitting(true);
    try {
      await addSpirometry({ patient: patientId, fev1: parseFloat(goldData.fev1), fev1_fvc: parseFloat(goldData.fev1_fvc) });
      toast.success('GOLD Classification recorded');
      fetchSpirometry();
      setShowGoldForm(false);
      setGoldData({ fev1: '', fev1_fvc: '' });
    } catch (error) {
      toast.error('Failed to save GOLD classification');
    } finally {
      setGoldSubmitting(false);
    }
  };

  const getGoldStage = (fev1, ratio) => {
    // COPD diagnosis requires FEV1/FVC < 0.7
    const r = parseFloat(ratio || 1.0);
    const f = parseFloat(fev1 || 0);
    
    if (r >= 0.7) return { stage: 'Non-COPD / Normal', color: 'var(--status-stable)' };
    
    if (f >= 80) return { stage: 'GOLD 1 (Mild)', color: 'var(--status-stable)' };
    if (f >= 50) return { stage: 'GOLD 2 (Moderate)', color: '#f59e0b' };
    if (f >= 30) return { stage: 'GOLD 3 (Severe)', color: '#ef8c00' };
    return { stage: 'GOLD 4 (Very Severe)', color: 'var(--status-critical)' };
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
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* ABG Entry Card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Arterial Blood Gas (ABG) Records</h3>
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
                  <label className="form-label">pH (6.8 - 7.8)</label>
                  <input type="number" step="0.01" name="ph" className="form-input" value={formData.ph} onChange={handleChange} required placeholder="6.8 - 7.8" min="6.8" max="7.8" />
                </div>
                <div className="form-group">
                  <label className="form-label">PaCO2 (20 - 80 mmHg)</label>
                  <input type="number" step="0.1" name="paco2" className="form-input" value={formData.paco2} onChange={handleChange} required placeholder="20 - 80" min="20" max="80" />
                </div>
                <div className="form-group">
                  <label className="form-label">PaO2 (30 - 120 mmHg)</label>
                  <input type="number" step="0.1" name="pao2" className="form-input" value={formData.pao2} onChange={handleChange} required placeholder="30 - 120" min="30" max="120" />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">HCO3 (10 - 40 mmol/L)</label>
                  <input type="number" step="0.1" name="hco3" className="form-input" value={formData.hco3} onChange={handleChange} required placeholder="10 - 40" min="10" max="40" />
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
                  <th>PaO2</th>
                  <th>HCO3</th>
                </tr>
              </thead>
              <tbody>
                {abgList.map(abg => (
                  <tr key={abg.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{new Date(abg.created_at).toLocaleDateString()}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{new Date(abg.created_at).toLocaleTimeString()}</div>
                    </td>
                    <td>{getPHStatus(abg.ph)}</td>
                    <td>{getCO2Status(abg.paco2)}</td>
                    <td style={{ fontWeight: 600 }}>{abg.pao2} mmHg</td>
                    <td style={{ fontWeight: 600 }}>{abg.hco3} mEq/L</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* GOLD Classification Card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600 }}>GOLD Classification</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>
              Based on post-bronchodilator FEV1 (% predicted)
            </p>
          </div>
          {!showGoldForm && (
            <button className="btn btn-outline" onClick={() => setShowGoldForm(true)}>
              <Plus size={16} /> Record FEV1
            </button>
          )}
        </div>

        {savedGoldData && !showGoldForm && (
          <div style={{ padding: '16px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: `1px solid ${getGoldStage(parseFloat(savedGoldData.fev1)).color}`, marginBottom: '16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>Recorded Classification</div>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, color: getGoldStage(savedGoldData.fev1, savedGoldData.fev1_fvc).color }}>
                {getGoldStage(savedGoldData.fev1, savedGoldData.fev1_fvc).stage}
              </div>
            </div>
            <div style={{ textAlign: 'right', display: 'flex', gap: '16px' }}>
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>FEV1</div>
                <div style={{ fontWeight: 600 }}>{savedGoldData.fev1}%</div>
              </div>
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>FEV1/FVC</div>
                <div style={{ fontWeight: 600 }}>{savedGoldData.fev1_fvc}</div>
              </div>
            </div>
          </div>
        )}

        {showGoldForm && (
          <div style={{ background: 'var(--bg-secondary)', padding: '20px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)', marginBottom: '16px' }}>
            <form onSubmit={handleGoldSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">FEV1 (% predicted)</label>
                  <input type="number" step="0.1" className="form-input" value={goldData.fev1} onChange={e => setGoldData({...goldData, fev1: e.target.value})} required placeholder="e.g. 65" />
                </div>
                <div className="form-group">
                  <label className="form-label">FEV1/FVC ratio</label>
                  <input type="number" step="0.01" className="form-input" value={goldData.fev1_fvc} onChange={e => setGoldData({...goldData, fev1_fvc: e.target.value})} required placeholder="e.g. 0.65" />
                </div>
              </div>
              {goldData.fev1 && (
                <div style={{ marginBottom: '12px', padding: '10px', background: 'var(--bg-surface)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border)' }}>
                  <span style={{ color: getGoldStage(goldData.fev1, goldData.fev1_fvc).color, fontWeight: 700 }}>
                    {getGoldStage(goldData.fev1, goldData.fev1_fvc).stage}
                  </span>
                </div>
              )}
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowGoldForm(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={goldSubmitting}>
                  {goldSubmitting ? 'Saving...' : 'Save GOLD'}
                </button>
              </div>
            </form>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: '12px' }}>
          {[
            { stage: 'GOLD 1', label: 'Mild', fev1: 'FEV1 ≥ 80%', color: 'var(--status-stable)' },
            { stage: 'GOLD 2', label: 'Moderate', fev1: '50% ≤ FEV1 < 80%', color: '#f59e0b' },
            { stage: 'GOLD 3', label: 'Severe', fev1: '30% ≤ FEV1 < 50%', color: '#ef8c00' },
            { stage: 'GOLD 4', label: 'Very Severe', fev1: 'FEV1 < 30%', color: 'var(--status-critical)' },
          ].map(g => (
            <div key={g.stage} style={{ padding: '16px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-md)', borderLeft: `4px solid ${g.color}` }}>
              <div style={{ fontWeight: 700, color: g.color, fontSize: '0.875rem' }}>{g.stage}</div>
              <div style={{ fontWeight: 600, fontSize: '1rem', marginTop: '4px' }}>{g.label}</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>{g.fev1}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ABGTab;
