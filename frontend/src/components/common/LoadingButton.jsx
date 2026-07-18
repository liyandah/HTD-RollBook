import React from 'react';

/**
 * Button with integrated loading state
 * @param {boolean} loading - Whether button is in loading state
 * @param {boolean} disabled - Whether button is disabled
 * @param {string} className - Additional CSS classes
 * @param {ReactNode} children - Button content
 * @param {ReactNode} loadingText - Text to show when loading (optional)
 */
const LoadingButton = ({ 
  loading = false, 
  disabled = false, 
  className = '', 
  children, 
  loadingText,
  ...props 
}) => {
  return (
    <button
      disabled={loading || disabled}
      className={`relative ${className} ${loading || disabled ? 'opacity-75 cursor-not-allowed' : ''}`}
      {...props}
    >
      {loading ? (
        <span className="flex items-center justify-center gap-2">
          {/* Spinner */}
          <svg 
            className="animate-spin h-5 w-5" 
            xmlns="http://www.w3.org/2000/svg" 
            fill="none" 
            viewBox="0 0 24 24"
          >
            <circle 
              className="opacity-25" 
              cx="12" 
              cy="12" 
              r="10" 
              stroke="currentColor" 
              strokeWidth="4"
            />
            <path 
              className="opacity-75" 
              fill="currentColor" 
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
          <span>{loadingText || 'Loading...'}</span>
        </span>
      ) : (
        children
      )}
    </button>
  );
};

export default LoadingButton;






