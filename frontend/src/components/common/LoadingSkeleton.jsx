import React from 'react';

/**
 * Modern skeleton loader component
 * @param {string} variant - 'card', 'table', 'list', 'stat' (default: 'list')
 * @param {number} count - Number of skeleton items to show
 */
const LoadingSkeleton = ({ variant = 'list', count = 5 }) => {
  // Stat Card Skeleton (for Dashboard stats)
  if (variant === 'stat') {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {Array.from({ length: count }).map((_, index) => (
          <div key={index} className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm animate-pulse">
            <div className="flex items-center justify-between">
              <div className="space-y-3 flex-1">
                <div className="h-2 w-20 bg-slate-200 rounded"></div>
                <div className="h-8 w-24 bg-slate-200 rounded"></div>
              </div>
              <div className="h-12 w-12 bg-slate-100 rounded-xl"></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  // Table Skeleton (for data tables)
  if (variant === 'table') {
    return (
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden animate-pulse">
        <div className="p-6 space-y-4">
          {Array.from({ length: count }).map((_, index) => (
            <div key={index} className="flex items-center gap-4">
              <div className="w-10 h-10 bg-slate-200 rounded-full"></div>
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-slate-200 rounded w-3/4"></div>
                <div className="h-3 bg-slate-100 rounded w-1/2"></div>
              </div>
              <div className="w-20 h-6 bg-slate-200 rounded-lg"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // Card Skeleton (for card layouts)
  if (variant === 'card') {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {Array.from({ length: count }).map((_, index) => (
          <div key={index} className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm animate-pulse">
            <div className="space-y-4">
              <div className="h-32 bg-slate-200 rounded-xl"></div>
              <div className="space-y-2">
                <div className="h-4 bg-slate-200 rounded w-3/4"></div>
                <div className="h-3 bg-slate-100 rounded w-1/2"></div>
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  // List Skeleton (default)
  return (
    <div className="space-y-4 animate-pulse">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="bg-white rounded-xl shadow-sm border border-slate-100 p-4">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-slate-200 rounded-full"></div>
            <div className="flex-1 space-y-2">
              <div className="h-4 bg-slate-200 rounded w-3/4"></div>
              <div className="h-3 bg-slate-100 rounded w-1/2"></div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default LoadingSkeleton;

