import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Plus, LogOut } from 'lucide-react';
import { logout } from '../../utils/auth';

const ChatSidebar = ({ conversations, selectedConversationId, onSelectConversation, onNewChat, onClose }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredConversations, setFilteredConversations] = useState(conversations);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredConversations(conversations);
      return;
    }

    const filtered = conversations.filter((conv) => {
      const query = searchQuery.toLowerCase();
      if (conv.otherParticipant) {
        const name = conv.otherParticipant.fullName?.toLowerCase() || '';
        const email = conv.otherParticipant.email?.toLowerCase() || '';
        return name.includes(query) || email.includes(query);
      }
      return conv.type === 'BOT' && 'bot'.includes(query);
    });

    setFilteredConversations(filtered);
  }, [searchQuery, conversations]);

  const formatTime = (date) => {
    if (!date) return '';
    const d = new Date(date);
    const now = new Date();
    const diff = now - d;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return d.toLocaleDateString();
  };

  const getConversationName = (conv) => {
    if (conv.type === 'BOT') return 'HT-E Roll Book Bot';
    if (conv.otherParticipant) {
      return conv.otherParticipant.fullName || conv.otherParticipant.email || 'Unknown';
    }
    return 'Unknown';
  };

  const getConversationPreview = (conv) => {
    if (conv.lastMessagePreview) {
      return conv.lastMessagePreview;
    }
    return conv.type === 'BOT' ? 'Start your registration...' : 'No messages yet';
  };

  return (
    <div className="sidebar w-full sm:w-[300px] bg-brand-navy text-white border-r border-white/10 flex flex-col h-full min-h-0 overflow-hidden shadow-lg sm:shadow-none">
      {/* Header */}
      <div className="p-3 sm:p-4 border-b border-brand-navyLight bg-brand-navy">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-base sm:text-lg font-semibold text-white">Chats</h2>
          <div className="flex items-center gap-1 sm:gap-2">
            <button
              onClick={onNewChat}
              className="p-2 rounded-full hover:bg-brand-navyLight transition-colors touch-manipulation min-w-[44px] min-h-[44px] sm:min-w-0 sm:min-h-0 flex items-center justify-center"
              title="New Chat"
            >
              <Plus className="w-5 h-5 text-white" />
            </button>
            <button
              onClick={handleLogout}
              className="p-2 rounded-full hover:bg-brand-navyLight transition-colors touch-manipulation min-w-[44px] min-h-[44px] sm:min-w-0 sm:min-h-0 flex items-center justify-center"
              title="Logout"
            >
              <LogOut className="w-5 h-5 text-white" />
            </button>
          </div>
        </div>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Search chats..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 sm:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-red focus:border-transparent text-base sm:text-sm text-gray-900"
          />
        </div>
      </div>

      {/* Conversations List */}
      <div className="flex-1 overflow-y-auto min-h-0 bg-white text-gray-900">
        {filteredConversations.length === 0 ? (
          <div className="p-4 text-center text-gray-500 text-sm">
            {searchQuery ? 'No chats found' : 'No conversations yet'}
          </div>
        ) : (
          filteredConversations.map((conv) => (
            <div
              key={conv.id}
              onClick={() => onSelectConversation(conv.id)}
              className={`p-3 sm:p-4 border-b border-gray-100 cursor-pointer hover:bg-gray-50 active:bg-gray-100 transition-colors touch-manipulation ${
                selectedConversationId === conv.id ? 'bg-blue-50 border-l-4 border-l-brand-red' : ''
              }`}
            >
              <div className="flex items-start gap-2 sm:gap-3">
                <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-full bg-gradient-to-br from-brand-red to-brand-redDark flex items-center justify-center flex-shrink-0">
                  {conv.type === 'BOT' ? (
                    <span className="text-white text-xs sm:text-sm font-bold">HTF</span>
                  ) : (
                    <span className="text-white text-xs font-medium">
                      {getConversationName(conv).charAt(0).toUpperCase()}
                    </span>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <h3 className="text-sm sm:text-sm font-semibold text-gray-900 truncate">
                      {getConversationName(conv)}
                    </h3>
                    {conv.lastMessageAt && (
                      <span className="text-xs text-gray-500 flex-shrink-0 ml-2">
                        {formatTime(conv.lastMessageAt)}
                      </span>
                    )}
                  </div>
                  <div className="flex items-center justify-between">
                    <p className="text-sm text-gray-600 truncate">
                      {getConversationPreview(conv)}
                    </p>
                    {conv.unreadCount > 0 && (
                      <span className="bg-brand-red text-white text-xs font-bold rounded-full w-5 h-5 sm:w-5 sm:h-5 flex items-center justify-center flex-shrink-0 ml-2">
                        {conv.unreadCount > 9 ? '9+' : conv.unreadCount}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default ChatSidebar;
