import React, { useState, useEffect } from 'react';
import { Bell, AlertCircle, AlertTriangle, Info, CheckCircle, Clock, Filter, Trash2 } from 'lucide-react';
import api from '../api/axios';
import toast from 'react-hot-toast';

const Notifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    try {
      const { data } = await api.get('/notifications/');
      setNotifications(Array.isArray(data) ? data : data.results || []);
    } catch {
      // Mock data
      setNotifications([
        { id: 1, type: 'critical', title: 'SpO2 Alert - Robert Fox', message: 'SpO2 dropped to 82%. Immediate intervention required.', created_at: new Date(Date.now() - 600000).toISOString(), read: false },
        { id: 2, type: 'warning', title: 'ABG Results - Jane Cooper', message: 'Latest ABG shows elevated PaCO2 (52 mmHg). Review recommended.', created_at: new Date(Date.now() - 3600000).toISOString(), read: false },
        { id: 3, type: 'info', title: 'Reassessment Due - Guy Hawkins', message: 'Scheduled reassessment is due in 15 minutes.', created_at: new Date(Date.now() - 7200000).toISOString(), read: true },
        { id: 4, type: 'critical', title: 'NIV Required - Sarah Wilson', message: 'pH dropped to 7.28. NIV initiation recommended.', created_at: new Date(Date.now() - 14400000).toISOString(), read: true },
        { id: 5, type: 'info', title: 'New Patient Assigned', message: 'Patient Michael Brown has been assigned to your care.', created_at: new Date(Date.now() - 28800000).toISOString(), read: true },
        { id: 6, type: 'warning', title: 'Therapy Change - Robert Fox', message: 'Oxygen device changed from Nasal Cannula to Venturi Mask.', created_at: new Date(Date.now() - 43200000).toISOString(), read: true }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = (id) => {
    setNotifications(ns => ns.map(n => n.id === id ? { ...n, read: true } : n));
  };

  const dismiss = (id) => {
    setNotifications(ns => ns.filter(n => n.id !== id));
    toast.success('Notification dismissed');
  };

  const markAllRead = () => {
    setNotifications(ns => ns.map(n => ({ ...n, read: true })));
    toast.success('All notifications marked as read');
  };

  const filtered = filter === 'all' ? notifications : notifications.filter(n => n.type === filter);
  const unreadCount = notifications.filter(n => !n.read).length;

  const getIcon = (type) => {
    switch (type) {
      case 'critical': return <AlertCircle size={20} color="#EF4444" />;
      case 'warning': return <AlertTriangle size={20} color="#F59E0B" />;
      default: return <Info size={20} color="#3B82F6" />;
    }
  };

  const getBg = (type, read) => {
    if (read) return 'transparent';
    switch (type) {
      case 'critical': return 'rgba(239, 68, 68, 0.06)';
      case 'warning': return 'rgba(245, 158, 11, 0.06)';
      default: return 'rgba(59, 130, 246, 0.06)';
    }
  };

  const timeAgo = (ts) => {
    const diff = Date.now() - new Date(ts).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
  };

  if (loading) return <div className="loader-container"><div className="spinner"></div></div>;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Bell size={24} /> Notifications
            {unreadCount > 0 && (
              <span style={{ background: 'var(--status-critical)', color: '#fff', padding: '2px 10px', borderRadius: '12px', fontSize: '0.8125rem', fontWeight: 700 }}>
                {unreadCount}
              </span>
            )}
          </h1>
          <p>Stay updated with patient alerts and system notifications</p>
        </div>
        <div className="page-header-actions">
          {unreadCount > 0 && (
            <button className="btn btn-outline" onClick={markAllRead}>
              <CheckCircle size={16} /> Mark All Read
            </button>
          )}
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="tabs" style={{ marginBottom: '24px', background: 'var(--bg-surface)', padding: '4px', borderRadius: 'var(--radius-lg)' }}>
        {[
          { key: 'all', label: 'All' },
          { key: 'critical', label: 'Critical' },
          { key: 'warning', label: 'Warning' },
          { key: 'info', label: 'Info' }
        ].map(f => (
          <button key={f.key} className={`tab ${filter === f.key ? 'active' : ''}`}
            onClick={() => setFilter(f.key)} style={{ flex: 1, padding: '10px' }}>
            {f.label}
          </button>
        ))}
      </div>

      {/* Notifications List */}
      {filtered.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <Bell className="empty-state-icon" />
            <h3>No Notifications</h3>
            <p>You're all caught up! No {filter !== 'all' ? filter : ''} notifications.</p>
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {filtered.map(n => (
            <div key={n.id} className="card" onClick={() => markAsRead(n.id)} style={{
              padding: '16px 20px', cursor: 'pointer', background: getBg(n.type, n.read),
              borderLeft: `4px solid ${n.type === 'critical' ? '#EF4444' : n.type === 'warning' ? '#F59E0B' : '#3B82F6'}`,
              opacity: n.read ? 0.75 : 1, transition: 'all 0.2s'
            }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '14px' }}>
                <div style={{ flexShrink: 0, marginTop: '2px' }}>{getIcon(n.type)}</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                    <h4 style={{ fontWeight: n.read ? 500 : 700, fontSize: '0.9375rem', margin: 0 }}>{n.title}</h4>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={12} /> {timeAgo(n.created_at)}
                      </span>
                      <button onClick={(e) => { e.stopPropagation(); dismiss(n.id); }} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '4px' }}>
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', margin: 0, lineHeight: 1.5 }}>{n.message}</p>
                  {!n.read && <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--accent-primary)', position: 'absolute', top: '12px', right: '12px' }}></div>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Notifications;
