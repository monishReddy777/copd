import React, { useState, useEffect } from 'react';
import { getTherapyRecommendations, startAIAnalysis } from '../../../api/therapy';
import { Activity, BrainCircuit, Wind, RefreshCw, AlertTriangle, AlertCircle, CheckCircle, ChevronRight } from 'lucide-react';
import toast from 'react-hot-toast';

const TherapyTab = ({ patientId }) => {
  const [recommendations, setRecommendations] = useState(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);

  useEffect(() => {
    fetchRecommendations();
  }, [patientId]);

  const fetchRecommendations = async () => {
    try {
      setLoading(true);
      const { data } = await getTherapyRecommendations(patientId);
      
      // If no recommendations found in DB, we'll keep it as null
      if (data && data.length > 0) {
        // We take the latest recommendation
        const latest = data[0];
        setRecommendations({
          id: latest.id,
          created_at: latest.created_at,
          status: latest.status || 'completed',
          content: latest.content,
          niv_advice: {
            recommended: latest.rec_type === 'niv',
            reason: latest.content
          },
          device_recommendation: {
            primary_device: latest.rec_type === 'therapy' ? latest.content.split('AI recommends ')[1]?.split(' with')[0] || latest.content : 'N/A',
            fio2: '24-35',
            flow_rate: '2-4 L/min',
            rationale: latest.content
          },
          oxygen_status: {
            is_hypoxemic: true, // Placeholder logic
            severity: 'Moderate',
            target_spo2_min: 88,
            target_spo2_max: 92
          },
          escalation_triggers: [
            "pH < 7.35 with PaCO2 > 45 mmHg",
            "Respiratory rate > 30 bpm",
            "Signs of respiratory distress"
          ]
        });
      } else {
        setRecommendations(null);
      }
    } catch (error) {
      console.error('Failed to load therapy recommendations', error);
      toast.error('Failed to load therapy recommendations');
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
          loading: 'AI is analyzing latest clinical data...',
          success: 'Analysis complete. Recommendations updated.',
          error: 'Analysis failed. Please check if patient has vitals/ABG data.',
        }
      );
      fetchRecommendations(); // Refresh data
    } catch (error) {
      console.error('Failed to start analysis', error);
    } finally {
      setAnalyzing(false);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '32px' }}>
        <div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BrainCircuit size={20} color="var(--accent-purple)" /> AI Therapy Recommendations
          </h3>
          {recommendations && (
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>
              Last updated: {new Date(recommendations.created_at).toLocaleString()}
            </p>
          )}
        </div>
        <button 
          className="btn btn-primary" 
          onClick={handleRunAnalysis} 
          disabled={analyzing}
          style={{ background: 'var(--accent-purple)', borderColor: 'var(--accent-purple)', boxShadow: '0 0 15px rgba(139, 92, 246, 0.3)' }}
        >
          {analyzing ? (
             <><RefreshCw size={16} className="spin" style={{ marginRight: '8px' }} /> Analyzing...</>
          ) : (
            <><Activity size={16} style={{ marginRight: '8px' }} /> Run New Analysis</>
          )}
        </button>
      </div>

      {!recommendations ? (
        <div className="empty-state">
          <BrainCircuit className="empty-state-icon" style={{ color: 'var(--accent-purple)' }} />
          <h3>No Analysis Found</h3>
          <p>Run the AI analysis to get therapy recommendations based on the latest Vitals and ABG data.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Oxygen Status & Targets */}
          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: '300px', background: 'var(--bg-secondary)', padding: '20px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)' }}>
              <h4 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', color: 'var(--text-primary)' }}>
                <Activity size={18} color="var(--status-warning)" /> Oxygen Status
              </h4>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Status</span>
                <span style={{ fontWeight: 600, color: recommendations.oxygen_status.is_hypoxemic ? 'var(--status-critical)' : 'var(--status-stable)' }}>
                  {recommendations.oxygen_status.is_hypoxemic ? `Hypoxemic (${recommendations.oxygen_status.severity})` : 'Stable'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Target SpO2</span>
                <span style={{ fontWeight: 600 }}>{recommendations.oxygen_status.target_spo2_min}% - {recommendations.oxygen_status.target_spo2_max}%</span>
              </div>
            </div>

            <div style={{ flex: 1, minWidth: '300px', background: 'var(--bg-secondary)', padding: '20px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)' }}>
              <h4 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', color: 'var(--text-primary)' }}>
                <AlertTriangle size={18} color="var(--status-critical)" /> NIV Assessment
              </h4>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Recommendation</span>
                <span style={{ fontWeight: 600, color: recommendations.niv_advice.recommended ? 'var(--status-critical)' : 'inherit' }}>
                  {recommendations.niv_advice.recommended ? 'NIV Indicated' : 'Not Currently Indicated'}
                </span>
              </div>
              <div>
                <span style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', display: 'block', marginBottom: '4px' }}>Rationale</span>
                <p style={{ fontSize: '0.875rem', lineHeight: 1.5 }}>{recommendations.niv_advice.reason}</p>
              </div>
            </div>
          </div>

          {/* Primary Recommendation */}
          <div style={{ background: 'var(--gradient-subtle)', padding: '24px', borderRadius: 'var(--radius-lg)', border: '1px solid var(--border-light)' }}>
            <h4 style={{ fontSize: '1.125rem', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
              <Wind size={20} color="var(--accent-primary)" /> Primary Device Recommendation
            </h4>
            
            <div style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}>
              <div style={{ background: 'var(--bg-surface)', padding: '16px', borderRadius: 'var(--radius-sm)', flex: 1, border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Recommended Device</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--accent-primary)' }}>{recommendations.device_recommendation.primary_device}</div>
              </div>
              <div style={{ background: 'var(--bg-surface)', padding: '16px', borderRadius: 'var(--radius-sm)', flex: 1, border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '4px' }}>FiO2 Setting</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{recommendations.device_recommendation.fio2}%</div>
              </div>
              <div style={{ background: 'var(--bg-surface)', padding: '16px', borderRadius: 'var(--radius-sm)', flex: 1, border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Flow Rate</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{recommendations.device_recommendation.flow_rate}</div>
              </div>
            </div>

            <div>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', display: 'block', marginBottom: '4px' }}>Clinical Rationale</span>
              <p style={{ background: 'var(--bg-surface)', padding: '12px', borderRadius: 'var(--radius-sm)', fontSize: '0.875rem', border: '1px solid var(--border)' }}>
                {recommendations.device_recommendation.rationale}
              </p>
            </div>
            
            <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end' }}>
              <button className="btn btn-primary" style={{ gap: '8px' }}>
                <CheckCircle size={16} /> Accept & Start Therapy
              </button>
            </div>
          </div>

          {/* Escalation Triggers */}
          {recommendations.escalation_triggers && recommendations.escalation_triggers.length > 0 && (
            <div>
              <h4 style={{ fontSize: '1rem', marginBottom: '16px', color: 'var(--text-primary)' }}>Monitor for Escalation (Red Flags)</h4>
              <ul style={{ listStyleType: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {recommendations.escalation_triggers.map((trigger, i) => (
                  <li key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', background: 'rgba(239, 68, 68, 0.05)', padding: '12px 16px', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
                    <AlertCircle size={16} color="var(--status-critical)" style={{ marginTop: '2px', flexShrink: 0 }} />
                    <span style={{ fontSize: '0.875rem', lineHeight: 1.5, color: 'var(--text-primary)' }}>{trigger}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

        </div>
      )}
    </div>
  );
};

export default TherapyTab;
