import React, { useState, useEffect, useRef } from 'react';
import { Send, Loader2, Menu, ArrowLeft } from 'lucide-react';
import MessageBubble from './MessageBubble';
import ImageAttachMenu from '../common/ImageAttachMenu';
import ImagePickButtons from '../common/ImagePickButtons';
import http from '../../api/apiClient';

// Simple UUID generator (no external dependency needed)
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
};

const ChatPanel = ({ conversationId, conversationType, otherParticipant, onMessageSent, onBack, onMenuClick }) => {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [greetingSent, setGreetingSent] = useState(false);
  const [currentStep, setCurrentStep] = useState(null); // Track registration step
  const [uploadingImage, setUploadingImage] = useState(false);
  const [quickReplies, setQuickReplies] = useState([]);
  const [quickReplyOptions, setQuickReplyOptions] = useState([]);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const prevMessageCountRef = useRef(0);

  const scrollToBottom = () => {
    requestAnimationFrame(() => {
      if (messagesContainerRef.current) {
        messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
      }
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    });
  };

  useEffect(() => {
    if (conversationId) {
      prevMessageCountRef.current = 0;
      setGreetingSent(false); // Reset greeting flag when conversation changes
      setQuickReplies([]);
      setQuickReplyOptions([]);
      loadMessages(0, true);
    } else {
      setMessages([]);
      setGreetingSent(false);
      setQuickReplies([]);
      setQuickReplyOptions([]);
    }
  }, [conversationId]);

  const applyChatExtras = (data) => {
    const opts = Array.isArray(data?.quickReplyOptions) && data.quickReplyOptions.length
      ? data.quickReplyOptions
      : [];
    setQuickReplyOptions(opts);
    if (opts.length) {
      setQuickReplies([]);
    } else {
      setQuickReplies(
        Array.isArray(data?.quickReplies) && data.quickReplies.length ? data.quickReplies : []
      );
    }
  };

  // Auto-send greeting to bot on first load
  useEffect(() => {
    if (conversationId && conversationType === 'BOT' && messages.length === 0 && !loadingMore && !greetingSent) {
      setGreetingSent(true);
      const timer = setTimeout(() => {
        http
          .post('/api/chat/message', {
            conversationId,
            text: 'hi',
            clientMessageId: generateUUID(),
          })
          .then((res) => {
            if (res.data?.registrationStep) setCurrentStep(res.data.registrationStep);
            applyChatExtras(res.data);
            setTimeout(() => loadMessages(0, true), 500);
          })
          .catch(console.error);
      }, 500);
      return () => clearTimeout(timer);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversationId, conversationType, messages.length, loadingMore, greetingSent]);

  useEffect(() => {
    const last = messages[messages.length - 1];
    const countIncreased = messages.length > prevMessageCountRef.current;
    prevMessageCountRef.current = messages.length;
    if (countIncreased && last && (last.isBot || last.senderUserId == null)) {
      scrollToBottom();
    }
  }, [messages]);

  const loadMessages = async (pageNum = 0, reset = false) => {
    if (!conversationId) return;

    setLoadingMore(true);
    try {
      const response = await http.get(`/api/conversations/${conversationId}/messages`, {
        params: { page: pageNum, size: 50 },
      });

      const newMessages = response.data.content || [];
      // Keep chronological order (oldest → newest). API may return newest-first; normalize to oldest-first for display.
      const chronological = [...newMessages].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
      if (reset) {
        setMessages(chronological);
        // Use server-provided step first (from GET messages for bot convos), then fallback to last bot message
        if (response.data.registrationStep != null) {
          setCurrentStep(response.data.registrationStep);
        } else {
          const botMessages = chronological.filter(m => m.isBot || !m.senderUserId);
          const lastBotMsg = botMessages.length ? botMessages[botMessages.length - 1] : null;
          if (lastBotMsg && lastBotMsg.registrationStep) {
            setCurrentStep(lastBotMsg.registrationStep);
          }
        }
      } else {
        // Prepend older page: newMessages for this page are older than prev, so [older..., ...prev]
        const olderChronological = [...newMessages].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        setMessages((prev) => [...olderChronological, ...prev]);
      }

      setHasMore(response.data.totalPages > pageNum + 1);
      setPage(pageNum);
    } catch (err) {
      console.error('Failed to load messages:', err);
    } finally {
      setLoadingMore(false);
    }
  };

  const sendBotText = async (messageText) => {
    if (!conversationId || sending) return;
    const clientMessageId = generateUUID();
    setSending(true);
    try {
      const response = await http.post('/api/chat/message', {
        conversationId: conversationId,
        text: messageText,
        clientMessageId: clientMessageId,
      });
      if (response.data.registrationStep) {
        setCurrentStep(response.data.registrationStep);
      }
      applyChatExtras(response.data);
      if (response.data.botReply && response.data.messageId) {
        const botMessage = {
          id: response.data.messageId,
          content: response.data.botReply,
          senderUserId: null,
          senderName: 'HT-E Roll Book Bot',
          createdAt: new Date().toISOString(),
          isBot: true,
        };
        setMessages((prev) => [...prev, botMessage]);
        scrollToBottom();
      }
      setTimeout(() => loadMessages(0, true), 800);
      if (onMessageSent) onMessageSent(response.data);
    } catch (err) {
      console.error('Failed to send message:', err);
    } finally {
      setSending(false);
    }
  };

  const handleSend = async (e) => {
    e.preventDefault();
    e.stopPropagation(); // Prevent double submission
    
    if (!inputMessage.trim() || !conversationId || sending) {
      return; // Already sending or empty message
    }

    const messageText = inputMessage.trim();
    const clientMessageId = generateUUID(); // Generate unique ID for idempotency
    
    // Clear input immediately to prevent double-submit
    setInputMessage('');
    setSending(true);

    try {
      let response;
      
      // Use new /api/chat/message endpoint for bot conversations
      if (conversationType === 'BOT') {
        response = await http.post('/api/chat/message', {
          conversationId: conversationId,
          text: messageText,
          clientMessageId: clientMessageId,
        });
        
        // Update current step from response IMMEDIATELY to prevent duplicate questions
        if (response.data.registrationStep) {
          setCurrentStep(response.data.registrationStep);
          console.log('[ChatPanel] Step updated:', response.data.registrationStep);
        }
        applyChatExtras(response.data);
        
        // Add bot reply to messages (only if we got a response AND a messageId)
        // If messageId is null, it's a duplicate/cached response - don't add it
        if (response.data.botReply && response.data.messageId) {
          const botMessage = {
            id: response.data.messageId,
            content: response.data.botReply,
            senderUserId: null,
            senderName: 'HT-E Roll Book Bot',
            createdAt: new Date().toISOString(),
            isBot: true,
          };
          setMessages((prev) => {
            // Prevent duplicate bot messages by checking if we already have this message ID
            const existingMsg = prev.find(msg => msg.id === botMessage.id);
            if (existingMsg) {
              console.log('[ChatPanel] Duplicate bot message detected (same ID), skipping');
              return prev;
            }
            // Also check if last message has same content (additional safeguard)
            const lastMsg = prev[prev.length - 1];
            if (lastMsg && lastMsg.isBot && lastMsg.content === botMessage.content && 
                lastMsg.id !== botMessage.id) {
              console.log('[ChatPanel] Duplicate bot message detected (same content), skipping');
              return prev;
            }
            return [...prev, botMessage];
          });
          scrollToBottom();
        } else if (response.data.botReply && !response.data.messageId) {
          // Duplicate/cached response - just update step, don't add message
          console.log('[ChatPanel] Received cached/duplicate response, not adding message');
        }
        
        // Reload messages to get user message and ensure sync (with delay to avoid race conditions)
        setTimeout(() => {
          loadMessages(0, true);
        }, 800); // Increased delay to ensure backend has processed
      } else {
        // Regular conversation - use old endpoint
        response = await http.post(`/api/conversations/${conversationId}/messages`, {
          content: messageText,
          clientMessageId: clientMessageId,
        });
        
        // Add message to local state
        setMessages((prev) => [...prev, response.data]);
        scrollToBottom();
      }

      // Notify parent
      if (onMessageSent) {
        onMessageSent(response.data);
      }
    } catch (err) {
      console.error('Failed to send message:', err);
      setInputMessage(messageText); // Restore message on error
    } finally {
      setSending(false);
    }
  };

  const handleLoadMore = () => {
    if (hasMore && !loadingMore) {
      loadMessages(page + 1, false);
    }
  };

  const getCurrentUserId = () => {
    return localStorage.getItem('userId');
  };

  const uploadImageFile = async (file) => {
    if (!file || !conversationId || uploadingImage) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      alert('Please select an image file (jpg, png, jpeg, etc.)');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      alert('Image size must be less than 5MB');
      return;
    }

    const imagePurpose = currentStep === 'ASK_CERT_IMAGE' ? 'cert' : 'person';
    const imageType = imagePurpose;

    setUploadingImage(true);

    try {
      const formData = new FormData();
      formData.append('conversationId', conversationId);
      formData.append('file', file);
      formData.append('imageType', imageType);
      formData.append('imagePurpose', imagePurpose);

      const response = await http.post('/api/chat/upload-image', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      if (response.data.registrationStep) {
        setCurrentStep(response.data.registrationStep);
      }
      applyChatExtras(response.data);

      if (response.data.botReply) {
        const botMessage = {
          id: response.data.messageId || `bot-${Date.now()}`,
          content: response.data.botReply,
          senderUserId: null,
          senderName: 'HT-E Roll Book Bot',
          createdAt: new Date().toISOString(),
          isBot: true,
        };
        setMessages((prev) => [...prev, botMessage]);
        scrollToBottom();
      }

      setTimeout(() => {
        loadMessages(0, true);
      }, 500);

      if (onMessageSent) {
        onMessageSent(response.data);
      }
    } catch (err) {
      console.error('Failed to upload image:', err);
      const errorMsg = err.response?.data?.message || err.message || 'Failed to upload image. Please try again.';
      alert(errorMsg);
    } finally {
      setUploadingImage(false);
    }
  };

  const showImageUpload = conversationType === 'BOT' && 
    (currentStep === 'ASK_PERSON_IMAGE' || currentStep === 'ASK_CERT_IMAGE');

  if (!conversationId) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="w-16 h-16 bg-gradient-to-br from-brand-red to-brand-redDark rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="text-white text-xl font-bold">HTF</span>
          </div>
          <p className="text-gray-500">Select a conversation to start chatting</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-white">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-3 sm:px-5 py-3 sm:py-4 flex items-center justify-between flex-shrink-0">
        <div className="flex items-center gap-2 sm:gap-3 min-w-0 flex-1">
          {/* Back/Menu button for mobile */}
          <button
            onClick={onBack || onMenuClick}
            className="sm:hidden p-2 -ml-2 text-gray-600 hover:text-gray-900 transition-colors"
            title="Back to chats"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          
          <div className="w-9 h-9 sm:w-11 sm:h-11 rounded-xl bg-brand-red flex items-center justify-center flex-shrink-0">
            {conversationType === 'BOT' ? (
              <span className="text-white text-xs sm:text-sm font-bold">HTF</span>
            ) : (
              <span className="text-white text-xs font-medium">
                {otherParticipant?.fullName?.charAt(0) || otherParticipant?.email?.charAt(0) || 'U'}
              </span>
            )}
          </div>
          <div className="min-w-0 flex-1">
            <h3 className="text-sm sm:text-base font-semibold text-gray-900 truncate">
              {conversationType === 'BOT'
                ? 'HTF Church Bot'
                : otherParticipant?.fullName || otherParticipant?.email || 'Unknown'}
            </h3>
            {conversationType === 'BOT' ? (
              <p className="text-xs text-gray-500 truncate">Community &amp; Stewardship Portal</p>
            ) : (
              <p className="text-xs text-gray-500 truncate hidden sm:block">{otherParticipant?.email}</p>
            )}
          </div>
        </div>
      </div>

      {/* Messages: WhatsApp-style — flex column, justify-end so messages sit at bottom; scroll to bottom on new message */}
      <div
        ref={messagesContainerRef}
        className="message-feed message-container message-list flex-1 overflow-y-auto min-h-0 p-6 sm:p-10 bg-white scroll-smooth flex flex-col gap-5"
        onScroll={(e) => {
          if (e.target.scrollTop === 0 && hasMore && !loadingMore) {
            handleLoadMore();
          }
        }}
      >
        {loadingMore && (
          <div className="text-center py-2 flex-shrink-0">
            <Loader2 className="w-5 h-5 animate-spin text-gray-400 mx-auto" />
          </div>
        )}

        {messages.length === 0 && !loading ? (
          <div className="flex items-center justify-center flex-1">
            <div className="text-center text-gray-500">
              <p>Greetings in the name of the Lord! Let&apos;s begin your registration.</p>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-4 mt-auto">
          {messages.map((msg) => {
            const currentUserId = getCurrentUserId();
            const isOwn = msg.senderUserId === currentUserId;
            return (
              <MessageBubble
                key={msg.id}
                message={msg.content}
                isOwn={isOwn}
                senderName={msg.senderName}
                time={msg.createdAt}
              />
            );
          })}
          </div>
        )}

        {sending && (
          <div className="flex justify-start">
            <div className="bg-white rounded-lg px-4 py-2 border border-gray-200">
              <Loader2 className="w-4 h-4 animate-spin text-gray-400" />
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="chat-input-wrapper input-area bg-white border-t border-gray-200 p-3 sm:px-10 sm:py-5 flex-shrink-0 safe-area-inset-bottom shadow-[0_-2px_12px_rgba(15,23,42,0.04)]">
        {conversationType === 'BOT' && quickReplyOptions.length > 0 && (
          <div className="mb-3 flex flex-wrap gap-2">
            {quickReplyOptions.map((opt, idx) => (
              <button
                key={`${opt.value}-${idx}`}
                type="button"
                disabled={sending}
                onClick={() => sendBotText(opt.value)}
                className="px-3 py-1.5 rounded-full text-sm font-medium bg-brand-red text-white border border-brand-red hover:bg-[#a01926] transition-colors disabled:opacity-50"
              >
                {opt.label}
              </button>
            ))}
          </div>
        )}
        {conversationType === 'BOT' && quickReplyOptions.length === 0 && quickReplies.length > 0 && (
          <div className="mb-3 flex flex-wrap gap-2">
            {quickReplies.map((label) => (
              <button
                key={label}
                type="button"
                disabled={sending}
                onClick={() => sendBotText(label)}
                className="px-3 py-1.5 rounded-full text-sm font-medium bg-slate-100 text-slate-800 border border-slate-200 hover:bg-brand-red hover:text-white hover:border-brand-red transition-colors disabled:opacity-50"
              >
                {label}
              </button>
            ))}
          </div>
        )}
        {/* Image upload button (shown when step requires image) */}
        {showImageUpload && (
          <div className="mb-2 space-y-2">
            {uploadingImage ? (
              <div className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium bg-gray-200 text-gray-500">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Uploading...</span>
              </div>
            ) : (
              <ImagePickButtons onFileSelected={uploadImageFile} disabled={uploadingImage} compact />
            )}
            <p className="text-xs text-gray-500">
              {currentStep === 'ASK_PERSON_IMAGE'
                ? 'Take a photo or choose from gallery. You can also use the 📎 icon.'
                : 'Upload your certificate photo using the buttons above or the 📎 icon.'}
            </p>
          </div>
        )}
        
        <form onSubmit={handleSend} className="input-container flex items-center gap-2 bg-[#f8f9fa] border border-gray-300 rounded-[25px] px-3 py-1">
          {/* Hidden file input for attachment */}
          <ImageAttachMenu
            onFileSelected={uploadImageFile}
            disabled={uploadingImage || !conversationId}
            title={showImageUpload ? 'Upload image' : 'Attach file'}
          />
          <div className="flex-1 relative">
            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSend(e);
                }
              }}
              placeholder="Type your message here..."
              className="w-full px-4 py-3 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-brand-red focus:border-transparent text-sm"
              disabled={sending}
            />
            {conversationType === 'BOT' && (
              <p className="absolute -bottom-5 left-3 text-xs text-gray-400 mt-1 hidden sm:block">
                Enter phone: 077xxxxxxx or +26377xxxxxxx
              </p>
            )}
          </div>
          <button
            type="submit"
            disabled={!inputMessage.trim() || sending}
            className="w-[45px] h-[45px] bg-brand-red text-white rounded-full hover:bg-brand-redDark disabled:opacity-50 disabled:cursor-not-allowed transition-colors touch-manipulation flex items-center justify-center flex-shrink-0"
          >
            {sending ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <Send className="w-5 h-5" />
            )}
          </button>
        </form>
        {conversationType === 'BOT' && (
          <p className="text-xs text-gray-400 mt-1 ml-12 sm:hidden">
            Phone: 077xxxxxxx or +26377xxxxxxx
          </p>
        )}
      </div>
    </div>
  );
};

export default ChatPanel;
