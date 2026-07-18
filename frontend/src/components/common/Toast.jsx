import React, { useEffect } from 'react';
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-react';

/**
 * Modern toast notification component
 * @param {string} message - Message to display
 * @param {string} type - 'success', 'error', 'warning', 'info'
 * @param {function} onClose - Close callback
 * @param {number} duration - Auto-close duration in ms (default: 4000)
 */
const Toast = ({ message, type = 'info', onClose, duration = 4000 }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const getTypeConfig = () => {
    switch (type) {
      case 'success':
        return {
          bg: 'bg-emerald-600',
          icon: CheckCircle,
          iconBg: 'bg-emerald-500',
          progress: 'bg-emerald-400',
        };
      case 'error':
        return {
          bg: 'bg-brand-red',
          icon: XCircle,
          iconBg: 'bg-red-700',
          progress: 'bg-red-400',
        };
      case 'warning':
        return {
          bg: 'bg-amber-500',
          icon: AlertTriangle,
          iconBg: 'bg-amber-600',
          progress: 'bg-amber-300',
        };
      default:
        return {
          bg: 'bg-blue-600',
          icon: Info,
          iconBg: 'bg-blue-500',
          progress: 'bg-blue-400',
        };
    }
  };

  const config = getTypeConfig();
  const Icon = config.icon;

  return (
    <div className="fixed bottom-8 right-8 z-50 animate-fadeIn">
      <div className={`${config.bg} text-white rounded-2xl shadow-2xl overflow-hidden max-w-md`}>
        <div className="flex items-center gap-4 px-6 py-4">
          {/* Icon */}
          <div className={`${config.iconBg} p-2 rounded-xl`}>
            <Icon size={20} strokeWidth={2.5} />
          </div>

          {/* Message */}
          <p className="flex-1 font-semibold text-sm">{message}</p>

          {/* Close Button */}
          <button
            onClick={onClose}
            className="p-1 hover:bg-white/20 rounded-lg transition-colors"
            aria-label="Close notification"
          >
            <X size={18} strokeWidth={2.5} />
          </button>
        </div>

        {/* Progress Bar */}
        <div className="h-1 bg-black/20">
          <div
            className={`h-full ${config.progress} animate-shrink`}
            style={{
              animationDuration: `${duration}ms`,
            }}
          ></div>
        </div>
      </div>
    </div>
  );
};

export default Toast;

