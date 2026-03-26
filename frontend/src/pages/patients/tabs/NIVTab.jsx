import React, { useState, useEffect } from 'react';
import { getAIRisk } from '../../../api/patients';
import { Wind, CheckCircle, Zap, AlertTriangle, Phone, ArrowLeft, BrainCircuit, Info } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

const NIVTab = ({ patientId }) => {
  const navigate = useNavigate();
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAIData();
  }, [patientId]);

  const fetchAIData = async () => {
    try {
      const { data } = await getAIRisk(patientId);
      setAnalysis(data);
    } catch (error) {
      toast.error('Failed to load NIV assessment');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  const nivStatus = analysis?.niv_status || 'Not Currently Indicated';
  const nivRationale = analysis?.niv_rationale || 'pH > 7.35 and PaCO2 is stable. Continue careful oxygen therapy and monitor.';
  const isBiPAP = nivStatus === 'BiPAP Indicated';
  const isICU = analysis?.icu_status === 'ICU Review Required';

  if (isICU) {
    return (
      <div style={{ 
        background: '#EF4444', 
        color: 'white', 
        padding: '60px 24px', 
        borderRadius: 'var(--radius-lg)', 
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '32px',
        minHeight: '400px',
        justifyContent: 'center'
      }}>
        <div style={{ 
          width: '80px', height: '80px', background: 'rgba(255,255,255,0.2)', 
          borderRadius: '50%', display: 'flex', justifyContent: 'center', alignItems: 'center' 
        }}>
          <AlertTriangle size={48} />
        </div>
        
        <div>
          <h2 style={{ fontSize: '2.5rem', fontWeight: 800, margin: '0 0 16px 0' }}>ICU Review Required</h2>
          <p style={{ fontSize: '1.1rem', opacity: 0.9, maxWidth: '600px', margin: '0 auto' }}>
            Patient meets criteria for immediate ICU admission or outreach review.
          </p>
        </div>

        <div style={{ 
          background: 'white', color: '#1a1a1a', padding: '24px 32px', 
          borderRadius: '24px', width: '100%', maxWidth: '400px', textAlign: 'left' 
        }}>
          <h4 style={{ color: '#EF4444', fontWeight: 600, fontSize: '0.9rem', marginBottom: '16px', textTransform: 'uppercase' }}>Triggers Met</h4>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {analysis?.icu_triggers?.map((trigger, idx) => (
              <li key={idx} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '1.1rem', fontWeight: 600 }}>
                <div style={{ width: '6px', height: '6px', background: '#EF4444', borderRadius: '50%' }}></div>
                {trigger}
              </li>
            ))}
          </ul>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', width: '100%', maxWidth: '400px' }}>
          <button 
            onClick={() => navigate('/doctor/dashboard')}
            style={{ 
              background: '#0D9488', color: 'white', border: 'none', 
              padding: '18px', borderRadius: '16px', fontWeight: 700, fontSize: '1.1rem',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px',
              cursor: 'pointer', boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
            }}
          >
            Return to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div className="card" style={{ padding: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
          <Wind size={24} color="#0D9488" />
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: '#1e293b' }}>NIV Recommendation</h3>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '32px', flexWrap: 'wrap' }}>
          <div style={{ 
            flex: '1', minWidth: '300px', background: isBiPAP ? 'rgba(99, 102, 241, 0.05)' : 'rgba(16, 185, 129, 0.05)', 
            border: `1px solid ${isBiPAP ? '#6366f1' : '#10b981'}30`, borderRadius: '24px',
            padding: '40px 24px', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px'
          }}>
            <div style={{ 
              width: '48px', height: '48px', borderRadius: '50%', 
              background: isBiPAP ? '#6366f1' : '#10b981', color: 'white',
              display: 'flex', justifyContent: 'center', alignItems: 'center'
            }}>
              {isBiPAP ? <Zap size={24} /> : <CheckCircle size={24} />}
            </div>
            <h4 style={{ 
              fontSize: '1.5rem', fontWeight: 700, margin: 0, 
              color: isBiPAP ? '#4f46e5' : '#059669' 
            }}>
              {nivStatus}
            </h4>
          </div>

          <div style={{ flex: '1.5', minWidth: '300px' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Rationale</span>
            <p style={{ fontSize: '1.1rem', color: '#475569', lineHeight: 1.6, marginTop: '8px', fontWeight: 500 }}>
              {nivRationale}
            </p>
          </div>
        </div>
      </div>

      {isBiPAP && (
        <div style={{ 
          background: '#F8FAFC', borderRadius: '32px', padding: '40px', textAlign: 'center',
          boxShadow: '0 4px 20px rgba(0,0,0,0.03)', border: '1px solid #E2E8F0'
        }}>
          <div style={{ 
            width: '64px', height: '64px', background: '#F5F3FF', color: '#7C3AED', 
            borderRadius: '50%', display: 'flex', justifyContent: 'center', alignItems: 'center',
            margin: '0 auto 20px'
          }}>
            <Zap size={32} />
          </div>

          <h3 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#1e293b', marginBottom: '8px' }}>BiPAP Indicated</h3>
          <p style={{ color: '#64748B', maxWidth: '500px', margin: '0 auto 32px' }}>
            {nivRationale}
          </p>

          <div style={{ 
            background: 'white', borderRadius: '24px', padding: '32px', 
            maxWidth: '460px', margin: '0 auto 32px', boxShadow: '0 2px 10px rgba(0,0,0,0.02)'
          }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#CBD5E1', textTransform: 'uppercase', display: 'block', marginBottom: '24px' }}>Initial Settings</span>
            
            <div style={{ display: 'flex', justifyContent: 'center', gap: '48px' }}>
              <div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#1e293b' }}>{analysis?.niv_settings?.ipap || 14}</div>
                <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#94a3b8' }}>IPAP (cmH₂O)</div>
              </div>
              <div style={{ width: '1px', background: '#F1F5F9' }}></div>
              <div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#1e293b' }}>{analysis?.niv_settings?.epap || 4}</div>
                <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#94a3b8' }}>EPAP (cmH₂O)</div>
              </div>
            </div>
            
            <div style={{ marginTop: '24px', fontSize: '0.9375rem', color: '#CBD5E1', fontWeight: 500 }}>
              Backup Rate: {analysis?.niv_settings?.backup_rate || 12} bpm
            </div>
          </div>

          <button style={{ 
            background: '#EF4444', color: 'white', border: 'none', 
            padding: '16px 48px', borderRadius: '16px', fontWeight: 700, fontSize: '1rem',
            cursor: 'pointer', boxShadow: '0 4px 12px rgba(239, 68, 68, 0.2)'
          }}>
            Check ICU Criteria
          </button>
        </div>
      )}
    </div>
  );
};

export default NIVTab;
