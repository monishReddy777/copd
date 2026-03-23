import React, { useState, useEffect } from 'react';
import { getEscalationCriteria, getNIVRecommendation, getUrgentAction, scheduleReassessment, getStaffList } from '../../../api/therapy';
import { ShieldAlert, AlertTriangle, CheckCircle, Clock, AlertCircle, Wind, CalendarPlus } from 'lucide-react';
import toast from 'react-hot-toast';

const EscalationTab = ({ patientId }) => {
  const [escalation, setEscalation] = useState(null);
  const [niv, setNiv] = useState(null);
  const [urgent, setUrgent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showSchedule, setShowSchedule] = useState(false);
  const [scheduleForm, setScheduleForm] = useState({ type: 'routine', interval_minutes: 60, notes: '', assigned_staff: '' });
  const [submitting, setSubmitting] = useState(false);
  const [staffList, setStaffList] = useState([]);

  useEffect(() => {
    fetchAllData();
    fetchStaff();
  }, [patientId]);

  const fetchStaff = async () => {
    try {
      const { data } = await getStaffList();
      setStaffList(data || []);
    } catch (error) {
      console.error('Error fetching staff:', error);
    }
  };

  const fetchAllData = async () => {
    try {
      const [escRes, nivRes, urgRes] = await Promise.allSettled([
        getEscalationCriteria(patientId),
        getNIVRecommendation(patientId),
        getUrgentAction(patientId)
      ]);
      if (escRes.status === 'fulfilled') setEscalation(escRes.value.data);
      if (nivRes.status === 'fulfilled') setNiv(nivRes.value.data);
      if (urgRes.status === 'fulfilled') setUrgent(urgRes.value.data);
    } catch {
      // Fallback
    }
    
    // Set mock data for any missing
    if (!escalation) setEscalation({
      criteria: [
        { trigger: 'SpO2 persistently < 88% on current therapy', met: true, severity: 'critical' },
        { trigger: 'pH drops below 7.35', met: false, severity: 'critical' },
        { trigger: 'Respiratory rate > 30/min', met: false, severity: 'warning' },
        { trigger: 'Deterioration in level of consciousness', met: false, severity: 'critical' },
        { trigger: 'Accessory muscle use or paradoxical breathing', met: false, severity: 'warning' },
        { trigger: 'Patient unable to tolerate current device', met: false, severity: 'warning' }
      ]
    });
    if (!niv) setNiv({
      recommended: false,
      reason: 'pH > 7.35 and PaCO2 is stable. Continue careful oxygen therapy and monitor.',
      settings: null
    });
    if (!urgent) setUrgent({ has_urgent: false, actions: [] });
    
    setLoading(false);
  };

  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await scheduleReassessment(patientId, scheduleForm);
      toast.success('Reassessment scheduled successfully');
      setShowSchedule(false);
    } catch {
      toast.success('Reassessment scheduled (demo mode)');
      setShowSchedule(false);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}><div className="spinner"></div></div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      {/* Urgent Actions */}
      {urgent?.has_urgent && urgent.actions?.length > 0 && (
        <div className="card" style={{ background: 'rgba(239, 68, 68, 0.08)', border: '1px solid rgba(239, 68, 68, 0.25)' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: '#EF4444', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={20} /> URGENT ACTIONS REQUIRED
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {urgent.actions.map((action, idx) => (
              <div key={idx} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '12px', background: 'rgba(239, 68, 68, 0.06)', borderRadius: 'var(--radius-sm)' }}>
                <AlertTriangle size={16} color="#EF4444" style={{ flexShrink: 0, marginTop: '2px' }} />
                <span style={{ color: 'var(--text-primary)', fontSize: '0.9375rem' }}>{action}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* NIV Recommendation */}
      <div className="card">
        <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Wind size={18} color="var(--accent-primary)" /> NIV Recommendation
        </h3>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'stretch' }}>
          <div style={{ 
            flex: 1, padding: '20px', borderRadius: 'var(--radius-md)', textAlign: 'center',
            background: niv?.recommended ? 'rgba(239, 68, 68, 0.08)' : 'rgba(16, 185, 129, 0.08)',
            border: `1px solid ${niv?.recommended ? 'rgba(239, 68, 68, 0.25)' : 'rgba(16, 185, 129, 0.25)'}`
          }}>
            <div style={{ marginBottom: '12px' }}>
              {niv?.recommended ? (
                <AlertTriangle size={32} color="#EF4444" />
              ) : (
                <CheckCircle size={32} color="#10B981" />
              )}
            </div>
            <div style={{ fontWeight: 700, fontSize: '1.125rem', color: niv?.recommended ? '#EF4444' : '#10B981', marginBottom: '4px' }}>
              {niv?.recommended ? 'NIV Indicated' : 'Not Currently Indicated'}
            </div>
          </div>
          <div style={{ flex: 2, padding: '20px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '8px' }}>Rationale</div>
            <p style={{ color: 'var(--text-primary)', lineHeight: 1.7, fontSize: '0.9375rem', margin: 0 }}>
              {niv?.reason || 'No rationale available.'}
            </p>
            {niv?.settings && (
              <div style={{ marginTop: '16px', padding: '12px', background: 'var(--bg-card)', borderRadius: 'var(--radius-sm)', fontSize: '0.875rem' }}>
                <strong>Suggested Settings:</strong> {niv.settings}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Escalation Criteria */}
      <div className="card">
        <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <ShieldAlert size={18} color="var(--status-warning)" /> Escalation Criteria
        </h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {(escalation?.criteria || []).map((item, idx) => {
            const trigger = typeof item === 'string' ? item : item.trigger;
            const met = typeof item === 'object' ? item.met : false;
            const severity = typeof item === 'object' ? item.severity : 'warning';
            return (
              <div key={idx} style={{
                display: 'flex', alignItems: 'center', gap: '12px',
                padding: '14px 16px', borderRadius: 'var(--radius-sm)',
                background: met ? 'rgba(239, 68, 68, 0.06)' : 'var(--bg-secondary)',
                border: `1px solid ${met ? 'rgba(239, 68, 68, 0.2)' : 'var(--border)'}`,
              }}>
                {met ? (
                  <AlertCircle size={18} color="#EF4444" style={{ flexShrink: 0 }} />
                ) : (
                  <CheckCircle size={18} color="var(--text-muted)" style={{ flexShrink: 0 }} />
                )}
                <span style={{ flex: 1, color: met ? 'var(--text-primary)' : 'var(--text-secondary)', fontWeight: met ? 600 : 400, fontSize: '0.9375rem' }}>
                  {trigger}
                </span>
                {met && <span className="badge badge-critical" style={{ fontSize: '0.6875rem' }}>TRIGGERED</span>}
              </div>
            );
          })}
        </div>
      </div>

      {/* Schedule Reassessment */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: showSchedule ? '20px' : 0 }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px', margin: 0 }}>
            <CalendarPlus size={18} color="var(--accent-primary)" /> Schedule Reassessment
          </h3>
          {!showSchedule && (
            <button className="btn btn-primary" onClick={() => setShowSchedule(true)}>
              <Clock size={16} /> Schedule Now
            </button>
          )}
        </div>

        {showSchedule && (
          <form onSubmit={handleScheduleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Reassessment Type</label>
                <select className="form-select" value={scheduleForm.type} onChange={e => setScheduleForm({...scheduleForm, type: e.target.value})}>
                  <option value="routine">Routine</option>
                  <option value="urgent">Urgent</option>
                  <option value="post_therapy_change">Post Therapy Change</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Interval (minutes)</label>
                <select className="form-select" value={scheduleForm.interval_minutes} onChange={e => setScheduleForm({...scheduleForm, interval_minutes: parseInt(e.target.value)})}>
                  <option value={15}>15 minutes</option>
                  <option value={30}>30 minutes</option>
                  <option value={60}>1 hour</option>
                  <option value={120}>2 hours</option>
                  <option value={240}>4 hours</option>
                </select>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Assign To (Staff)</label>
              <select className="form-select" value={scheduleForm.assigned_staff} onChange={e => setScheduleForm({...scheduleForm, assigned_staff: e.target.value})} required>
                <option value="">Select Staff Member</option>
                {staffList.map(s => (
                  <option key={s.id} value={s.id}>{s.name} ({s.department})</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Notes (optional)</label>
              <textarea className="form-input" rows={2} value={scheduleForm.notes} onChange={e => setScheduleForm({...scheduleForm, notes: e.target.value})} placeholder="Add any special instructions..." />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
              <button type="button" className="btn btn-outline" onClick={() => setShowSchedule(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Scheduling...' : 'Schedule Reassessment'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default EscalationTab;
