import React, { useState, useEffect } from 'react';
import { getTherapyRecommendations, startAIAnalysis, acceptTherapy } from '../../../api/therapy';
import api from '../../../api/axios';
import { getStaffList, postScheduleReassessment } from '../../../api/admin';
import { Activity, BrainCircuit, Wind, RefreshCw, CheckCircle, ShieldCheck, Clock, Edit3, AlertTriangle, Calendar } from 'lucide-react';
import toast from 'react-hot-toast';

// 4 standard devices
const DEVICES = [
  {
    id: 'nasal',
    name: 'Nasal Cannula',
    flow: '1–4 L/min',
    fio2: '24–36%',
    indication: 'Mild hypoxemia, SpO2 > 92%',
    color: '#10B981',
  },
  {
    id: 'venturi',
    name: 'Venturi Mask',
    flow: '4–12 L/min',
    fio2: '24–60% (controlled)',
    indication: 'COPD – precise FiO2 needed, SpO2 88–92%',
    color: '#3B82F6',
  },
  {
    id: 'hfnc',
    name: 'High-Flow Nasal Cannula (HFNC)',
    flow: '30–60 L/min',
    fio2: '21–100%',
    indication: 'Severe hypoxemia, pH < 7.35 or SpO2 < 88%',
    color: '#F59E0B',
  },
  {
    id: 'nrb',
    name: 'Non-Rebreather Mask',
    flow: '10–15 L/min',
    fio2: '60–90%',
    indication: 'Critical hypoxemia, SpO2 < 85%',
    color: '#EF4444',
  },
];

const TherapyTab = ({ patientId }) => {
  const [recommendations, setRecommendations] = useState(null);
  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [accepting, setAccepting] = useState(false);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [aiDevice, setAiDevice] = useState(null);
  const [showOverride, setShowOverride] = useState(false);
  const [overrideReason, setOverrideReason] = useState('');
  const [staffList, setStaffList] = useState([]);
  const [showSchedule, setShowSchedule] = useState(false);
  const [scheduleData, setScheduleData] = useState({ interval_minutes: '30', assigned_staff: '' });
  const [scheduling, setScheduling] = useState(false);

  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userRole = (user?.role || localStorage.getItem('role') || 'staff').toLowerCase();

  useEffect(() => {
    fetchData();
    if (userRole === 'doctor') loadStaff();
  }, [patientId]);

  const loadStaff = async () => {
    try {
      const { data } = await getStaffList();
      setStaffList(data);
    } catch (e) {
      console.error('Failed to load staff list:', e);
      toast.error('Could not load staff members');
    }
  };

  const fetchData = async () => {
    try {
      setLoading(true);
      const [patientRes, recRes] = await Promise.all([
        api.get(`/patients/${patientId}/`),
        getTherapyRecommendations(patientId)
      ]);
      setPatient(patientRes.data);

      const pending = (recRes.data?.recommendations || []).filter(r => r.status === 'pending');
      if (pending.length > 0) {
        const latest = pending[0];
        const content = latest.content;
        let device = null;

        // Detect AI device from content
        if (content.includes('Non-Rebreather')) device = 'nrb';
        else if (content.includes('HFNC') || content.includes('High-Flow')) device = 'hfnc';
        else if (content.includes('Venturi')) device = 'venturi';
        else device = 'nasal';

        setAiDevice(device);
        setSelectedDevice(device);
        setRecommendations({
          id: latest.id,
          content: latest.content,
          status: latest.status,
          created_at: latest.created_at,
        });
      } else {
        setRecommendations(null);
        setAiDevice(null);
      }
    } catch (error) {
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
        { loading: 'AI analyzing clinical data...', success: 'Analysis complete.', error: 'Need Vitals/ABG data first.' }
      );
      fetchData();
    } catch (e) {}
    finally { setAnalyzing(false); }
  };

  const handleConfirmTherapy = async () => {
    if (userRole !== 'doctor') { toast.error('Only doctors can approve therapy'); return; }
    setAccepting(true);
    const device = DEVICES.find(d => d.id === selectedDevice);
    const isOverride = selectedDevice !== aiDevice;

    try {
      if (isOverride && !overrideReason.trim()) {
        toast.error('Please provide an override reason');
        setAccepting(false);
        return;
      }
      await acceptTherapy(patientId, recommendations?.id, {
        selected_device: device?.name,
        flow_rate: device?.flow,
        is_override: isOverride,
        override_reason: overrideReason,
      });
      toast.success(`Therapy approved: ${device?.name}`);
      setShowOverride(false);
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to approve therapy');
    } finally {
      setAccepting(false);
    }
  };

  const handleScheduleReassessment = async (e) => {
    e.preventDefault();
    setScheduling(true);
    try {
      await postScheduleReassessment(patientId, {
        interval_minutes: parseInt(scheduleData.interval_minutes),
        assigned_staff: scheduleData.assigned_staff,
      });
      toast.success(`Reassessment scheduled in ${scheduleData.interval_minutes} minutes. Staff notified.`);
      setShowSchedule(false);
    } catch (error) {
      toast.error('Failed to schedule reassessment');
    } finally {
      setScheduling(false);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><RefreshCw className="spin" /></div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <BrainCircuit size={20} color="var(--accent-purple)" /> Respiratory Therapy Support
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>
              AI-driven device recommendation based on real-time vitals & ABG
            </p>
          </div>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button className="btn btn-primary"
              onClick={handleRunAnalysis}
              disabled={analyzing}
              style={{ background: 'var(--accent-purple)', borderColor: 'var(--accent-purple)' }}>
              {analyzing ? <RefreshCw size={16} className="spin" /> : <Activity size={16} />}
              <span style={{ marginLeft: '8px' }}>Run AI Analysis</span>
            </button>
            {userRole === 'doctor' && (
              <button className="btn btn-outline" onClick={() => setShowSchedule(s => !s)}>
                <Calendar size={16} /> Schedule Reassessment
              </button>
            )}
          </div>
        </div>

        {/* Reassessment Scheduler */}
        {showSchedule && userRole === 'doctor' && (
          <div style={{ marginTop: '20px', padding: '20px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)' }}>
            <h4 style={{ marginBottom: '16px', fontWeight: 600 }}>Schedule Staff Reassessment</h4>
            <form onSubmit={handleScheduleReassessment}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Reassessment Interval</label>
                  <select className="form-select" value={scheduleData.interval_minutes}
                    onChange={e => setScheduleData({ ...scheduleData, interval_minutes: e.target.value })} required>
                    <option value="30">30 minutes</option>
                    <option value="60">1 hour</option>
                    <option value="120">2 hours</option>
                    <option value="240">4 hours</option>
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Assign To (Staff)</label>
                  <select className="form-select" value={scheduleData.assigned_staff}
                    onChange={e => setScheduleData({ ...scheduleData, assigned_staff: e.target.value })} required>
                    <option value="">Select Staff Member</option>
                    {staffList.map(s => (
                      <option key={s.id} value={s.id}>{s.name} ({s.department})</option>
                    ))}
                  </select>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '16px' }}>
                <button type="button" className="btn btn-ghost" onClick={() => setShowSchedule(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={scheduling}>
                  {scheduling ? 'Scheduling...' : 'Schedule & Notify Staff'}
                </button>
              </div>
            </form>
          </div>
        )}
      </div>

      {/* Active approved therapy banner */}
      {patient?.current_device && (
        <div style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid var(--status-stable)', padding: '20px', borderRadius: 'var(--radius-lg)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ background: 'var(--status-stable)', padding: '12px', borderRadius: '50%', color: 'white' }}>
              <ShieldCheck size={24} />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--status-stable)', fontWeight: 700 }}>Active Approved Therapy</div>
              <h4 style={{ fontSize: '1.5rem', fontWeight: 800, margin: '4px 0' }}>{patient.current_device}</h4>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                Flow: <strong>{patient.current_flow_rate}</strong>
                {patient.therapy_approved_at && <> • Started: {new Date(patient.therapy_approved_at).toLocaleString()}</>}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 4-Device selection grid */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h4 style={{ fontSize: '1.125rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Wind size={18} color="var(--accent-primary)" /> Oxygen Delivery Device Selection
          </h4>
          {aiDevice && (
            <span style={{ fontSize: '0.8125rem', color: 'var(--accent-purple)', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <BrainCircuit size={14} /> AI Recommends: <strong style={{ marginLeft: '4px' }}>{DEVICES.find(d => d.id === aiDevice)?.name}</strong>
            </span>
          )}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: '16px' }}>
          {DEVICES.map(device => {
            const isAI = device.id === aiDevice;
            const isSelected = device.id === selectedDevice;
            return (
              <div
                key={device.id}
                onClick={() => userRole === 'doctor' && setSelectedDevice(device.id)}
                style={{
                  padding: '20px',
                  borderRadius: 'var(--radius-lg)',
                  border: isSelected ? `2px solid ${device.color}` : '2px solid var(--border)',
                  background: isSelected ? `${device.color}15` : 'var(--bg-secondary)',
                  cursor: userRole === 'doctor' ? 'pointer' : 'default',
                  position: 'relative',
                  transition: 'all 0.2s ease',
                }}
              >
                {isAI && (
                  <div style={{ position: 'absolute', top: '10px', right: '10px', background: 'var(--accent-purple)', color: 'white', padding: '3px 10px', borderRadius: '20px', fontSize: '0.7rem', fontWeight: 700 }}>
                    AI RECOMMENDED
                  </div>
                )}
                {isSelected && (
                  <CheckCircle size={18} color={device.color} style={{ position: 'absolute', top: '10px', left: '10px' }} />
                )}
                <div style={{ paddingLeft: isSelected ? '24px' : 0 }}>
                  <div style={{ fontWeight: 700, fontSize: '1rem', color: device.color, marginBottom: '6px' }}>{device.name}</div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                    Flow: <strong>{device.flow}</strong> · FiO2: <strong>{device.fio2}</strong>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '6px' }}>{device.indication}</div>
                </div>
              </div>
            );
          })}
        </div>

        {userRole === 'doctor' && recommendations && selectedDevice !== aiDevice && (
          <div style={{ marginTop: '16px', padding: '14px', background: 'rgba(245,158,11,0.1)', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(245,158,11,0.3)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px', fontWeight: 600, color: 'var(--status-warning)' }}>
              <AlertTriangle size={16} /> Override — Reason Required
            </div>
            <textarea
              className="form-input"
              rows={2}
              placeholder="Explain why you are overriding the AI recommendation..."
              value={overrideReason}
              onChange={e => setOverrideReason(e.target.value)}
              style={{ width: '100%', resize: 'vertical' }}
            />
          </div>
        )}

        {userRole === 'doctor' && recommendations && (
          <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
            <button className="btn btn-primary" onClick={handleConfirmTherapy} disabled={accepting}
              style={{ gap: '8px' }}>
              {accepting ? <RefreshCw size={16} className="spin" /> : <CheckCircle size={16} />}
              {selectedDevice !== aiDevice ? 'Override & Apply Therapy' : 'Accept & Start Therapy'}
            </button>
          </div>
        )}

        {userRole !== 'doctor' && (
          <div style={{ marginTop: '16px', padding: '10px', background: 'rgba(245,158,11,0.1)', borderRadius: 'var(--radius-sm)', fontSize: '0.75rem', color: 'var(--status-warning)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={14} /> Waiting for doctor's clinical approval
          </div>
        )}

        {!recommendations && (
          <div className="empty-state" style={{ marginTop: '20px' }}>
            <BrainCircuit className="empty-state-icon" style={{ color: 'var(--accent-purple)' }} />
            <h3>Run AI Analysis First</h3>
            <p>AI will recommend a device based on current vitals and ABG values.</p>
          </div>
        )}
      </div>

    </div>
  );
};

export default TherapyTab;
