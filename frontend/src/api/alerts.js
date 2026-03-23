import api from './axios';

// Doctor alerts
export const getDoctorAlerts = () => api.get('/alerts/');

// Staff alerts
export const getStaffAlerts = () => api.get('/alerts/');

// Notifications
export const getNotifications = () => api.get('/notifications/');

// Settings
export const getSettings = () => api.get('/settings/');
export const getClinicalGuidelines = () => api.get('/clinical-guidelines/');
export const getHelpSupport = () => api.get('/help-support/');

// Update Alert
export const updateAlert = (id, action) => api.post(`/alerts/${id}/update/`, { action });
