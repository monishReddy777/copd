import { getImageUrl } from '../../utils/imageUrl';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { 
  Home, Users, Activity, Settings, Bell, 
  LogOut, Shield, FileText,
  UserPlus, Stethoscope, HeartPulse
} from 'lucide-react';

const Sidebar = ({ isOpen, toggleSidebar }) => {
  const { role, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    if (window.confirm("Are you want to log out?")) {
      logout();
      navigate('/login');
    }
  };

  const doctorLinks = [
    { to: '/doctor/dashboard', icon: <Home className="nav-icon" />, label: 'Dashboard' },
    { to: '/doctor/patients', icon: <Users className="nav-icon" />, label: 'Patients' },
    { to: '/doctor/alerts', icon: <Bell className="nav-icon" />, label: 'Alerts' },
  ];

  const staffLinks = [
    { to: '/staff/dashboard', icon: <Home className="nav-icon" />, label: 'Dashboard' },
    { to: '/staff/patients', icon: <Users className="nav-icon" />, label: 'Patient List' },
    { to: '/staff/alerts', icon: <Bell className="nav-icon" />, label: 'Alerts' },
  ];

  const adminLinks = [
    { to: '/admin/dashboard', icon: <Home className="nav-icon" />, label: 'Dashboard' },
    { to: '/admin/doctors', icon: <Stethoscope className="nav-icon" />, label: 'Manage Doctors' },
    { to: '/admin/staff', icon: <HeartPulse className="nav-icon" />, label: 'Manage Staff' },
    { to: '/admin/approvals', icon: <Shield className="nav-icon" />, label: 'Approvals' },
  ];

  const getLinks = () => {
    if (role === 'doctor') return doctorLinks;
    if (role === 'staff') return staffLinks;
    if (role === 'admin') return adminLinks;
    return [];
  };

  const getProfileLink = () => {
    if (role === 'doctor') return '/doctor/profile';
    if (role === 'staff') return '/staff/profile';
    if (role === 'admin') return '/admin/profile';
    return '/';
  };

  return (
    <aside className={`sidebar ${!isOpen ? 'collapsed' : ''}`}>
      <div className="sidebar-header">
        <div className="sidebar-logo">
          <Activity size={24} color="#fff" />
        </div>
        <div className="sidebar-brand">CDSS <span>COPD</span></div>
      </div>
      
      <div className="sidebar-nav">
        <div className="sidebar-section">
          <div className="sidebar-section-title">Main</div>
          {getLinks().map((link) => (
            <NavLink 
              key={link.to} 
              to={link.to} 
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              {link.icon}
              <span className="nav-label">{link.label}</span>
            </NavLink>
          ))}
        </div>
        
        {role !== 'admin' && (
          <div className="sidebar-section">
            <div className="sidebar-section-title">Clinical</div>
            <NavLink to="/add-patient" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <UserPlus className="nav-icon" />
              <span className="nav-label">Add Patient</span>
            </NavLink>
            <NavLink to="/settings/guidelines" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <FileText className="nav-icon" />
              <span className="nav-label">Guidelines</span>
            </NavLink>
          </div>
        )}

        <div className="sidebar-section">
          <div className="sidebar-section-title">Preferences</div>
          <NavLink to={getProfileLink()} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <Settings className="nav-icon" />
            <span className="nav-label">Profile & Settings</span>
          </NavLink>
          <div className="nav-item" onClick={handleLogout}>
            <LogOut className="nav-icon" />
            <span className="nav-label">Logout</span>
          </div>
        </div>
      </div>

      <div className="sidebar-footer">
        <div className="sidebar-avatar" style={{ overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {user?.profile_image ? (
            <img 
              src={getImageUrl(user.profile_image)} 
              alt="Avatar" 
              style={{ width: '100%', height: '100%', objectFit: 'cover' }} 
            />
          ) : (
            user?.name ? user.name.charAt(0).toUpperCase() : 'U'
          )}
        </div>
        <div className="sidebar-user-info">
          <div className="sidebar-user-name">{user?.name || 'User'}</div>
          <div className="sidebar-user-role">{role || 'Unknown Role'}</div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
