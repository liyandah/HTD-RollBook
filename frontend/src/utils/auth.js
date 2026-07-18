export const isAuthenticated = () => {
  return !!localStorage.getItem('token');
};

export const getToken = () => {
  return localStorage.getItem('token');
};

export const getUsername = () => {
  return localStorage.getItem('username');
};

export const setAuthData = (token, username) => {
  localStorage.setItem('token', token);
  localStorage.setItem('username', username);
};

export const clearAuthData = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('userId');
  localStorage.removeItem('userEmail');
  localStorage.removeItem('userName');
};

export const logout = () => {
  clearAuthData();
};


