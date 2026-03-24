import React, { useState, useEffect } from 'react';
import { getSystemStatistics } from '../../api/admin';
import { Stethoscope, HeartPulse, ShieldAlert } from 'lucide-react';
import toast from 'react-hot-toast';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      const { data } = await getSystemStatistics();
      setStats(data);
    } catch (error) {
      toast.error('Failed to load system statistics');
      setStats({
        total_doctors: 0,
        total_staff: 0,
        pending_approvals: 0
      });
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loader-container"><div className="spinner"></div></div>;
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>System Overview</h1>
          <p>Monitor CDSS COPD platform metrics</p>
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-card-icon blue">
            <Stethoscope size={24} />
          </div>
          <div className="stat-card-value">{stats?.total_doctors || 0}</div>
          <div className="stat-card-label">Active Doctors</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon green">
            <HeartPulse size={24} />
          </div>
          <div className="stat-card-value">{stats?.total_staff || 0}</div>
          <div className="stat-card-label">Active Staff</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon orange">
            <ShieldAlert size={24} />
          </div>
          <div className="stat-card-value">{stats?.pending_approvals || stats?.pending_requests || 0}</div>
          <div className="stat-card-label">Pending Approvals</div>
        </div>
      </div>

      <div className="card" style={{ marginTop: '24px' }}>
        <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px' }}>Quick Actions</h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <a href="/admin/approvals" className="btn btn-primary" style={{ justifyContent: 'flex-start', background: 'var(--bg-secondary)', color: 'var(--text-primary)', boxShadow: 'none' }}>
            <ShieldAlert size={18} /> Review Approvals
          </a>
          <a href="/admin/doctors" className="btn btn-outline" style={{ justifyContent: 'flex-start' }}>
            <Stethoscope size={18} /> Manage Doctors
          </a>
          <a href="/admin/staff" className="btn btn-outline" style={{ justifyContent: 'flex-start' }}>
            <HeartPulse size={18} /> Manage Staff
          </a>
        </div>
      </div>
    </>
  );
};

export default Dashboard;
