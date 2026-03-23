import React, { useState, useEffect } from 'react';
import { getOxygenStatus } from '../../../api/therapy';
import { getPatientDetail } from '../../../api/patients';
import { Wind, Gauge, Settings, AlertCircle, CheckCircle, TrendingUp, TrendingDown } from 'lucide-react';
import toast from 'react-hot-toast';

const OxygenTab = ({ patientId }) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchOxygenData();
  }, [patientId]);

  const fetchOxygenData = async () => {
    try {
      const { data: res } = await getOxygenStatus(patientId);
      setData(res);
    } catch (error) {
      toast.error('Failed to load oxygen status');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  const getObservation = (spo2, target) => {
    if (!spo2 || spo2 === '--') return 'No SpO2 data available. Please check monitoring device.';
    const val = parseInt(spo2);
    let targetMin = 88, targetMax = 92;
    if (target && target.includes('-')) {
      const parts = target.replace('%', '').split('-');
      targetMin = parseInt(parts[0].trim());
      targetMax = parseInt(parts[1].trim());
    }
    if (val < targetMin) return 'Patient is currently below target range. Consider increasing FiO2 or checking device fit.';
    if (val > targetMax) return 'Patient SpO2 is above target range. Consider decreasing flow to avoid oxygen toxicity in COPD.';
    return 'Patient SpO2 is within target range. Continue current oxygen therapy settings.';
  };

  const getStatusColor = (spo2, target) => {
    if (!spo2) return 'var(--text-muted)';
    const val = parseInt(spo2);
    let targetMin = 88;
    if (target && target.includes('-')) {
      targetMin = parseInt(target.replace('%', '').split('-')[0].trim());
    }
    if (val < targetMin - 3) return 'var(--status-critical)';
    if (val < targetMin) return 'var(--status-warning)';
    return 'var(--status-stable)';
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;
  if (!data) return <div className="empty-state"><Wind className="empty-state-icon" /><h3>No oxygen data available</h3></div>;

  const spo2Val = data.spo2 || data.current_spo2 || '--';
  const targetRange = data.target_spo2 || '88-92';
  const observation = data.observation || getObservation(spo2Val, targetRange);
  const statusColor = getStatusColor(spo2Val, targetRange);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

      {/* SpO2 Gauge Card */}
      <div className="card" style={{ textAlign: 'center', padding: '40px' }}>
        <div style={{ position: 'relative', width: '200px', height: '200px', margin: '0 auto 24px', borderRadius: '50%', background: `conic-gradient(${statusColor} ${(parseInt(spo2Val) || 0) * 3.6}deg, var(--bg-secondary) 0deg)`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ width: '160px', height: '160px', borderRadius: '50%', background: 'var(--bg-card)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ fontSize: '3rem', fontWeight: 800, color: statusColor, lineHeight: 1 }}>{spo2Val}</div>
            <div style={{ fontSize: '1.25rem', color: 'var(--text-muted)', fontWeight: 500 }}>% SpO2</div>
          </div>
        </div>
        <div style={{ fontSize: '1rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
          Target Range: <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{targetRange}%</span>
        </div>
      </div>

      {/* Device & Flow Info */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px' }}>
        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginBottom: '12px', color: 'var(--text-secondary)' }}>
            <Settings size={16} /> Current Device
          </div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--accent-primary)' }}>{data.device || 'Not Set'}</div>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginBottom: '12px', color: 'var(--text-secondary)' }}>
            <Wind size={16} /> Flow Rate
          </div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>{data.flow_rate || data.flow || '--'}</div>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginBottom: '12px', color: 'var(--text-secondary)' }}>
            <Gauge size={16} /> FiO2
          </div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>{data.fio2 || '--'}%</div>
        </div>
      </div>

      {/* Clinical Observation */}
      <div className="card" style={{ position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, left: 0, bottom: 0, width: '4px', background: statusColor }}></div>
        <div style={{ paddingLeft: '16px' }}>
          <h4 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px', fontSize: '1rem', fontWeight: 600 }}>
            {parseInt(spo2Val) < 88 ? <AlertCircle size={18} color="var(--status-critical)" /> : <CheckCircle size={18} color="var(--status-stable)" />}
            Clinical Observation
          </h4>
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.9375rem' }}>{observation}</p>
        </div>
      </div>
    </div>
  );
};

export default OxygenTab;
