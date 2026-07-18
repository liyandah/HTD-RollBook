import React, { useState, useEffect } from 'react';
import { Menu, Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getUsername } from '../../utils/auth';
import { getUnreadCount } from '../../api/contributionsApi';

const Topbar = ({ onMenuClick }) => {
  const username = getUsername();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    loadUnreadCount();
    // Refresh every 30 seconds
    const interval = setInterval(loadUnreadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadUnreadCount = async () => {
    try {
      const response = await getUnreadCount();
      setUnreadCount(response.data?.count || 0);
    } catch (err) {
      console.error('Failed to load unread count:', err);
    }
  };

  return (
    <div className="bg-white shadow-sm border-b h-16 flex items-center justify-between px-4 sm:px-6 sticky top-0 z-30">
      <div className="flex items-center gap-4">
        {/* Hamburger Menu Button - Only visible on mobile/tablet */}
        <button
          onClick={onMenuClick}
          className="lg:hidden p-2 hover:bg-gray-100 rounded-lg transition-colors"
          aria-label="Toggle menu"
        >
          <Menu size={24} className="text-gray-700" />
        </button>
        
        <div className="text-gray-600">
          <h2 className="text-base sm:text-lg font-semibold">Admin Dashboard</h2>
        </div>
      </div>

      <div className="flex items-center gap-2 sm:gap-4">
        {/* Notification Bell */}
        <button
          onClick={() => navigate('/contributions/notifications')}
          className="relative p-2 hover:bg-gray-100 rounded-lg transition-colors"
          aria-label="Notifications"
        >
          <Bell size={20} className="text-gray-700" />
          {unreadCount > 0 && (
            <span className="absolute top-1 right-1 w-5 h-5 bg-brand-red text-white text-xs font-bold rounded-full flex items-center justify-center">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </button>

        <div className="flex items-center gap-2">
          <div className="w-8 h-8 bg-brand-red text-white rounded-full flex items-center justify-center font-semibold">
            {username ? username[0].toUpperCase() : 'A'}
          </div>
          <span className="text-gray-700 font-medium hidden sm:inline">{username}</span>
        </div>
      </div>
    </div>
  );
};

export default Topbar;


