import api from './axios';

// Oxygen status
export const getOxygenStatus = (id) => api.get(`/patients/${id}/oxygen-status/`);

// AI Analysis
export const getAIAnalysis = (id) => api.get(`/patients/${id}/ai-analysis/`);
export const startAIAnalysis = (id) => api.post(`/patients/${id}/ai-analysis/`);

// ABG Trends
export const getABGTrends = (id) => api.get(`/patients/${id}/abg-trends/`);

// Hypoxemia
export const getHypoxemiaCause = (data) => api.post('/patient/hypoxemia-cause/', data);

// Oxygen requirement
export const getOxygenRequirement = (id) => api.get(`/patients/${id}/oxygen-requirement/`);
export const postOxygenRequirement = (data) => api.post('/patient/oxygen-requirement/', data);

// Device selection
export const getDeviceSelection = (id) => api.get(`/patients/${id}/device-selection/`);
export const postDeviceSelection = (data) => api.post('/patient/device-selection/', data);
export const getAIDeviceRecommendation = (id) => api.get(`/patient/device-recommendation/${id}/`);

// Review recommendation
export const getReviewRecommendation = (id) => api.get(`/patients/${id}/review-recommendation/`);

// Therapy
export const getTherapyRecommendation = (id) => api.get(`/patients/${id}/therapy-recommendation/`);
export const getTherapyRecommendations = (id) => api.get(`/patients/${id}/therapy-recommendation/`);
export const getNIVRecommendation = (id) => api.get(`/patients/${id}/niv-recommendation/`);
export const getEscalationCriteria = (id) => api.get(`/patients/${id}/escalation-criteria/`);
export const scheduleReassessment = (id, data) => api.post(`/patients/${id}/schedule-reassessment/`, data);
export const getUrgentAction = (id) => api.get(`/patients/${id}/urgent-action/`);

export const acceptTherapy = (id, recommendationId, data = {}) =>
  api.post(`/patients/${id}/accept-therapy/`, { recommendation_id: recommendationId, ...data });

export const getStaffList = () => api.get('/staff-list/');
