/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Salvation Army Brand Colors
        brand: {
          red: '#C61F2C',        // Primary Salvation Army Red
          redDark: '#A01924',    // Darker red for hover states
          redLight: '#E8475C',   // Lighter red for accents
          navy: '#002D72',       // Deep Navy for headers/sidebars
          navyDark: '#001A41',   // Darkest Navy for backgrounds
          navyLight: '#003D8F',  // Lighter navy for hover states
          yellow: '#FFB81C',     // Salvation Army Gold/Yellow
          yellowDark: '#E5A419', // Darker yellow for hover
        },
        // Surface colors for backgrounds and borders
        surface: {
          50: '#F8FAFC',         // Lightest background
          100: '#F1F5F9',        // Input fields, cards
          200: '#E2E8F0',        // Borders, dividers
          300: '#CBD5E1',        // Hover borders
          400: '#94A3B8',        // Muted text
          500: '#64748B',        // Secondary text
        },
        // Legacy colors (for backward compatibility)
        'sa-red': '#C8102E',
        'sa-blue': '#0033A0',
        'sa-yellow': '#FFB81C',
      },
      borderRadius: {
        'xl': '12px',
        '2xl': '16px',
        '3xl': '24px',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        display: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        'soft': '0 2px 8px rgba(0, 0, 0, 0.04)',
        'medium': '0 4px 16px rgba(0, 0, 0, 0.08)',
        'large': '0 8px 32px rgba(0, 0, 0, 0.12)',
      },
      keyframes: {
        'slide-in-right': {
          '0%': { transform: 'translateX(100%)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' },
        },
        'fade-in': {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
      },
      animation: {
        'slide-in-right': 'slide-in-right 0.3s ease-out',
        'fade-in': 'fade-in 0.2s ease-out',
      },
    },
  },
  plugins: [],
}

