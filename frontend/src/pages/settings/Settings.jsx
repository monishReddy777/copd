import React, { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Settings as SettingsIcon, Bell, Lock, Globe, Moon } from 'lucide-react';
import toast from 'react-hot-toast';

const Settings = () => {
  const { role } = useAuth();
  
  const [notifications, setNotifications] = useState({
    email_alerts: true,
    sms_alerts: false,
    critical_only: false,
    daily_summary: true
  });

  const [appearance, setAppearance] = useState({
    theme: 'dark', // We built a dark theme by default
    compact_mode: false
  });

  const handleNotificationChange = (e) => {
    setNotifications({ ...notifications, [e.target.name]: e.target.checked });
  };

  const handleAppearanceChange = (e) => {
    setAppearance({ ...appearance, [e.target.name]: e.target.value });
  };

  const saveSettings = (e) => {
    e.preventDefault();
    toast.success('Settings saved successfully');
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1>App Settings</h1>
          <p>Customize your experience and notification preferences</p>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '32px', alignItems: 'flex-start' }}>
        
        {/* Sidebar Nav */}
        <div className="card" style={{ width: '240px', padding: '16px' }}>
          <ul style={{ listStyleType: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <li>
              <button className="btn btn-ghost" style={{ width: '100%', justifyContent: 'flex-start', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}>
                <SettingsIcon size={18} /> General
              </button>
            </li>
            <li>
              <button className="btn btn-ghost" style={{ width: '100%', justifyContent: 'flex-start', color: 'var(--text-secondary)' }}>
                <Bell size={18} /> Notifications
              </button>
            </li>
            <li>
              <button className="btn btn-ghost" style={{ width: '100%', justifyContent: 'flex-start', color: 'var(--text-secondary)' }}>
                <Lock size={18} /> Security
              </button>
            </li>
          </ul>
        </div>

        {/* Content Area */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Notifications Card */}
          <div className="card">
            <h3 className="section-title"><Bell size={20} /> Notification Preferences</h3>
            <form onSubmit={saveSettings}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '24px' }}>
                <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)' }}>
                  <div>
                    <div style={{ fontWeight: 500 }}>Email Alerts</div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Receive critical alerts via email</div>
                  </div>
                  <input type="checkbox" name="email_alerts" checked={notifications.email_alerts} onChange={handleNotificationChange} style={{ width: '20px', height: '20px' }} />
                </label>

                <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)' }}>
                  <div>
                    <div style={{ fontWeight: 500 }}>SMS Alerts</div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Receive notifications on your phone</div>
                  </div>
                  <input type="checkbox" name="sms_alerts" checked={notifications.sms_alerts} onChange={handleNotificationChange} style={{ width: '20px', height: '20px' }} />
                </label>

                <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)' }}>
                  <div>
                    <div style={{ fontWeight: 500 }}>Critical Alerts Only</div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Mute info and warning notifications</div>
                  </div>
                  <input type="checkbox" name="critical_only" checked={notifications.critical_only} onChange={handleNotificationChange} style={{ width: '20px', height: '20px' }} />
                </label>
              </div>

              <div style={{ borderTop: '1px solid var(--border-light)', paddingTop: '20px', display: 'flex', justifyContent: 'flex-end' }}>
                <button type="submit" className="btn btn-primary">Save Preferences</button>
              </div>
            </form>
          </div>

          {/* Appearance Card */}
          <div className="card">
            <h3 className="section-title"><Moon size={20} /> Appearance</h3>
            <div className="form-group">
              <label className="form-label">Theme</label>
              <select className="form-select" name="theme" value={appearance.theme} onChange={handleAppearanceChange}>
                <option value="dark">Dark Theme (Default)</option>
                <option value="light">Light Theme (Coming Soon)</option>
                <option value="system">System Preference</option>
              </select>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
};

export default Settings;
