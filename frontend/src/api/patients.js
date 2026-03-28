import api from './axios';

// Patient list & CRUD
export const getPatients = () => api.get('/patients/');
export const addPatient = (data) => api.post('/patients/', data);
export const getPatientDetail = (id) => api.get(`/patients/${id}/`);
export const getPatientDetails = (id) => api.get(`/patients/${id}/`);
export const getPatientDetailsForDoctor = (id) => api.get(`/patient/details/${id}/`);
export const deletePatient = (id) => api.delete(`/patients/${id}/`);

// Clinical data entry
export const addBaselineDetails = (data) => api.post(`/patients/${data.patient || data.patient_id}/baseline/`, data);
export const addGoldClassification = (data) => api.post(`/patients/${data.patient || data.patient_id}/baseline/`, data); // Fallback
export const addSpirometry = (data) => api.post(`/patients/${data.patient || data.patient_id}/spirometry/`, data);
export const addGasExchangeHistory = (data) => api.post(`/patients/${data.patient || data.patient_id}/baseline/`, data); // Fallback
export const addCurrentSymptoms = (data) => api.post(`/patients/${data.patient || data.patient_id}/symptoms/`, data);
export const addVitals = (data) => api.post(`/patients/${data.patient || data.patient_id}/vitals/`, data);
export const addAbgEntry = (data) => api.post(`/patients/${data.patient || data.patient_id}/abg/`, data);

// Vitals & ABG getters (aliases for tab imports)
export const getPatientVitals = (id) => api.get(`/patients/${id}/vitals/`);
export const getPatientABGs = (id) => api.get(`/patients/${id}/abg/`);
export const addABG = (data) => api.post(`/patients/${data.patient || data.patient_id}/abg/`, data);

// Reassessment
export const getReassessmentChecklist = (id) => api.get(`/patients/${id}/reassessment-checklist/`);
export const addReassessment = (id, data) => api.post(`/patients/${id}/reassessment/`, data);

// AI & Decision Support
export const getAIRisk = (id) => api.get(`/patient/ai-risk/${id}/`);
export const getDecisionSupport = (id) => api.get(`/patient/decision-support/${id}/`);
export const getTrendAnalysis = (id) => api.get(`/patient/trend-analysis/${id}/`);
export const getClinicalReview = (id) => api.get(`/patient/clinical-review/${id}/`);
export const getClinicalTherapy = (id) => api.get(`/patient/clinical-therapy/${id}/`);
export const getClinicalReassessment = (id) => api.get(`/patient/clinical-reassessment/${id}/`);

