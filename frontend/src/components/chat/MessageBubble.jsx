import React from 'react';

const MessageBubble = ({ message, isOwn, senderName, time }) => {
  const formatTime = (date) => {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} items-end gap-2`}>
      {!isOwn && (
        <div className="w-7 h-7 rounded-lg bg-brand-red text-white text-[10px] font-bold flex items-center justify-center flex-shrink-0">
          HTF
        </div>
      )}
      <div className={`max-w-[70%] ${isOwn ? 'order-2' : 'order-1'}`}>
        {!isOwn && senderName && (
          <div className="text-xs text-gray-500 mb-1 px-1 sm:px-2">{senderName}</div>
        )}
        <div
          className={`rounded-[18px] px-4 py-3 shadow-sm ${
            isOwn
              ? 'bg-brand-red text-white rounded-br-[4px]'
              : 'bg-[#f0f2f5] text-gray-900 border border-gray-200 rounded-bl-[4px]'
          }`}
        >
          <p className="text-[0.95rem] whitespace-pre-wrap break-words leading-relaxed">{message}</p>
          <div
            className={`text-xs mt-1 ${
              isOwn ? 'text-white/70' : 'text-gray-500/80'
            }`}
          >
            {formatTime(time)}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MessageBubble;
