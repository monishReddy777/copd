import React, { useState, useEffect } from 'react';
import { getTherapyRecommendations, startAIAnalysis, acceptTherapy } from '../../../api/therapy';
import api from '../../../api/axios'; // Import api to get patient details
import { Activity, BrainCircuit, Wind, RefreshCw, AlertTriangle, AlertCircle, CheckCircle, ChevronRight, ShieldCheck, Clock } from 'lucide-react';
import toast from 'react-hot-toast';

const TherapyTab = ({ patientId }) => {
  const [recommendations, setRecommendations] = useState(null);
  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [accepting, setAccepting] = useState(false);
  const user = JSON.parse(localStorage.getItem('user'));
  const role = user?.role || localStorage.getItem('role') || 'staff';

  useEffect(() => {
    fetchPatientAndRecommendations();
  }, [patientId]);

  const fetchPatientAndRecommendations = async () => {
    try {
      setLoading(true);
      const [patientRes, recRes] = await Promise.all([
        api.get(`/patients/${patientId}/`),
        getTherapyRecommendations(patientId)
      ]);
      
      setPatient(patientRes.data);
      
      const data = (recRes.data?.recommendations || []).filter(r => r.status === 'pending');
      if (data && data.length > 0) {
        const latest = data[0];
        
        // Extract device and flow from content string
        const content = latest.content;
        let device = 'N/A';
        let flow = 'As recommended';
        
        const deviceMatch = content.match(/AI recommends (.*?) \(Flow: (.*?)\)/);
        if (deviceMatch) {
          device = deviceMatch[1];
          flow = deviceMatch[2];
        } else if (content.includes('AI recommends ')) {
          device = content.split('AI recommends ')[1].split(' with')[0];
        }

        setRecommendations({
          id: latest.id,
          created_at: latest.created_at,
          status: latest.status,
          content: latest.content,
          niv_advice: {
            recommended: latest.content.toLowerCase().includes('niv'),
            reason: latest.content
          },
          device_recommendation: {
            primary_device: device,
            flow_rate: flow,
            rationale: latest.content
          },
          oxygen_status: {
            is_hypoxemic: latest.content.toLowerCase().includes('risk'),
            severity: latest.content.includes('HIGH') ? 'High' : 'Moderate',
            target_spo2_min: 88,
            target_spo2_max: 92
          },
          escalation_triggers: [
            "pH < 7.35 with PaCO2 > 45 mmHg",
            "Respiratory rate > 30 bpm",
            "Signs of respiratory distress",
            "SpO2 remains < 88% despite current therapy"
          ]
        });
      } else {
        setRecommendations(null);
      }
    } catch (error) {
      console.error('Failed to load data', error);
      toast.error('Failed to load therapy data');
    } finally {
      setLoading(false);
    }
  };

  const handleRunAnalysis = async () => {
    setAnalyzing(true);
    try {
      await toast.promise(
        startAIAnalysis(patientId),
        {
          loading: 'AI is analyzing clinical data...',
          success: 'Analysis complete.',
          error: 'Analysis failed. Need Vitals/ABG data.',
        }
      );
      fetchPatientAndRecommendations();
    } catch (error) {
      console.error('Analysis error', error);
    } finally {
      setAnalyzing(false);
    }
  };

  const handleAcceptTherapy = async () => {
    if (role !== 'doctor') {
      toast.error('Only doctors can approve therapy');
      return;
    }

    setAccepting(true);
    try {
      await toast.promise(
        acceptTherapy(patientId, recommendations.id),
        {
          loading: 'Approving therapy...',
          success: 'Therapy approved and started!',
          error: (err) => err.response?.data?.error || 'Failed to approve therapy',
        }
      );
      fetchPatientAndRecommendations();
    } catch (error) {
      console.error('Approval error', error);
    } finally {
      setAccepting(false);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><RefreshCw className="spin" /></div>;

  return (
    <div className="card">
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '32px' }}>
        <div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BrainCircuit size={20} color="var(--accent-purple)" /> Respiratory Therapy Support
          </h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>
            AI-driven device recommendation based on real-time vitals
          </p>
        </div>
        <button 
          className="btn btn-primary" 
          onClick={handleRunAnalysis} 
          disabled={analyzing}
          style={{ background: 'var(--accent-purple)', borderColor: 'var(--accent-purple)' }}
        >
          {analyzing ? <RefreshCw size={16} className="spin" /> : <Activity size={16} />}
          <span style={{ marginLeft: '8px' }}>Run AI Analysis</span>
        </button>
      </div>

      {/* Active Therapy Banner */}
      {patient?.current_device && (
        <div style={{ 
          background: 'rgba(16, 185, 129, 0.1)', 
          border: '1px solid var(--status-stable)', 
          padding: '20px', 
          borderRadius: 'var(--radius-lg)',
          marginBottom: '32px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ background: 'var(--status-stable)', padding: '12px', borderRadius: '50%', color: 'white' }}>
              <ShieldCheck size={24} />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--status-stable)', fontWeight: 700 }}>Active Approved Therapy</div>
              <h4 style={{ fontSize: '1.5rem', fontWeight: 800, margin: '4px 0' }}>{patient.current_device}</h4>
              <div style={{ display: 'flex', gap: '12px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                <span>Flow: <strong>{patient.current_flow_rate}</strong></span>
                <span>•</span>
                <span>Started: {new Date(patient.therapy_approved_at).toLocaleString()}</span>
              </div>
            </div>
          </div>
          <div style={{ textAlign: 'right', display: 'none' }}>
            {/* Could add a 'Stop Therapy' button here if needed */}
          </div>
        </div>
      )}

      {!recommendations ? (
        <div className="empty-state">
          <BrainCircuit className="empty-state-icon" style={{ color: 'var(--accent-purple)' }} />
          <h3>No Recommendations Yet</h3>
          <p>Run analysis to get AI-powered therapy guidance for this patient.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Recommendation Info */}
          <div style={{ 
            background: recommendations.status === 'accepted' ? 'var(--bg-secondary)' : 'var(--gradient-subtle)', 
            padding: '24px', 
            borderRadius: 'var(--radius-lg)', 
            border: '1px solid var(--border)',
            position: 'relative',
            opacity: recommendations.status === 'accepted' ? 0.8 : 1
          }}>
            {recommendations.status === 'accepted' && (
              <div style={{ position: 'absolute', top: '12px', right: '12px', background: 'var(--status-stable)', color: 'white', padding: '4px 12px', borderRadius: '20px', fontSize: '0.75rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px' }}>
                <CheckCircle size={12} /> Approved
              </div>
            )}

            <h4 style={{ fontSize: '1.125rem', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
              <Wind size={20} color="var(--accent-primary)" /> AI Recommendation
            </h4>
            
            <div style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}>
              <div style={{ background: 'var(--bg-surface)', padding: '16px', borderRadius: 'var(--radius-sm)', flex: 2, border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Recommended Device</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--accent-primary)' }}>{recommendations.device_recommendation.primary_device}</div>
              </div>
              <div style={{ background: 'var(--bg-surface)', padding: '16px', borderRadius: 'var(--radius-sm)', flex: 1, border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Flow Rate</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{recommendations.device_recommendation.flow_rate}</div>
              </div>
            </div>

            <div>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', display: 'block', marginBottom: '4px' }}>Clinical Rationale</span>
              <p style={{ background: 'var(--bg-surface)', padding: '12px', borderRadius: 'var(--radius-sm)', fontSize: '0.875rem', border: '1px solid var(--border)', color: 'var(--text-primary)' }}>
                {recommendations.content}
              </p>
            </div>
            
            {recommendations.status !== 'accepted' && role === 'doctor' && (
              <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end' }}>
                <button 
                  className="btn btn-primary" 
                  onClick={handleAcceptTherapy}
                  disabled={accepting}
                  style={{ gap: '8px' }}
                >
                  {accepting ? <RefreshCw size={16} className="spin" /> : <CheckCircle size={16} />}
                  Accept & Start Therapy
                </button>
              </div>
            )}
            
            {recommendations.status !== 'accepted' && role !== 'doctor' && (
              <div style={{ marginTop: '16px', padding: '10px', background: 'rgba(245, 158, 11, 0.1)', borderRadius: 'var(--radius-sm)', fontSize: '0.75rem', color: 'var(--status-warning)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Clock size={14} /> Waiting for doctor's clinical approval
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: '24px' }}>
             {/* Escalation Triggers */}
            <div style={{ flex: 1 }}>
              <h4 style={{ fontSize: '1rem', marginBottom: '16px', color: 'var(--text-primary)' }}>Monitor for Escalation</h4>
              <ul style={{ listStyleType: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {recommendations.escalation_triggers.map((trigger, i) => (
                  <li key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', background: 'rgba(239, 68, 68, 0.05)', padding: '12px 16px', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(239, 68, 68, 0.1)' }}>
                    <AlertCircle size={14} color="var(--status-critical)" style={{ marginTop: '3px' }} />
                    <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>{trigger}</span>
                  </li>
                ))}
              </ul>
            </div>
            
            <div style={{ flex: 1, background: 'var(--bg-secondary)', padding: '20px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)' }}>
              <h4 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', fontSize: '0.9375rem' }}>
                <Activity size={16} color="var(--status-warning)" /> Treatment Targets
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Oxygen Target</span>
                  <span style={{ fontWeight: 600, fontSize: '0.875rem' }}>88% - 92% SpO2</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Hypercapnia</span>
                  <span style={{ fontWeight: 600, fontSize: '0.875rem' }}>Avoid PaCO2 {'>'} 45</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>pH Balance</span>
                  <span style={{ fontWeight: 600, fontSize: '0.875rem' }}>Maintain 7.35 - 7.45</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      )}
    </div>
  );
};

export default TherapyTab;
