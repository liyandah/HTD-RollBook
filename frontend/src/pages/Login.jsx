import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import http from '../api/apiClient';
import { setAuthData, isAuthenticated } from '../utils/auth';

const SHIELD_LOGO_SRC = '/shield.jpg?v=20260414';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [logoLoadFailed, setLogoLoadFailed] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated()) {
      navigate('/dashboard');
    }
  }, [navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await http.post('/api/auth/login', {
        username,
        password,
      });

      setAuthData(response.data.token, response.data.username);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[radial-gradient(circle_at_center,_#3a5a8c_0%,_#001a3d_100%)] animate-fadeIn px-6">
      <div className="w-full max-w-md text-center text-white">
        <div className="mb-10">
          <div className="w-[90px] h-[90px] border-2 border-white/80 rounded-full mx-auto mb-5 flex items-center justify-center bg-white/10 overflow-hidden">
            {!logoLoadFailed ? (
              <img
                src={SHIELD_LOGO_SRC}
                alt="Salvation Army Shield"
                className="w-full h-full object-cover"
                onError={() => setLogoLoadFailed(true)}
              />
            ) : (
              <span className="text-2xl font-semibold tracking-wide">HTE</span>
            )}
          </div>
          <h1 className="text-[1.8rem] font-normal tracking-wide uppercase text-white mb-1">HT-E Roll Book</h1>
          <p className="text-sm text-white/70">Data Collection Dashboard</p>
        </div>

        <form onSubmit={handleSubmit}>
          {error && (
            <div className="mb-5 border border-[#c62828] bg-[rgba(198,40,40,0.2)] text-[#ffcdd2] px-4 py-3 rounded animate-shake text-left">
              {error}
            </div>
          )}

          <div className="mb-7 border-b border-white/60 flex items-center pb-2">
            <span className="mr-3 text-white/80 text-base" aria-hidden="true">✉</span>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full bg-transparent border-none outline-none text-white text-base placeholder:text-white/60"
              placeholder="Username"
              required
            />
          </div>

          <div className="mb-7 border-b border-white/60 flex items-center pb-2">
            <span className="mr-3 text-white/80 text-base" aria-hidden="true">🔒</span>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-transparent border-none outline-none text-white text-base placeholder:text-white/60"
              placeholder="Password"
              required
            />
          </div>

          <div className="flex justify-between items-center text-sm mb-8 text-white/85">
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" className="accent-brand-red" />
              <span>Remember me</span>
            </label>
            <a href="#" className="italic hover:text-white transition-colors">Forgot Password??</a>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 bg-brand-red hover:bg-[#a01926] text-white font-semibold tracking-wider transition-all duration-300 shadow-lg hover:-translate-y-0.5 disabled:opacity-70 disabled:cursor-not-allowed disabled:transform-none"
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Logging in...
              </span>
            ) : (
              'Login'
            )}
          </button>
        </form>

      </div>
    </div>
  );
};

export default Login;

