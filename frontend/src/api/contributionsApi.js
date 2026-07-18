import http from './apiClient';

// Payments
export const createPayment = (data) => http.post('/api/payments', data);
export const getPayments = (page = 0, size = 20) => 
  http.get('/api/payments', { params: { page, size } });
export const getPaymentById = (id) => http.get(`/api/payments/${id}`);
export const getPaymentsByMember = (memberId, page = 0, size = 20) => 
  http.get(`/api/payments/member/${memberId}`, { params: { page, size } });
export const getPaymentsByCategory = (categoryId, page = 0, size = 20) => 
  http.get(`/api/payments/category/${categoryId}`, { params: { page, size } });
export const getPaymentsByProject = (projectId, page = 0, size = 20) => 
  http.get(`/api/payments/project/${projectId}`, { params: { page, size } });
export const getPaymentsByEvent = (eventId, page = 0, size = 20) => 
  http.get(`/api/payments/event/${eventId}`, { params: { page, size } });

// Projects
export const getProjects = () => http.get('/api/projects');
export const getActiveProjects = () => http.get('/api/projects/active');
export const getProjectById = (id) => http.get(`/api/projects/${id}`);
export const createProject = (data) => http.post('/api/projects', data);
export const updateProject = (id, data) => http.put(`/api/projects/${id}`, data);
export const deleteProject = (id) => http.delete(`/api/projects/${id}`);

// Events
export const getEvents = () => http.get('/api/events');
export const getActiveEvents = () => http.get('/api/events/active');
export const getEventById = (id) => http.get(`/api/events/${id}`);
export const createEvent = (data) => http.post('/api/events', data);
export const updateEvent = (id, data) => http.put(`/api/events/${id}`, data);
export const deleteEvent = (id) => http.delete(`/api/events/${id}`);

// Categories
export const getCategories = () => http.get('/api/contribution-categories');
export const getActiveCategories = () => http.get('/api/contribution-categories/active');
export const getCategoriesByType = (type) => 
  http.get(`/api/contribution-categories/type/${type}`);

// Notifications
export const getNotifications = (page = 0, size = 20) => 
  http.get('/api/notifications', { params: { page, size } });
export const getUnreadNotifications = () => http.get('/api/notifications/unread');
export const getUnreadCount = () => http.get('/api/notifications/unread-count');
export const markNotificationAsRead = (id) => http.put(`/api/notifications/${id}/read`);
export const markAllNotificationsAsRead = () => http.put('/api/notifications/read-all');

// Contributions Overview
export const getContributionsOverview = () => http.get('/api/contributions/overview');

// Members search (using existing UserController)
export const searchMembers = (query) => http.get('/api/users/search', { params: { q: query } });
