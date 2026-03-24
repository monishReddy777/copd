import api from './axios';

// Unified login
export const unifiedLogin = (data) => api.post('/login/', data);

// Doctor auth
export const doctorLogin = (data) => api.post('/auth/login/', data);
export const doctorSignup = (data) => api.post('/doctor/signup/', data);
export const doctorForgotPassword = (data) => api.post('/forgot-password/', data);
export const doctorResetPassword = (data) => api.post('/reset-password/', data);
export const doctorVerifyOTP = (data) => api.post('/auth/verify-otp/', data);

// Staff auth
export const staffLogin = (data) => api.post('/auth/login/', data);
export const staffSignup = (data) => api.post('/staff/signup/', data);
export const staffForgotPassword = (data) => api.post('/forgot-password/', data);
export const staffVerifyOTP = (data) => api.post('/auth/verify-otp/', data);
export const staffResetPassword = (data) => api.post('/reset-password/', data);

// Admin auth
export const adminLogin = (data) => api.post('/auth/login/', data);

// Register (general)
export const register = (data) => api.post('/register/', data);

// Accept terms
export const acceptTerms = (data) => api.post('/accept-terms/', data);

// Forgot password (general)
export const forgotPassword = (data) => api.post('/forgot-password/', data);
export const verifyOTP = (data) => api.post('/forgot-password/verify-otp/', data);
export const resetPassword = (data) => api.post('/reset-password/', data);

// Profile
export const updateProfile = (data) => api.put('/update-profile/', data);

// OTP Verification (New)
export const requestOTP = (data) => api.post('/auth/request-otp/', data);
export const verifyEmailOTP = (data) => api.post('/auth/verify-otp/', data);
