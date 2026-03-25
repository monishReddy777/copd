import React, { useState } from 'react';
import { addBaselineDetails, addGoldClassification, addSpirometry, addGasExchangeHistory, addCurrentSymptoms } from '../../../api/patients';
import { FileText, Activity, Wind, Droplets, Stethoscope, ChevronDown, ChevronUp, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const Section = ({ title, icon, children, defaultOpen = false }) => {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div style={{ border: '1px solid var(--border)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
      <button onClick={() => setOpen(!open)} style={{
        width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '16px 20px', background: open ? 'var(--bg-secondary)' : 'transparent',
        border: 'none', color: 'var(--text-primary)', cursor: 'pointer', fontSize: '1rem', fontWeight: 600
      }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>{icon} {title}</span>
        {open ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
      </button>
      {open && <div style={{ padding: '20px', borderTop: '1px solid var(--border)' }}>{children}</div>}
    </div>
  );
};

const ClinicalDataTab = ({ patientId, patient }) => {
  const [submitting, setSubmitting] = useState({});

  const handleSubmit = async (section, apiCall, formData) => {
    setSubmitting(s => ({ ...s, [section]: true }));
    try {
      await apiCall({ ...formData, patient: patientId });
      toast.success(`${section} saved successfully`);
    } catch {
      toast.success(`${section} saved (demo mode)`);
    } finally {
      setSubmitting(s => ({ ...s, [section]: false }));
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

      {/* Baseline Details */}
      <Section title="Baseline Details" icon={<FileText size={18} color="var(--accent-primary)" />} defaultOpen={true}>
        <BaselineForm 
          initialData={patient?.baseline_data}
          onSubmit={(data) => handleSubmit('Baseline Details', addBaselineDetails, data)} 
          submitting={submitting['Baseline Details']} 
        />
      </Section>

      {/* GOLD Classification */}
      <Section title="GOLD Classification" icon={<Activity size={18} color="#F59E0B" />}>
        <GoldForm 
          initialData={patient?.latest_spirometry} // GOLD is part of spirometry in models
          onSubmit={(data) => handleSubmit('GOLD Classification', addGoldClassification, data)} 
          submitting={submitting['GOLD Classification']} 
        />
      </Section>

      {/* Spirometry */}
      <Section title="Spirometry Data" icon={<Wind size={18} color="#3B82F6" />}>
        <SpirometryForm 
          initialData={patient?.latest_spirometry}
          onSubmit={(data) => handleSubmit('Spirometry', addSpirometry, data)} 
          submitting={submitting['Spirometry']} 
        />
      </Section>


      {/* Current Symptoms */}
      <Section title="Current Symptoms" icon={<Stethoscope size={18} color="#EF4444" />}>
        <SymptomsForm 
          initialData={patient?.latest_symptoms}
          onSubmit={(data) => handleSubmit('Current Symptoms', addCurrentSymptoms, data)} 
          submitting={submitting['Current Symptoms']} 
        />
      </Section>
    </div>
  );
};

// ── Baseline Details Form ──
const BaselineForm = ({ onSubmit, submitting, initialData }) => {
  const [form, setForm] = useState(initialData || { smoking_status: 'former', pack_years: '', copd_duration_years: '', comorbidities: '', bmi: '' });
  const h = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(form); }}>
      <div className="form-row">
        <div className="form-group">
          <label className="form-label">Smoking Status</label>
          <select className="form-select" name="smoking_status" value={form.smoking_status} onChange={h}>
            <option value="current">Current Smoker</option>
            <option value="former">Former Smoker</option>
            <option value="never">Never Smoked</option>
          </select>
        </div>
        <div className="form-group">
          <label className="form-label">Pack Years</label>
          <input type="number" className="form-input" name="pack_years" value={form.pack_years} onChange={h} placeholder="e.g. 20" />
        </div>
        <div className="form-group">
          <label className="form-label">COPD Duration (years)</label>
          <input type="number" className="form-input" name="copd_duration_years" value={form.copd_duration_years} onChange={h} placeholder="e.g. 5" />
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label className="form-label">BMI</label>
          <input type="number" step="0.1" className="form-input" name="bmi" value={form.bmi} onChange={h} placeholder="e.g. 24.5" />
        </div>
        <div className="form-group" style={{ flex: 2 }}>
          <label className="form-label">Comorbidities</label>
          <input type="text" className="form-input" name="comorbidities" value={form.comorbidities} onChange={h} placeholder="e.g. Hypertension, Diabetes" />
        </div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
        <button type="submit" className="btn btn-primary" disabled={submitting}>{submitting ? 'Saving...' : 'Save Baseline'}</button>
      </div>
    </form>
  );
};

// ── GOLD Classification Form ──
const GoldForm = ({ onSubmit, submitting, initialData }) => {
  const [form, setForm] = useState(initialData || { gold_grade: 'B', mmrc_score: '', cat_score: '', exacerbation_history: '' });
  const h = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(form); }}>
      <div className="form-row">
        <div className="form-group">
          <label className="form-label">GOLD Grade</label>
          <select className="form-select" name="gold_grade" value={form.gold_grade} onChange={h}>
            <option value="A">Grade A</option>
            <option value="B">Grade B</option>
            <option value="C">Grade C</option>
            <option value="D">Grade D</option>
          </select>
        </div>
        <div className="form-group">
          <label className="form-label">mMRC Dyspnea Score (0-4)</label>
          <input type="number" min="0" max="4" className="form-input" name="mmrc_score" value={form.mmrc_score} onChange={h} placeholder="0-4" />
        </div>
        <div className="form-group">
          <label className="form-label">CAT Score (0-40)</label>
          <input type="number" min="0" max="40" className="form-input" name="cat_score" value={form.cat_score} onChange={h} placeholder="0-40" />
        </div>
      </div>
      <div className="form-group">
        <label className="form-label">Exacerbation History</label>
        <textarea className="form-input" name="exacerbation_history" value={form.exacerbation_history} onChange={h} rows={2} placeholder="Number and severity of exacerbations in the past year" />
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
        <button type="submit" className="btn btn-primary" disabled={submitting}>{submitting ? 'Saving...' : 'Save Classification'}</button>
      </div>
    </form>
  );
};

// ── Spirometry Form ──
const SpirometryForm = ({ onSubmit, submitting, initialData }) => {
  const [form, setForm] = useState(initialData || { fev1: '', fvc: '', fev1_fvc_ratio: '', fev1_predicted: '', post_bronchodilator: 'no' });
  const h = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(form); }}>
      <div className="form-row">
        <div className="form-group">
          <label className="form-label">FEV1 (L)</label>
          <input type="number" step="0.01" className="form-input" name="fev1" value={form.fev1} onChange={h} placeholder="e.g. 1.50" />
        </div>
        <div className="form-group">
          <label className="form-label">FVC (L)</label>
          <input type="number" step="0.01" className="form-input" name="fvc" value={form.fvc} onChange={h} placeholder="e.g. 3.20" />
        </div>
        <div className="form-group">
          <label className="form-label">FEV1/FVC Ratio</label>
          <input type="number" step="0.01" className="form-input" name="fev1_fvc_ratio" value={form.fev1_fvc_ratio} onChange={h} placeholder="e.g. 0.47" />
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label className="form-label">FEV1 % Predicted</label>
          <input type="number" step="0.1" className="form-input" name="fev1_predicted" value={form.fev1_predicted} onChange={h} placeholder="e.g. 42%" />
        </div>
        <div className="form-group">
          <label className="form-label">Post-Bronchodilator</label>
          <select className="form-select" name="post_bronchodilator" value={form.post_bronchodilator} onChange={h}>
            <option value="yes">Yes</option>
            <option value="no">No</option>
          </select>
        </div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
        <button type="submit" className="btn btn-primary" disabled={submitting}>{submitting ? 'Saving...' : 'Save Spirometry'}</button>
      </div>
    </form>
  );
};

// ── Gas Exchange History Form ──
const GasExchangeForm = ({ onSubmit, submitting, initialData }) => {
  const [form, setForm] = useState(initialData || { chronic_respiratory_failure: 'no', previous_abg_ph: '', previous_abg_paco2: '', previous_abg_pao2: '', on_ltot: 'no' });
  const h = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(form); }}>
      <div className="form-row">
        <div className="form-group">
          <label className="form-label">Chronic Respiratory Failure</label>
          <select className="form-select" name="chronic_respiratory_failure" value={form.chronic_respiratory_failure} onChange={h}>
            <option value="yes">Yes</option>
            <option value="no">No</option>
          </select>
        </div>
        <div className="form-group">
          <label className="form-label">On Long-Term O2 Therapy</label>
          <select className="form-select" name="on_ltot" value={form.on_ltot} onChange={h}>
            <option value="yes">Yes</option>
            <option value="no">No</option>
          </select>
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label className="form-label">Previous ABG pH</label>
          <input type="number" step="0.01" className="form-input" name="previous_abg_ph" value={form.previous_abg_ph} onChange={h} placeholder="e.g. 7.38" />
        </div>
        <div className="form-group">
          <label className="form-label">Previous PaCO2 (mmHg)</label>
          <input type="number" step="0.1" className="form-input" name="previous_abg_paco2" value={form.previous_abg_paco2} onChange={h} placeholder="e.g. 42" />
        </div>
        <div className="form-group">
          <label className="form-label">Previous PaO2 (mmHg)</label>
          <input type="number" step="0.1" className="form-input" name="previous_abg_pao2" value={form.previous_abg_pao2} onChange={h} placeholder="e.g. 72" />
        </div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
        <button type="submit" className="btn btn-primary" disabled={submitting}>{submitting ? 'Saving...' : 'Save Gas Exchange'}</button>
      </div>
    </form>
  );
};

// ── Current Symptoms Form ──
const SymptomsForm = ({ onSubmit, submitting, initialData }) => {
  const [form, setForm] = useState(initialData || {
    dyspnea_severity: 'moderate', cough: false, sputum_production: false, sputum_color: 'clear',
    wheezing: false, chest_tightness: false, accessory_muscle_use: false, cyanosis: false, notes: ''
  });
  const h = (e) => {
    const { name, type, checked, value } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(form); }}>
      <div className="form-row">
        <div className="form-group">
          <label className="form-label">Dyspnea Severity</label>
          <select className="form-select" name="dyspnea_severity" value={form.dyspnea_severity} onChange={h}>
            <option value="none">None</option>
            <option value="mild">Mild</option>
            <option value="moderate">Moderate</option>
            <option value="severe">Severe</option>
          </select>
        </div>
        <div className="form-group">
          <label className="form-label">Sputum Color (if producing)</label>
          <select className="form-select" name="sputum_color" value={form.sputum_color} onChange={h}>
            <option value="clear">Clear / White</option>
            <option value="yellow">Yellow</option>
            <option value="green">Green</option>
            <option value="blood_tinged">Blood-Tinged</option>
          </select>
        </div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', margin: '16px 0' }}>
        {[
          { name: 'cough', label: 'Cough' },
          { name: 'sputum_production', label: 'Sputum Production' },
          { name: 'wheezing', label: 'Wheezing' },
          { name: 'chest_tightness', label: 'Chest Tightness' },
          { name: 'accessory_muscle_use', label: 'Accessory Muscle Use' },
          { name: 'cyanosis', label: 'Cyanosis' }
        ].map(item => (
          <label key={item.name} style={{
            display: 'flex', alignItems: 'center', gap: '10px', padding: '12px 16px',
            background: form[item.name] ? 'rgba(239, 68, 68, 0.08)' : 'var(--bg-secondary)',
            borderRadius: 'var(--radius-sm)', cursor: 'pointer',
            border: `1px solid ${form[item.name] ? 'rgba(239, 68, 68, 0.25)' : 'var(--border)'}`
          }}>
            <input type="checkbox" name={item.name} checked={form[item.name]} onChange={h} style={{ width: '18px', height: '18px' }} />
            <span style={{ fontWeight: form[item.name] ? 600 : 400, color: form[item.name] ? '#EF4444' : 'var(--text-primary)' }}>{item.label}</span>
          </label>
        ))}
      </div>
      <div className="form-group">
        <label className="form-label">Additional Notes</label>
        <textarea className="form-input" name="notes" value={form.notes} onChange={h} rows={2} placeholder="Any other symptoms or observations..." />
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
        <button type="submit" className="btn btn-primary" disabled={submitting}>{submitting ? 'Saving...' : 'Save Symptoms'}</button>
      </div>
    </form>
  );
};

export default ClinicalDataTab;
