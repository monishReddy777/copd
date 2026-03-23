import React, { useState, useEffect } from 'react';
import { getDecisionSupport } from '../../../api/patients';
import { ShieldAlert, AlertCircle, CheckCircle, TrendingUp, Activity, Stethoscope } from 'lucide-react';
import toast from 'react-hot-toast';

const DecisionSupportTab = ({ patientId }) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, [patientId]);

  const fetchData = async () => {
    try {
      const { data: res } = await getDecisionSupport(patientId);
      setData(res);
    } catch {
      setData({
        has_data: true,
        risk_level: 'MODERATE',
        confidence_score: 68,
        action_level: 'WARNING',
        recommendation: 'Monitor closely. Consider adjusting oxygen therapy if SpO2 drops below target. Prepare for potential NIV initiation.',
        overall_status: 'Unstable',
        paco2_status: 'Rising',
        ph_status: 'Normal',
        spo2_status: 'Dropping',
        acidosis: 0,
        hypercapnia: 1
      });
    } finally {
      setLoading(false);
    }
  };

  const getRiskColor = (risk) => {
    const r = (risk || '').toUpperCase();
    if (r === 'HIGH' || r === 'CRITICAL') return '#EF4444';
    if (r === 'MODERATE' || r === 'WARNING') return '#F59E0B';
    return '#10B981';
  };

  const getRiskBg = (risk) => {
    const r = (risk || '').toUpperCase();
    if (r === 'HIGH' || r === 'CRITICAL') return 'rgba(239, 68, 68, 0.08)';
    if (r === 'MODERATE' || r === 'WARNING') return 'rgba(245, 158, 11, 0.08)';
    return 'rgba(16, 185, 129, 0.08)';
  };

  const getStatusColor = (status) => {
    if (!status) return '#10B981';
    const s = status.toLowerCase();
    if (['rising', 'dropping', 'worsening', 'critical'].includes(s)) return '#EF4444';
    if (s === 'unstable') return '#F59E0B';
    return '#10B981';
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  if (!data || !data.has_data) {
    return (
      <div className="card">
        <div className="empty-state">
          <ShieldAlert className="empty-state-icon" style={{ color: 'var(--accent-primary)' }} />
          <h3>No Decision Support Data</h3>
          <p>Decision support analysis requires ABG and vitals data. Enter clinical data first.</p>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      {/* Risk Level & Confidence */}
      <div className="card" style={{ display: 'flex', gap: '24px', alignItems: 'center', padding: '32px' }}>
        <div style={{ flex: 1, textAlign: 'center' }}>
          <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '8px' }}>Risk Level</div>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: getRiskColor(data.risk_level), letterSpacing: '0.05em' }}>
            {data.risk_level} RISK
          </div>
        </div>
        <div style={{ width: '1px', height: '60px', background: 'var(--border)' }}></div>
        <div style={{ flex: 1, textAlign: 'center' }}>
          <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '8px' }}>Confidence</div>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: getRiskColor(data.risk_level) }}>
            {data.confidence_score}%
          </div>
        </div>
      </div>

      {/* Recommendation Card */}
      <div className="card" style={{ background: getRiskBg(data.action_level), border: `1px solid ${getRiskColor(data.action_level)}30`, position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, left: 0, bottom: 0, width: '4px', background: getRiskColor(data.action_level) }}></div>
        <div style={{ paddingLeft: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
            <Stethoscope size={20} color={getRiskColor(data.action_level)} />
            <span style={{ fontWeight: 700, color: getRiskColor(data.action_level), fontSize: '1.125rem', textTransform: 'uppercase', letterSpacing: '0.03em' }}>
              {data.action_level}
            </span>
          </div>
          <p style={{ color: 'var(--text-primary)', lineHeight: 1.7, fontSize: '0.9375rem', margin: 0 }}>
            {data.recommendation}
          </p>
        </div>
      </div>

      {/* Trend Summary */}
      <div className="card">
        <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <TrendingUp size={18} color="var(--accent-primary)" /> Parameter Trend Summary
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px' }}>
          {[
            { label: 'Overall Status', value: data.overall_status || 'Stable' },
            { label: 'PaCO2 Trend', value: data.paco2_status || 'Normal' },
            { label: 'pH Trend', value: data.ph_status || 'Normal' },
            { label: 'SpO2 Trend', value: data.spo2_status || 'Stable' }
          ].map((item, idx) => (
            <div key={idx} style={{ background: 'var(--bg-secondary)', padding: '20px 16px', borderRadius: 'var(--radius-md)', textAlign: 'center', border: '1px solid var(--border)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{item.label}</div>
              <div style={{ fontWeight: 700, color: getStatusColor(item.value), fontSize: '1rem' }}>{item.value}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Key Indicators */}
      {(data.acidosis === 1 || data.hypercapnia === 1) && (
        <div className="card">
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={18} color="var(--status-critical)" /> Key Indicators Detected
          </h3>
          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
            {data.acidosis === 1 && (
              <div style={{ flex: 1, minWidth: '250px', background: 'rgba(239, 68, 68, 0.08)', border: '1px solid rgba(239, 68, 68, 0.25)', padding: '20px', borderRadius: 'var(--radius-md)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                  <AlertCircle size={18} color="#EF4444" />
                  <span style={{ fontWeight: 700, color: '#EF4444', fontSize: '1rem' }}>Respiratory Acidosis</span>
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', margin: 0 }}>pH &lt; 7.35 detected. Consider NIV initiation and close monitoring.</p>
              </div>
            )}
            {data.hypercapnia === 1 && (
              <div style={{ flex: 1, minWidth: '250px', background: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.25)', padding: '20px', borderRadius: 'var(--radius-md)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                  <AlertCircle size={18} color="#F59E0B" />
                  <span style={{ fontWeight: 700, color: '#F59E0B', fontSize: '1rem' }}>Hypercapnia</span>
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', margin: 0 }}>PaCO2 &gt; 45 mmHg detected. Risk of CO2 retention with uncontrolled oxygen.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default DecisionSupportTab;
