import React, { useState, useEffect } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { getPatientDetails } from '../../api/patients';
import { useAuth } from '../../hooks/useAuth';
import { Activity, Thermometer, Wind, AlertCircle, Calendar, Droplets, ChevronRight } from 'lucide-react';
import toast from 'react-hot-toast';

// Tab Components
import VitalsTab from './tabs/VitalsTab';
import ABGTab from './tabs/ABGTab';
import TherapyTab from './tabs/TherapyTab';
import OxygenTab from './tabs/OxygenTab';
import AIAnalysisTab from './tabs/AIAnalysisTab';
import DecisionSupportTab from './tabs/DecisionSupportTab';
import ABGTrendsTab from './tabs/ABGTrendsTab';
import EscalationTab from './tabs/EscalationTab';
import ClinicalDataTab from './tabs/ClinicalDataTab';

const PatientDetail = () => {
  const { id } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { role } = useAuth();
  
  const defaultTab = searchParams.get('tab') || 'overview';
  const [activeTab, setActiveTab] = useState(defaultTab);
  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setActiveTab(defaultTab);
  }, [defaultTab]);

  useEffect(() => {
    fetchPatientData();
  }, [id]);

  const fetchPatientData = async () => {
    try {
      const { data } = await getPatientDetails(id);
      setPatient(data);
    } catch (error) {
      toast.error('Failed to load patient details');
      setPatient({
        id,
        full_name: 'Robert Fox',
        age: 68,
        dob: '1954-05-12',
        sex: 'Male',
        height_cm: 175,
        weight_kg: 78,
        ward: 'ICU',
        bed_number: 'A-12',
        status: 'critical',
        admission_date: '2026-03-20',
        latest_vitals: { hr: 110, rr: 28, bp: '140/90', temp: 38.2, spo2: 86 },
        latest_abg: { ph: 7.31, paco2: 55, pao2: 58, hco3: 28, lac: 2.1 }
      });
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setSearchParams({ tab });
  };

  if (loading || !patient) return <div className="loader-container"><div className="spinner"></div></div>;

  // Build tabs based on role: staff only sees Overview, Vitals, ABG Records
  const tabs = [
    { key: 'overview', label: 'Overview' },
    { key: 'vitals', label: 'Vitals' },
    { key: 'abg', label: 'ABG Records' },
    ...(role === 'doctor' ? [
      { key: 'oxygen', label: 'O₂ Status' },
      { key: 'clinical', label: 'Clinical Data' },
      { key: 'therapy', label: 'AI Therapy' },
      { key: 'ai-analysis', label: 'AI Analysis' },
      { key: 'decision', label: 'Decision Support' },
      { key: 'trends', label: 'ABG Trends' },
      { key: 'escalation', label: 'Escalation' },
    ] : []),
  ];

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
      
      {/* Patient Header Card */}
      <div className="card" style={{ marginBottom: '24px', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, left: 0, bottom: 0, width: '6px', background: patient.status === 'critical' ? 'var(--status-critical)' : patient.status === 'warning' ? 'var(--status-warning)' : 'var(--status-stable)' }}></div>
        
        <div className="patient-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingLeft: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
              <h1 style={{ fontSize: '1.75rem', fontWeight: 700, margin: 0 }}>{patient.full_name}</h1>
              {patient.status === 'critical' ? <span className="badge badge-critical">Critical</span> : 
               patient.status === 'warning' ? <span className="badge badge-warning">Warning</span> : 
               <span className="badge badge-stable">Stable</span>}
            </div>
            
            <div style={{ display: 'flex', gap: '16px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              <span>{patient.sex}</span>
              <span>•</span>
              <span>{patient.age} yrs (DOB: {patient.dob})</span>
              <span>•</span>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{patient.ward} - Bed {patient.bed_number}</span>
            </div>
          </div>
          
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Admitted On</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 500 }}>
              <Calendar size={14} /> {new Date(patient.admission_date || patient.created_at).toLocaleDateString()}
            </div>
          </div>
        </div>

        {/* Quick Vitals Strip */}
        <div style={{ display: 'flex', gap: '20px', marginTop: '24px', paddingTop: '20px', borderTop: '1px solid var(--border-light)', paddingLeft: '16px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div className={`alert-icon ${patient.latest_vitals?.spo2 < 88 ? 'red' : 'blue'}`} style={{ width: '32px', height: '32px', fontSize: '14px' }}><Wind size={16} /></div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>SpO2</div>
              <div style={{ fontWeight: 700, fontSize: '1.125rem', color: patient.latest_vitals?.spo2 < 88 ? 'var(--status-critical)' : 'inherit' }}>{patient.latest_vitals?.spo2 || '--'}%</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div className="alert-icon orange" style={{ width: '32px', height: '32px', fontSize: '14px' }}><Activity size={16} /></div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Heart Rate</div>
              <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{patient.latest_vitals?.hr || '--'} bpm</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div className="alert-icon blue" style={{ width: '32px', height: '32px', fontSize: '14px' }}><Activity size={16} style={{ transform: 'rotate(90deg)' }} /></div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Resp Rate</div>
              <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{patient.latest_vitals?.rr || '--'} /min</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div className="alert-icon red" style={{ width: '32px', height: '32px', fontSize: '14px' }}><Droplets size={16} /></div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>BP</div>
              <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{patient.latest_vitals?.bp || '--'}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs Layout - Scrollable */}
      <div style={{ marginBottom: '24px', overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
        <div className="tabs" style={{ background: 'var(--bg-surface)', padding: '4px', borderRadius: 'var(--radius-lg)', display: 'inline-flex', minWidth: '100%' }}>
          {tabs.map(t => (
            <button key={t.key} className={`tab ${activeTab === t.key ? 'active' : ''}`}
              onClick={() => handleTabChange(t.key)}
              style={{ padding: '10px 16px', whiteSpace: 'nowrap', fontSize: '0.8125rem' }}>
              {t.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content Areas */}
      
      {activeTab === 'overview' && (
        <div className="data-grid">
          <div className="card">
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <AlertCircle size={18} color="var(--status-warning)" /> Active Clinical Alerts
            </h3>
            {patient.status !== 'stable' ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div className="alert-card critical" style={{ margin: 0 }}>
                  <div className="alert-icon red"><AlertCircle size={16} /></div>
                  <div className="alert-content">
                    <h4 style={{ fontSize: '0.875rem' }}>Severe Hypoxemia</h4>
                    <p style={{ fontSize: '0.8125rem' }}>SpO2 dropped to 86%. Immediate intervention required.</p>
                  </div>
                </div>
                <div className="alert-card warning" style={{ margin: 0 }}>
                  <div className="alert-icon orange"><AlertCircle size={16} /></div>
                  <div className="alert-content">
                    <h4 style={{ fontSize: '0.875rem' }}>Respiratory Acidosis</h4>
                    <p style={{ fontSize: '0.8125rem' }}>Latest ABG indicates elevated PaCO2 (55 mmHg).</p>
                  </div>
                </div>
              </div>
            ) : (
              <p style={{ color: 'var(--text-muted)' }}>No active alerts for this patient.</p>
            )}
          </div>

          <div className="card">
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px' }}>Current Management</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '12px', borderBottom: '1px solid var(--border-light)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Current Device</span>
                <span style={{ fontWeight: 600 }}>{patient.current_device || 'Not Set'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '12px', borderBottom: '1px solid var(--border-light)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Flow Rate</span>
                <span style={{ fontWeight: 600 }}>{patient.current_flow_rate || '--'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '12px', borderBottom: '1px solid var(--border-light)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Target SpO2</span>
                <span style={{ fontWeight: 600 }}>88 - 92%</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Next Reassessment</span>
                <span style={{ fontWeight: 600, color: 'var(--status-warning)' }}>In 45 mins</span>
              </div>
            </div>
            {role === 'doctor' && (
              <button className="btn btn-outline btn-block" style={{ marginTop: '24px' }} onClick={() => handleTabChange('therapy')}>
                View AI Recommendations <ChevronRight size={16} />
              </button>
            )}
          </div>
        </div>
      )}

      {activeTab === 'vitals' && <VitalsTab patientId={id} />}
      {activeTab === 'abg' && <ABGTab patientId={id} />}
      {activeTab === 'oxygen' && <OxygenTab patientId={id} />}
      {activeTab === 'clinical' && <ClinicalDataTab patientId={id} patient={patient} />}
      {role === 'doctor' && activeTab === 'therapy' && <TherapyTab patientId={id} />}
      {role === 'doctor' && activeTab === 'ai-analysis' && <AIAnalysisTab patientId={id} />}
      {role === 'doctor' && activeTab === 'decision' && <DecisionSupportTab patientId={id} patient={patient} onApproval={fetchPatientData} />}
      {activeTab === 'trends' && <ABGTrendsTab patientId={id} />}
      {activeTab === 'escalation' && <EscalationTab patientId={id} />}

    </div>
  );
};

export default PatientDetail;
