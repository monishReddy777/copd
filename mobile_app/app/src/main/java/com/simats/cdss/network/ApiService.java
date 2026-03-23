package com.simats.cdss.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import com.simats.cdss.models.*;

public interface ApiService {

    // ─────────────────────────────────────────
    // 1. Authentication (Admin / Doctor / Staff)
    // ─────────────────────────────────────────

    // Unified Login (Doctor / Staff)
    @POST("api/login/")
    Call<LoginResponse> unifiedLogin(@Body Map<String, String> request);

    // Admin Login
    @POST("api/admin/login/")
    Call<LoginResponse> adminLogin(@Body Map<String, String> request);

    // Doctor Signup
    @POST("api/doctor/signup/")
    Call<GenericResponse> doctorSignup(@Body SignupRequest request);

    // Doctor Login
    @POST("api/doctor/login/")
    Call<LoginResponse> doctorLogin(@Body Map<String, String> request);

    // Doctor Verify OTP
    @POST("api/doctor/verify-otp/")
    Call<LoginResponse> verifyDoctorOtp(@Body Map<String, String> request);

    // Staff Signup
    @POST("api/staff/signup/")
    Call<GenericResponse> staffSignup(@Body SignupRequest request);

    // Staff Login
    @POST("api/staff/login/")
    Call<LoginResponse> staffLogin(@Body Map<String, String> request);

    // Staff Verify OTP
    @POST("api/staff/verify-otp/")
    Call<LoginResponse> verifyStaffOtp(@Body Map<String, String> request);

    // Accept Terms & Conditions
    @POST("api/accept-terms/")
    Call<LoginResponse> acceptTerms(@Body Map<String, String> request);

    // Forgot Password - sends OTP to email
    @POST("api/forgot-password/")
    Call<LoginResponse> forgotPassword(@Body Map<String, String> request);

    // Forgot Password - verify OTP
    @POST("api/forgot-password/verify-otp/")
    Call<LoginResponse> forgotPasswordVerifyOtp(@Body Map<String, String> request);

    // Reset Password - updates password
    @POST("api/reset-password/")
    Call<LoginResponse> resetPassword(@Body Map<String, String> request);

    // Universal Profile Update
    @POST("api/update-profile/")
    Call<Map<String, String>> updateProfile(@Body Map<String, String> request);


    // ─────────────────────────────────────────
    // 2. Patient Management
    // ─────────────────────────────────────────

    @GET("api/patients/")
    Call<List<PatientResponse>> getPatients();

    @POST("api/patients/add/")
    Call<PatientResponse> addPatient(@Body PatientRequest request);

    @GET("api/patient/details/{id}/")
    Call<PatientDetailResponse> getPatientDetails(@Path("id") int patientId);

    @GET("api/patient/ai-risk/{id}/")
    Call<AIRiskResponse> getPatientAIRisk(@Path("id") int patientId);

    @GET("api/patient/trend-analysis/{id}/")
    Call<TrendAnalysisResponse> getPatientTrendAnalysis(@Path("id") int patientId);

    @GET("api/patient/decision-support/{id}/")
    Call<DecisionSupportResponse> getPatientDecisionSupport(@Path("id") int patientId);

    @POST("api/patient/hypoxemia-cause/")
    Call<GenericResponse> setHypoxemiaCause(@Body Map<String, Object> request);

    @POST("api/patient/oxygen-requirement/")
    Call<GenericResponse> setOxygenRequirement(@Body Map<String, Object> request);

    @GET("api/patient/device-recommendation/{id}/")
    Call<DeviceRecommendationResponse> getDeviceRecommendation(@Path("id") int patientId);

    @POST("api/patient/device-selection/")
    Call<GenericResponse> saveDeviceSelection(@Body Map<String, Object> request);

    // ── Clinical Decision Support Flow ──

    @GET("api/patient/clinical-review/{id}/")
    Call<ClinicalReviewResponse> getClinicalReview(@Path("id") int patientId);

    @POST("api/patient/clinical-review/{id}/")
    Call<GenericResponse> saveClinicalReview(@Path("id") int patientId, @Body Map<String, Object> request);

    @GET("api/patient/clinical-therapy/{id}/")
    Call<TherapyPlanResponse> getTherapyPlan(@Path("id") int patientId);

    @GET("api/patients/{id}/abg-trends/")
    Call<ABGTrendResponse> getABGTrends(@Path("id") int patientId, @retrofit2.http.Query("filter") String filter);

    @POST("api/patient/clinical-reassessment/{id}/")
    Call<GenericResponse> saveReassessment(@Path("id") int patientId, @Body Map<String, Object> request);

    // ── Reassessment Checklist ──

    @POST("api/reassessment/")
    Call<GenericResponse> saveReassessmentChecklist(@Body Map<String, Object> request);

    @GET("api/reassessment/")
    Call<ReassessmentResponse> getLatestReassessment(@retrofit2.http.Query("patient_id") int patientId);

    // ── Schedule Reassessment ──

    @POST("api/schedule-reassessment/")
    Call<GenericResponse> saveScheduleReassessment(@Body Map<String, Object> request);

    @GET("api/schedule-reassessment/")
    Call<ScheduleReassessmentListResponse> getPendingReassessments();

    // ── Staff Checklist (saves reassessment values entered by staff) ──

    @POST("api/staff-checklist/")
    Call<GenericResponse> saveStaffChecklist(@Body Map<String, Object> request);

    // ── Doctor view: staff-entered reassessment values ──

    @GET("api/patient/staff-reassessments/{id}/")
    Call<StaffReassessmentValuesResponse> getStaffReassessmentValues(@Path("id") int patientId);

    @POST("api/baseline-details/add/")
    Call<GenericResponse> addBaselineDetails(@Body Map<String, Object> request);

    @POST("api/gold-classification/add/")
    Call<GenericResponse> addGoldClassification(@Body Map<String, Object> request);

    @POST("api/spirometry/add/")
    Call<GenericResponse> addSpirometryData(@Body Map<String, Object> request);

    @POST("api/gas-exchange-history/add/")
    Call<GenericResponse> addGasExchangeHistory(@Body Map<String, Object> request);

    @POST("api/current-symptoms/add/")
    Call<GenericResponse> addCurrentSymptoms(@Body Map<String, Object> request);


    // ─────────────────────────────────────────
    // 3. Clinical Data
    // ─────────────────────────────────────────

    @POST("api/abg-entry/add/")
    Call<GenericResponse> addAbgEntry(@Body java.util.Map<String, Object> request);

    @POST("api/vitals/add/")
    Call<GenericResponse> addVitalsData(@Body java.util.Map<String, Object> request);

    @PUT("api/staff/update-vitals/{id}/")
    Call<GenericResponse> updateStaffVitals(@Path("id") int patientId, @Body java.util.Map<String, Object> request);

    @PUT("api/staff/update-abg/{id}/")
    Call<GenericResponse> updateStaffAbg(@Path("id") int patientId, @Body java.util.Map<String, Object> request);


    // ─────────────────────────────────────────
    // 4. Alerts & Recommendations
    // ─────────────────────────────────────────

    @GET("api/patients/{id}/escalation-criteria/")
    Call<EscalationCriteriaResponse> getEscalationCriteria(@Path("id") int patientId);

    @GET("api/patients/{id}/niv-recommendation/")
    Call<NIVRecommendationResponse> getNIVRecommendation(@Path("id") int patientId);

    @GET("api/patients/{id}/urgent-action/")
    Call<UrgentActionResponse> getUrgentAction(@Path("id") int patientId);

    @DELETE("api/patients/{id}/")
    Call<GenericResponse> deletePatient(@Path("id") int patientId);

    // Staff Reassessment Alerts
    @GET("api/staff/alerts/")
    Call<com.simats.cdss.models.StaffAlertsResponse> getStaffAlerts();

    @POST("api/staff/alerts/")
    Call<GenericResponse> postStaffAlertAction(@Body Map<String, Object> request);

    // Doctor Alerts
    @GET("api/doctor/alerts/")
    Call<com.simats.cdss.models.DoctorAlertsResponse> getDoctorAlerts();

    @POST("api/doctor/alerts/")
    Call<GenericResponse> postDoctorAlertAction(@Body Map<String, Object> request);

    @GET("api/notifications/")
    Call<List<AlertResponse>> getAlerts();

    @GET("api/patients/{id}/therapy-recommendation/")
    Call<List<RecommendationResponse>> getPatientRecommendations(
            @Path("id") int patientId
    );

    @POST("api/patients/{id}/review-recommendation/")
    Call<GenericResponse> handleRecommendation(
            @Path("id") int patientId,
            @Body HandleRecommendationRequest request
    );


    // ─────────────────────────────────────────
    // 5. Admin Profile
    // ─────────────────────────────────────────

    @GET("api/admin/profile/")
    Call<AdminProfileResponse> getAdminProfile();


    // ─────────────────────────────────────────
    // 6. Admin Approval Requests
    // ─────────────────────────────────────────

    @GET("api/admin/approval-requests/")
    Call<List<ApprovalRequest>> getApprovalRequests();

    @POST("api/admin/approve-user/")
    Call<GenericResponse> approveUser(@Body Map<String, Object> request);

    @POST("api/admin/reject-user/")
    Call<GenericResponse> rejectUser(@Body Map<String, Object> request);


    // ─────────────────────────────────────────
    // 7. Admin Manage Doctors
    // ─────────────────────────────────────────

    @GET("api/admin/doctors/")
    Call<List<Doctor>> getDoctors();

    @GET("api/admin/doctors/{id}/")
    Call<DoctorDetailResponse> getDoctorDetails(@Path("id") int doctorId);

    @POST("api/admin/doctors/toggle-status/")
    Call<GenericResponse> toggleDoctorStatus(@Body Map<String, Object> request);

    @DELETE("api/admin/doctors/{id}/")
    Call<GenericResponse> deleteDoctor(@Path("id") int doctorId);


    // ─────────────────────────────────────────
    // 8. Admin Manage Staff
    // ─────────────────────────────────────────

    @GET("api/admin/staff/")
    Call<List<Staff>> getStaff();

    @GET("api/admin/staff/{id}/")
    Call<StaffDetailResponse> getStaffDetails(@Path("id") int staffId);

    @POST("api/admin/staff/toggle-status/")
    Call<GenericResponse> toggleStaffStatus(@Body Map<String, Object> request);

    @DELETE("api/admin/staff/{id}/")
    Call<GenericResponse> deleteStaff(@Path("id") int staffId);


    // ─────────────────────────────────────────
    // 9. Admin Dashboard Metrics
    // ─────────────────────────────────────────

    @GET("api/system-statistics/")
    Call<DashboardStatsResponse> getAdminDashboardStats();

    // ─────────────────────────────────────────
    // 10. Doctor Dashboard
    // ─────────────────────────────────────────

    @GET("api/doctor/dashboard/")
    Call<DoctorDashboardResponse> getDoctorDashboard(@retrofit2.http.Query("email") String email);


    // ─────────────────────────────────────────
    // 11. Staff Dashboard
    // ─────────────────────────────────────────

    @GET("api/staff/dashboard/")
    Call<StaffDashboardResponse> getStaffDashboard(@retrofit2.http.Query("email") String email);
}