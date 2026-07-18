import http from '../api/apiClient';

let cachedUser = null;
let cacheTimestamp = null;
const CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

export const getCurrentUser = async () => {
  const now = Date.now();
  
  // Return cached user if still valid
  if (cachedUser && cacheTimestamp && (now - cacheTimestamp) < CACHE_DURATION) {
    return cachedUser;
  }
  
  try {
    const response = await http.get('/api/users/me');
    cachedUser = response.data;
    cacheTimestamp = now;
    return cachedUser;
  } catch (error) {
    console.error('Failed to fetch current user:', error);
    // Return a default user object if lookup fails
    // Edge case: token present but user not yet in users table
    const defaultUser = {
      id: localStorage.getItem('userId'),
      email: localStorage.getItem('userEmail') || '',
      fullName: localStorage.getItem('userName') || '',
      role: 'VIEWER' // Default to VIEWER for safety
    };
    cachedUser = defaultUser;
    cacheTimestamp = now;
    return defaultUser;
  }
};

export const getUserRole = async () => {
  const user = await getCurrentUser();
  return user?.role || 'VIEWER';
};

export const hasRole = async (...roles) => {
  const userRole = await getUserRole();
  return roles.includes(userRole);
};

export const clearUserCache = () => {
  cachedUser = null;
  cacheTimestamp = null;
};
