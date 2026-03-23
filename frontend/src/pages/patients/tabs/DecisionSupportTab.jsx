import React, { useState, useEffect } from 'react';
import { getDecisionSupport } from '../../../api/patients';
import { acceptTherapy } from '../../../api/therapy';
import { ShieldAlert, AlertCircle, CheckCircle, TrendingUp, Zap, Stethoscope } from 'lucide-react';
import toast from 'react-hot-toast';

const DecisionSupportTab = ({ patientId, onApproval }) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [approving, setApproving] = useState(null);
  const user = JSON.parse(localStorage.getItem('user'));
  const role = user?.role || localStorage.getItem('role') || 'staff';

  useEffect(() => {
    fetchData();
  }, [patientId]);

  const fetchData = async () => {
    try {
      const { data: res } = await getDecisionSupport(patientId);
      setData(res);
    } catch {
      // Fallback/Mock
      setData({
        has_data: true,
        risk_level: 'MODERATE',
        confidence_score: 68,
        action_level: 'WARNING',
        recommendation: 'Monitor closely. Consider adjusting oxygen therapy if SpO2 drops below target.',
        overall_status: 'Unstable',
        paco2_status: 'Rising',
        ph_status: 'Normal',
        spo2_status: 'Dropping',
        acidosis: 0,
        hypercapnia: 1,
        recommendations: []
      });
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (recId) => {
    setApproving(recId);
    try {
      await acceptTherapy(patientId, recId);
      toast.success('Therapy approved successfully');
      if (onApproval) onApproval();
      fetchData();
    } catch (error) {
      toast.error('Failed to approve therapy');
    } finally {
      setApproving(null);
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
    if (['rising', 'dropping', 'worsening', 'critical', 'low'].includes(s)) return '#EF4444';
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

  const allPending = (data.recommendations || []).filter(r => r.status === 'pending');
  const pendingRecs = allPending.length > 0 ? [allPending[0]] : [];
  const pastRecs = (data.recommendations || []).filter(r => r.status !== 'pending');

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

      {/* AI Therapy Decisions (Pending Approval) */}
      {pendingRecs.length > 0 && (
        <div className="card" style={{ background: 'linear-gradient(135deg, var(--bg-card) 0%, var(--bg-secondary) 100%)', border: '1px solid var(--accent-primary)' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Zap size={20} color="var(--accent-primary)" fill="var(--accent-primary)" /> AI Therapy Decision
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {pendingRecs.map((rec) => (
              <div key={rec.id} style={{ padding: '20px', background: 'var(--bg-card)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Recommendation Details:</div>
                  <span className="badge badge-warning" style={{ fontSize: '0.6875rem' }}>PENDING DOCTOR APPROVAL</span>
                </div>
                <p style={{ fontSize: '0.9375rem', color: 'var(--text-secondary)', margin: '0 0 20px 0', lineHeight: 1.6 }}>
                  {rec.content}
                </p>
                {role === 'doctor' && (
                  <button 
                    className="btn btn-primary" 
                    onClick={() => handleApprove(rec.id)}
                    disabled={approving === rec.id}
                    style={{ width: '100%', justifyContent: 'center' }}
                  >
                    {approving === rec.id ? 'Approving...' : 'Approve & Apply Therapy'}
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Primary Recommendation summary if no pending */}
      {pendingRecs.length === 0 && (
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
      )}

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

      {/* Past Decisions History */}
      {pastRecs.length > 0 && (
        <div className="card">
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckCircle size={18} color="#10B981" /> Decision History
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {pastRecs.map((rec) => (
              <div key={rec.id} className="alert-card stable" style={{ margin: 0, opacity: 0.8 }}>
                <div className="alert-icon green"><CheckCircle size={16} /></div>
                <div className="alert-content">
                  <h4 style={{ fontSize: '0.875rem', fontWeight: 600 }}>Approved On {new Date(rec.created_at).toLocaleDateString()}</h4>
                  <p style={{ fontSize: '0.8125rem' }}>{rec.content}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default DecisionSupportTab;
