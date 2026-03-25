import React, { useState, useEffect } from 'react';
import { getAIRisk, getTrendAnalysis } from '../../../api/patients';
import { BrainCircuit, TrendingUp, AlertTriangle, CheckCircle, BarChart3, Info } from 'lucide-react';
import toast from 'react-hot-toast';

const AIAnalysisTab = ({ patientId }) => {
  const [analysis, setAnalysis] = useState(null);
  const [trendData, setTrendData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showTrends, setShowTrends] = useState(false);

  useEffect(() => {
    fetchAIData();
    fetchTrendData();
  }, [patientId]);

  const fetchAIData = async () => {
    try {
      const { data } = await getAIRisk(patientId);
      setAnalysis(data);
    } catch (error) {
      toast.error('Failed to load AI risk analysis');
      setAnalysis(null);
    } finally {
      setLoading(false);
    }
  };

  const fetchTrendData = async () => {
    try {
      const { data } = await getTrendAnalysis(patientId);
      setTrendData(data);
    } catch {
      toast.error('Failed to load trend analysis');
    }
    setShowTrends(true);
  };

  const getRiskColor = (risk) => {
    if (!risk) return 'var(--text-muted)';
    const r = risk.toUpperCase();
    if (r === 'HIGH') return '#EF4444';
    if (r === 'MODERATE') return '#F59E0B';
    return '#10B981';
  };

  const getRiskBg = (risk) => {
    if (!risk) return 'transparent';
    const r = risk.toUpperCase();
    if (r === 'HIGH') return 'rgba(239, 68, 68, 0.1)';
    if (r === 'MODERATE') return 'rgba(245, 158, 11, 0.1)';
    return 'rgba(16, 185, 129, 0.1)';
  };

  const getSeverityColor = (sev) => {
    if (sev === 'high') return 'var(--status-critical)';
    if (sev === 'moderate') return 'var(--status-warning)';
    return 'var(--status-stable)';
  };

  const getTrendColor = (trend) => {
    if (!trend) return 'var(--text-muted)';
    const t = trend.toLowerCase();
    if (['rising', 'dropping', 'worsening', 'declining'].includes(t)) return '#EF4444';
    if (['unstable'].includes(t)) return '#F59E0B';
    return '#10B981';
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  if (!analysis || (analysis.confidence_score === 0 && analysis.message)) {
    return (
      <div className="card">
        <div className="empty-state">
          <BrainCircuit className="empty-state-icon" style={{ color: 'var(--accent-purple)' }} />
          <h3>No AI Analysis Available</h3>
          <p>Run the AI analysis from the therapy tab to get risk predictions for this patient.</p>
        </div>
      </div>
    );
  }

  const riskColor = getRiskColor(analysis.risk_level);
  const riskBg = getRiskBg(analysis.risk_level);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      {/* Risk Level Header */}
      <div className="card" style={{ textAlign: 'center', padding: '40px', background: riskBg, border: `1px solid ${riskColor}30` }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px', marginBottom: '16px' }}>
          <BrainCircuit size={28} color={riskColor} />
          <h2 style={{ fontSize: '2rem', fontWeight: 800, color: riskColor, margin: 0, letterSpacing: '0.05em' }}>
            {analysis.risk_level} RISK
          </h2>
        </div>

        {/* Confidence Score Bar */}
        <div style={{ maxWidth: '400px', margin: '0 auto' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Confidence Score</span>
            <span style={{ fontSize: '0.875rem', fontWeight: 700, color: riskColor }}>{analysis.confidence_score}%</span>
          </div>
          <div style={{ height: '10px', background: 'var(--bg-secondary)', borderRadius: '5px', overflow: 'hidden' }}>
            <div style={{ height: '100%', width: `${analysis.confidence_score}%`, background: riskColor, borderRadius: '5px', transition: 'width 1s ease-in-out' }}></div>
          </div>
        </div>
      </div>

      {/* Key Factors */}
      {analysis.key_factors && analysis.key_factors.length > 0 && (
        <div className="card">
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertTriangle size={18} color="var(--status-warning)" /> Key Risk Factors
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {analysis.key_factors.map((kf, idx) => (
              <div key={idx} style={{ 
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '16px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)',
                borderLeft: `4px solid ${getSeverityColor(kf.severity)}`
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: getSeverityColor(kf.severity) }}></div>
                  <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{kf.factor || kf}</span>
                </div>
                {kf.value && (
                  <span style={{ fontWeight: 700, color: getSeverityColor(kf.severity), fontSize: '0.9375rem' }}>{kf.value}</span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Trend Analysis */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BarChart3 size={18} color="var(--accent-primary)" /> Trend Analysis
          </h3>
        </div>

        {trendData ? (
          <div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px', marginBottom: '20px' }}>
              {[
                { label: 'Overall', value: trendData.overall_trend || 'Stable' },
                { label: 'SpO2', value: trendData.spo2_trend || 'Stable' },
                { label: 'PaCO2', value: trendData.paco2_trend || 'Normal' },
                { label: 'pH', value: trendData.ph_trend || 'Normal' }
              ].map((item, idx) => (
                <div key={idx} style={{ background: 'var(--bg-secondary)', padding: '16px', borderRadius: 'var(--radius-sm)', textAlign: 'center' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '6px' }}>{item.label}</div>
                  <div style={{ fontWeight: 700, color: getTrendColor(item.value), fontSize: '0.9375rem' }}>{item.value}</div>
                </div>
              ))}
            </div>
            {trendData.summary && (
              <div style={{ padding: '16px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <Info size={18} color="var(--accent-primary)" style={{ flexShrink: 0, marginTop: '2px' }} />
                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.9375rem', margin: 0 }}>{trendData.summary}</p>
              </div>
            )}
          </div>
        ) : (
          <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>
             <div className="spinner" style={{ margin: '0 auto 10px' }}></div>
             Loading trend analysis...
          </div>
        )}
      </div>
    </div>
  );
};

export default AIAnalysisTab;
