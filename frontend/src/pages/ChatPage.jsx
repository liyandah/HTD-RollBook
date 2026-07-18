import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ChatSidebar from '../components/chat/ChatSidebar';
import ChatPanel from '../components/chat/ChatPanel';
import NewChatModal from '../components/chat/NewChatModal';
import http from '../api/apiClient';

const ChatPage = () => {
  const navigate = useNavigate();
  const [conversations, setConversations] = useState([]);
  const [selectedConversationId, setSelectedConversationId] = useState(null);
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showNewChatModal, setShowNewChatModal] = useState(false);
  const [showSidebar, setShowSidebar] = useState(true); // For mobile sidebar toggle

  // Check authentication and password requirement
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }
    
    // Check if password is required
    http.get('/api/auth/check-password')
      .then((response) => {
        if (response.data.requiresPassword) {
          navigate('/create-password');
          return;
        }
        loadConversations();
      })
      .catch((err) => {
        console.error('Failed to check password requirement:', err);
        // Continue to load conversations anyway
        loadConversations();
      });
  }, [navigate]);

  const loadConversations = async () => {
    try {
      setLoading(true);
      const response = await http.get('/api/conversations');
      let convs = response.data || [];
      
      console.log('[ChatPage] Loaded conversations:', convs);

      // Ensure bot conversation exists (backend should create it, but double-check)
      let botConv = convs.find((c) => c.type === 'BOT');
      console.log('[ChatPage] Bot conversation found:', botConv);
      
      if (!botConv) {
        console.log('[ChatPage] Bot conversation not found, creating...');
        try {
          const botResponse = await http.get('/api/conversations/bot');
          botConv = botResponse.data;
          console.log('[ChatPage] Bot conversation created:', botConv);
          if (botConv) {
            convs = [botConv, ...convs];
          }
        } catch (botErr) {
          console.error('[ChatPage] Failed to get bot conversation:', botErr);
        }
      } else {
        // Ensure bot is first in list
        convs = [botConv, ...convs.filter((c) => c.id !== botConv.id)];
      }

      console.log('[ChatPage] Final conversations list:', convs);
      setConversations(convs);

      // Auto-select bot conversation if none selected
      if (!selectedConversationId && botConv) {
        handleSelectConversation(botConv.id, botConv);
      } else if (selectedConversationId) {
        const selected = convs.find((c) => c.id === selectedConversationId);
        if (selected) {
          setSelectedConversation(selected);
        }
      }
    } catch (err) {
      console.error('[ChatPage] Failed to load conversations:', err);
      console.error('[ChatPage] Error response:', err.response?.data);
      console.error('[ChatPage] Error status:', err.response?.status);
      if (err.response?.status === 401) {
        localStorage.removeItem('token');
        navigate('/login');
      } else if (err.response?.status === 500) {
        // Log detailed error for debugging
        console.error('[ChatPage] Server error:', err.response?.data);
        // If user not found, redirect to login
        if (err.response?.data?.message?.includes('User not found')) {
          console.error('[ChatPage] User not found in database. Redirecting to login...');
          localStorage.removeItem('token');
          localStorage.removeItem('userId');
          localStorage.removeItem('userEmail');
          localStorage.removeItem('userName');
          navigate('/login');
        }
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSelectConversation = (conversationId, conversation = null) => {
    setSelectedConversationId(conversationId);
    const conv = conversation || conversations.find((c) => c.id === conversationId);
    setSelectedConversation(conv);
    
    // On mobile, hide sidebar when conversation is selected
    if (window.innerWidth < 640) {
      setShowSidebar(false);
    }

    // Mark messages as read
    if (conv) {
      http.get(`/api/conversations/${conversationId}/messages`, {
        params: { page: 0, size: 1 },
      }).catch(console.error);
    }
  };
  
  const handleBackToSidebar = () => {
    setShowSidebar(true);
    setSelectedConversationId(null);
    setSelectedConversation(null);
  };

  const handleNewChat = () => {
    setShowNewChatModal(true);
  };

  const handleSelectUser = async (user) => {
    try {
      const response = await http.post('/api/conversations/direct', null, {
        params: { targetUserId: user.id },
      });

      const newConv = response.data;
      setConversations((prev) => [newConv, ...prev]);
      handleSelectConversation(newConv.id, newConv);
    } catch (err) {
      console.error('Failed to create direct conversation:', err);
    }
  };

  const handleMessageSent = () => {
    // Reload conversations to update last message
    loadConversations();
  };

  // Auto-start bot conversation on first load (only on desktop)
  useEffect(() => {
    if (conversations.length > 0 && !selectedConversationId && window.innerWidth >= 640) {
      const botConv = conversations.find((c) => c.type === 'BOT');
      if (botConv) {
        handleSelectConversation(botConv.id, botConv);
      }
    }
  }, [conversations]);
  
  // Handle window resize
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 640) {
        setShowSidebar(true);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  if (loading) {
    return (
      <div className="h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-brand-red border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">Loading chats...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="app-container h-screen w-screen flex bg-slate-50 overflow-hidden relative">
      {/* Sidebar - hidden on mobile when chat is open */}
      <div className={`absolute sm:relative inset-0 z-20 sm:z-auto transition-transform duration-300 ease-in-out ${
        showSidebar ? 'translate-x-0' : '-translate-x-full sm:translate-x-0'
      }`}>
        <ChatSidebar
          conversations={conversations}
          selectedConversationId={selectedConversationId}
          onSelectConversation={handleSelectConversation}
          onNewChat={handleNewChat}
          onClose={() => setShowSidebar(false)}
        />
      </div>
      
      {/* Overlay for mobile when sidebar is open and chat is selected */}
      {showSidebar && selectedConversationId && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-10 sm:hidden"
          onClick={() => setShowSidebar(false)}
        />
      )}

      {/* Chat Panel */}
      <div className={`chat-area chat-main flex-1 ${selectedConversationId ? 'block' : 'hidden sm:block'}`}>
        <ChatPanel
          conversationId={selectedConversationId}
          conversationType={selectedConversation?.type}
          otherParticipant={selectedConversation?.otherParticipant}
          onMessageSent={handleMessageSent}
          onBack={handleBackToSidebar}
          onMenuClick={() => setShowSidebar(true)}
        />
      </div>
      
      {/* Empty state when no conversation selected on desktop */}
      {!selectedConversationId && (
        <div className="hidden sm:flex flex-1 items-center justify-center bg-[radial-gradient(circle_at_center,_#1a365d_0%,_#001a3d_100%)]">
          <div className="text-center">
            <div className="w-16 h-16 bg-gradient-to-br from-brand-red to-brand-redDark rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-white text-xl font-bold">HTF</span>
            </div>
            <p className="text-gray-500">Select a conversation to start chatting</p>
          </div>
        </div>
      )}

      <NewChatModal
        isOpen={showNewChatModal}
        onClose={() => setShowNewChatModal(false)}
        onSelectUser={handleSelectUser}
      />
    </div>
  );
};

export default ChatPage;
