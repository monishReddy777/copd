import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Menu, Search, Bell, User } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

const Header = ({ toggleSidebar }) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  
  return (
    <header style={{ 
      height: '64px', 
      borderBottom: '1px solid var(--border)', 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'space-between',
      padding: '0 24px',
      background: 'var(--bg-card)',
      backdropFilter: 'blur(10px)',
      position: 'sticky',
      top: 0,
      zIndex: 10
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <button 
          onClick={toggleSidebar}
          style={{ 
            background: 'none', 
            border: 'none', 
            color: 'var(--text-secondary)',
            cursor: 'pointer',
            display: 'flex',
            padding: '8px',
            borderRadius: 'var(--radius-sm)',
            transition: 'background 0.2s'
          }}
          onMouseOver={(e) => e.currentTarget.style.background = 'var(--bg-surface)'}
          onMouseOut={(e) => e.currentTarget.style.background = 'none'}
        >
          <Menu size={20} />
        </button>
        
        <div className="search-box">
          <Search className="search-icon" />
          <input type="text" placeholder="Search patients, alerts..." />
        </div>
      </div>
      
      <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
        <div onClick={() => navigate('/notifications')} style={{ position: 'relative', cursor: 'pointer', color: 'var(--text-secondary)' }}>
          <Bell size={20} />
          <div style={{ 
            position: 'absolute', 
            top: '-2px', 
            right: '-2px', 
            width: '8px', 
            height: '8px', 
            backgroundColor: 'var(--status-critical)', 
            borderRadius: '50%',
            border: '2px solid var(--bg-card)'
          }}></div>
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', paddingLeft: '20px', borderLeft: '1px solid var(--border)' }}>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--text-primary)' }}>{user?.name || 'Doctor'}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>CDSS Portal</div>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
