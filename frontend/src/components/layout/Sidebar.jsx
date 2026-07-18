import React, { useState, useEffect } from 'react';
import { 
  LayoutDashboard, 
  Database, 
  BarChart3, 
  Users, 
  Settings,
  LogOut,
  X,
  DollarSign,
  ShieldCheck,
  QrCode
} from 'lucide-react';
import { logout } from '../../utils/auth';
import { useNavigate } from 'react-router-dom';
import SidebarItem from './SidebarItem';

const SHIELD_LOGO_SRC = '/shield.jpg?v=20260414';

const Sidebar = ({ isOpen, onClose }) => {
  const navigate = useNavigate();
  const [userRole, setUserRole] = useState('VIEWER');
  const [logoLoadFailed, setLogoLoadFailed] = useState(false);

  useEffect(() => {
    const loadUserRole = async () => {
      try {
        const { getUserRole } = await import('../../utils/user');
        const role = await getUserRole();
        setUserRole(role || 'VIEWER'); // Default to VIEWER if role is null
      } catch (err) {
        console.error('Failed to load user role:', err);
        setUserRole('VIEWER'); // Default to VIEWER on error
      }
    };
    loadUserRole();
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleLinkClick = () => {
    // Close sidebar on mobile when a link is clicked
    if (window.innerWidth < 1024) {
      onClose();
    }
  };

  const menuItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/records', label: 'Records', icon: Database },
    { 
      path: '/contributions', 
      label: 'Contributions & Projects', 
      icon: DollarSign,
      subItems: [
        { path: '/contributions/overview', label: 'Overview' },
        { path: '/contributions/capture', label: 'Capture Payment', roles: ['ADMIN', 'SECRETARY', 'TREASURER'] },
        { path: '/contributions/payments', label: 'Payments' },
        { path: '/contributions/projects', label: 'Projects', roles: ['ADMIN'] },
        { path: '/contributions/events', label: 'Events', roles: ['ADMIN'] },
        { path: '/contributions/my-contributions', label: 'My Contributions', roles: ['VIEWER'] },
        { path: '/contributions/notifications', label: 'Notifications' },
      ]
    },
    { path: '/reports', label: 'Reports', icon: BarChart3 },
    { path: '/admin/verifications', label: 'Verifications', icon: ShieldCheck },
    { path: '/admin/scan', label: 'Attendance Scanner', icon: QrCode },
    { path: '/users', label: 'Users', icon: Users },
    { path: '/settings', label: 'Settings', icon: Settings },
  ];

  return (
    <div 
      className={`
        w-72 bg-gradient-to-b from-brand-navy to-brand-navyDark text-white 
        min-h-screen fixed left-0 top-0 shadow-2xl z-50
        transform transition-transform duration-300 ease-in-out
        ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
      `}
    >
      {/* Logo Section */}
      <div className="p-6 border-b border-white/10">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center shadow-lg overflow-hidden border border-white/70">
              {!logoLoadFailed ? (
                <img
                  src={SHIELD_LOGO_SRC}
                  alt="Salvation Army Shield"
                  className="w-full h-full object-cover"
                  onError={() => setLogoLoadFailed(true)}
                />
              ) : (
                <span className="text-brand-red text-xl font-bold">HTE</span>
              )}
            </div>
            <div>
              <h1 className="text-xl font-bold text-white">HT-E Roll Book</h1>
              <p className="text-xs text-blue-200">Data Collection</p>
            </div>
          </div>
          {/* Close button for mobile */}
          <button
            onClick={onClose}
            className="lg:hidden p-2 hover:bg-white/10 rounded-lg transition-colors"
            aria-label="Close menu"
          >
            <X size={24} />
          </button>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex flex-col gap-2 p-4 mt-2 pb-32">
        {menuItems.map((item) => (
          <SidebarItem
            key={item.path}
            item={item}
            userRole={userRole}
            onLinkClick={handleLinkClick}
          />
        ))}
      </nav>

      {/* User Section */}
      <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-white/10">
        <div className="bg-white/5 rounded-xl p-4 mb-3">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-brand-red rounded-full flex items-center justify-center">
              <span className="text-white text-sm font-bold">AD</span>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-white truncate">Admin User</p>
              <p className="text-xs text-blue-300 truncate">admin@salvationarmy.org</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-white/10 hover:bg-white/20 rounded-lg text-sm font-medium text-white transition-all duration-200 group"
          >
            <LogOut size={16} className="group-hover:translate-x-0.5 transition-transform" />
            Logout
          </button>
        </div>
        <p className="text-xs text-blue-400 text-center">© 2026 HT-E Roll Book</p>
      </div>
    </div>
  );
};

export default Sidebar;
