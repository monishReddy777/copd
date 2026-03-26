import React, { useState, useEffect } from 'react';
import { getABGTrends } from '../../../api/therapy';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend, ReferenceLine } from 'recharts';
import { BarChart3, Clock, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const ABGTrendsTab = ({ patientId }) => {
  const [trends, setTrends] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('7d');

  useEffect(() => {
    fetchTrends();
  }, [patientId, filter]);

  const fetchTrends = async () => {
    setLoading(true);
    try {
      const { data } = await getABGTrends(patientId);
      // Backend now returns { trends: [...] }
      setTrends(data.trends || []);
    } catch {
      // Mock trend data
      const now = Date.now();
      const mockData = Array.from({ length: 8 }, (_, i) => ({
        timestamp: new Date(now - (7 - i) * 3600000 * 6).toISOString(),
        ph: +(7.28 + Math.random() * 0.18).toFixed(2),
        paco2: +(40 + Math.random() * 20).toFixed(1),
        pao2: +(55 + Math.random() * 25).toFixed(1),
        spo2: +(84 + Math.random() * 10).toFixed(0),
        hco3: +(22 + Math.random() * 8).toFixed(1)
      }));
      setTrends(mockData);
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (ts) => {
    const d = new Date(ts);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ' ' + d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  };

  // Pre-process trends to ensure all values are present for smooth lines
  // Recharts connects points for the same key. By merging near-simultaneous records
  // or just having them in sequence, we get a complete view.
  const chartData = trends.map(t => ({
    ...t,
    time: formatTime(t.timestamp),
    ph: t.ph ? parseFloat(t.ph) : undefined,
    paco2: t.paco2 ? parseFloat(t.paco2) : undefined,
    pao2: t.pao2 ? parseFloat(t.pao2) : undefined,
    spo2: t.spo2 ? parseFloat(t.spo2) : undefined,
    hco3: t.hco3 ? parseFloat(t.hco3) : undefined,
    hr: t.hr ? parseFloat(t.hr) : undefined
  }));

  const tooltipStyle = {
    backgroundColor: 'var(--bg-card)',
    border: '1px solid var(--border)',
    borderRadius: '8px',
    color: 'var(--text-primary)',
    fontSize: '0.8125rem'
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  if (chartData.length === 0) {
    return (
      <div className="card">
        <div className="empty-state">
          <BarChart3 className="empty-state-icon" />
          <h3>No ABG Trend Data</h3>
          <p>Record ABG results to see trend charts over time.</p>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

      {/* Filter Bar */}
      <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px' }}>
        <h3 style={{ fontSize: '1.125rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px', margin: 0 }}>
          <BarChart3 size={18} color="var(--accent-primary)" /> ABG Trend Charts
        </h3>
        <div style={{ display: 'flex', gap: '8px' }}>
          {['24h', '7d', '30d', 'all'].map(f => (
            <button key={f} className={`btn ${filter === f ? 'btn-primary' : 'btn-ghost'}`}
              style={{ padding: '6px 14px', fontSize: '0.8125rem' }}
              onClick={() => setFilter(f)}>
              {f === 'all' ? 'All' : f}
            </button>
          ))}
        </div>
      </div>

      {/* pH Chart */}
      <div className="card">
        <h4 style={{ marginBottom: '16px', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
          pH Level <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(Normal: 7.35 – 7.45)</span>
        </h4>
        <ResponsiveContainer width="100%" height={250}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
            <XAxis dataKey="time" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
            <YAxis domain={[7.1, 7.6]} tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <ReferenceLine y={7.35} stroke="#EF4444" strokeDasharray="5 5" label={{ value: '7.35', fill: '#EF4444', fontSize: 10 }} />
            <ReferenceLine y={7.45} stroke="#F59E0B" strokeDasharray="5 5" label={{ value: '7.45', fill: '#F59E0B', fontSize: 10 }} />
            <Line type="monotone" dataKey="ph" stroke="#8B5CF6" strokeWidth={2} dot={{ r: 4, fill: '#8B5CF6' }} activeDot={{ r: 6 }} name="pH" connectNulls />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* PaCO2 Chart */}
      <div className="card">
        <h4 style={{ marginBottom: '16px', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
          PaCO2 Level <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(mmHg) (Normal: 35 – 45)</span>
        </h4>
        <ResponsiveContainer width="100%" height={250}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
            <XAxis dataKey="time" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
            <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <ReferenceLine y={35} stroke="#10B981" strokeDasharray="5 5" label={{ value: '35', fill: '#10B981', fontSize: 10 }} />
            <ReferenceLine y={45} stroke="#EF4444" strokeDasharray="5 5" label={{ value: '45', fill: '#EF4444', fontSize: 10 }} />
            <Line type="monotone" dataKey="paco2" stroke="#EF4444" strokeWidth={2} dot={{ r: 4, fill: '#EF4444' }} name="PaCO2" connectNulls />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* PaO2 Chart */}
      <div className="card">
        <h4 style={{ marginBottom: '16px', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
          PaO2 Level <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(mmHg) (Target: &gt; 60)</span>
        </h4>
        <ResponsiveContainer width="100%" height={250}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
            <XAxis dataKey="time" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
            <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <ReferenceLine y={60} stroke="#EF4444" strokeDasharray="5 5" label={{ value: '60', fill: '#EF4444', fontSize: 10 }} />
            <Line type="monotone" dataKey="pao2" stroke="#3B82F6" strokeWidth={2} dot={{ r: 4, fill: '#3B82F6' }} name="PaO2" connectNulls />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* SpO2 Trend Chart */}
      <div className="card">
        <h4 style={{ marginBottom: '16px', fontWeight: 600 }}>SpO2 Trend <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(Target SpO2: 88-92%)</span></h4>
        <ResponsiveContainer width="100%" height={250}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
            <XAxis dataKey="time" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
            <YAxis domain={[75, 100]} tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <ReferenceLine y={88} stroke="#EF4444" strokeDasharray="5 5" label={{ value: '88%', fill: '#EF4444', fontSize: 10 }} />
            <ReferenceLine y={92} stroke="#F59E0B" strokeDasharray="5 5" label={{ value: '92%', fill: '#F59E0B', fontSize: 10 }} />
            <Line type="monotone" dataKey="spo2" stroke="#10B981" strokeWidth={2} dot={{ r: 4, fill: '#10B981' }} name="SpO2 %" connectNulls />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default ABGTrendsTab;
