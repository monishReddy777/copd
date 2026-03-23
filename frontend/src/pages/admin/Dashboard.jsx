import React, { useState, useEffect } from 'react';
import { getSystemStatistics } from '../../api/admin';
import { Stethoscope, HeartPulse, Users, ShieldAlert } from 'lucide-react';
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
      // Set dummy data for the UI if backend fails
      setStats({
        total_doctors: 12,
        total_staff: 28,
        total_patients: 156,
        pending_approvals: 5
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
          <p>Monitor CDSS COPD platform metrics and activity</p>
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
          <div className="stat-card-icon purple">
            <Users size={24} />
          </div>
          <div className="stat-card-value">{stats?.total_patients || 0}</div>
          <div className="stat-card-label">Total Patients</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon orange">
            <ShieldAlert size={24} />
          </div>
          <div className="stat-card-value">{stats?.pending_approvals || 0}</div>
          <div className="stat-card-label">Pending Approvals</div>
        </div>
      </div>

      <div className="data-grid">
        <div className="card" style={{ gridColumn: 'span 2' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px' }}>Platform Activity</h3>
          <div style={{ height: '300px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-md)' }}>
            <p style={{ color: 'var(--text-muted)' }}>Chart visualization will be displayed here</p>
          </div>
        </div>

        <div className="card">
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
      </div>
    </>
  );
};

export default Dashboard;
