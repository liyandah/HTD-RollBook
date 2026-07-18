import React from 'react';

/**
 * Modern horizontal tabs component for inner page navigation
 * 
 * @param {Array} tabs - Array of tab objects with { id, label, icon (optional), count (optional) }
 * @param {String} activeTab - Currently active tab id
 * @param {Function} onChange - Callback when tab is clicked
 * @param {String} variant - 'pills' (default) or 'underline'
 */
const Tabs = ({ tabs, activeTab, onChange, variant = 'pills' }) => {
  if (variant === 'underline') {
    return (
      <div className="border-b border-slate-200 bg-white">
        <nav className="flex gap-8 px-6" aria-label="Tabs">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;

            return (
              <button
                key={tab.id}
                onClick={() => onChange(tab.id)}
                className={`
                  relative flex items-center gap-2 py-4 font-semibold text-sm transition-all duration-200 group
                  ${isActive 
                    ? 'text-brand-red' 
                    : 'text-slate-500 hover:text-slate-700'}
                `}
              >
                {Icon && (
                  <Icon 
                    size={18} 
                    className={`transition-transform duration-200 ${
                      isActive ? 'scale-110' : 'group-hover:scale-110'
                    }`}
                    strokeWidth={2.5}
                  />
                )}
                
                <span className="whitespace-nowrap">{tab.label}</span>
                
                {tab.count !== undefined && tab.count > 0 && (
                  <span className={`
                    inline-flex items-center justify-center min-w-[20px] h-5 px-2 rounded-full text-[10px] font-bold transition-all duration-200
                    ${isActive 
                      ? 'bg-brand-red text-white scale-110' 
                      : 'bg-slate-100 text-slate-600 group-hover:bg-slate-200'}
                  `}>
                    {tab.count}
                  </span>
                )}

                {/* Animated Underline - Only show on active */}
                {isActive && (
                  <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-brand-red rounded-t-full animate-fadeIn" />
                )}
                
                {/* Hover Underline - Show on hover for inactive */}
                {!isActive && (
                  <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-slate-300 rounded-t-full opacity-0 group-hover:opacity-100 transition-opacity duration-200" />
                )}
              </button>
            );
          })}
        </nav>
      </div>
    );
  }

  // Pills variant (default)
  return (
    <div className="inline-flex items-center gap-2 p-1.5 bg-slate-100 rounded-xl">
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const isActive = activeTab === tab.id;

        return (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            className={`
              relative flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-sm transition-all duration-200 group
              ${isActive 
                ? 'bg-white text-brand-navy shadow-md' 
                : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'}
            `}
          >
            {Icon && (
              <Icon 
                size={16} 
                className={`transition-transform duration-200 ${
                  isActive 
                    ? 'text-brand-red scale-110' 
                    : 'group-hover:scale-110'
                }`}
                strokeWidth={2.5}
              />
            )}
            
            <span className="font-semibold whitespace-nowrap">{tab.label}</span>
            
            {tab.count !== undefined && (
              <span className={`
                inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full text-xs font-bold
                ${isActive 
                  ? 'bg-brand-red text-white' 
                  : 'bg-slate-200 text-slate-600 group-hover:bg-slate-300'}
              `}>
                {tab.count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
};

export default Tabs;

