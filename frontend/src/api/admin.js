import api from './axios';

// Admin auth & dashboard
export const adminLogin = (data) => api.post('/auth/login/', data);
export const getAdminDashboard = () => api.get('/system-statistics/');
export const getSystemStatistics = () => api.get('/system-statistics/');
export const getAdminProfile = () => api.get('/auth/profile/');

// Manage doctors
export const getAdminDoctors = () => api.get('/admin/doctors/');
export const getAdminDoctorDetail = (id) => api.get(`/admin/doctors/${id}/`);
export const toggleDoctorStatus = (id) => api.patch(`/admin/doctors/${id}/toggle/`);
export const deleteDoctor = (id) => api.delete(`/admin/doctors/${id}/`);

// Manage staff
export const getAdminStaff = () => api.get('/admin/staff/');
export const getAdminStaffDetail = (id) => api.get(`/admin/staff/${id}/`);
export const toggleStaffStatus = (id) => api.patch(`/admin/staff/${id}/toggle/`);
export const deleteStaff = (id) => api.delete(`/admin/staff/${id}/`);

// Approvals
export const getApprovalRequests = () => api.get('/admin/approval-requests/');
export const approveUser = (data) => api.post('/admin/approve-user/', data);
export const rejectUser = (data) => api.post('/admin/reject-user/', data);

// Doctor dashboard
export const getDoctorDashboard = () => api.get('/system-statistics/');
export const getDoctorProfile = () => api.get('/auth/profile/');

// Staff dashboard
export const getStaffDashboard = () => api.get('/system-statistics/');
export const getStaffProfile = () => api.get('/auth/profile/');
export const updateStaffVitals = (id, data) => api.post(`/patients/${id}/vitals/`, data);
export const updateStaffAbg = (id, data) => api.post(`/patients/${id}/abg/`, data);

// Reassessment
export const getReassessments = () => api.get('/reassessment/');
export const postScheduleReassessment = (patientId, data) => api.post(`/patients/${patientId}/schedule-reassessment/`, data);
export const getStaffChecklist = () => api.get('/staff-checklist/');
export const getStaffReassessmentValues = (id) => api.get(`/patient/staff-reassessments/${id}/`);
export const getReassessmentSchedule = (patientId) => api.get(`/patients/${patientId}/schedule-reassessment/`);
export const getStaffList = () => api.get('/staff-list/');
