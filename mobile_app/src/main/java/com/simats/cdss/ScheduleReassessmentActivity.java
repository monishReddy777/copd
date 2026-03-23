package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.simats.cdss.models.AIRiskResponse;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleReassessmentActivity extends AppCompatActivity {

    private int patientId = -1;
    private int selectedMinutes = -1;
    private MaterialCardView selectedCard = null;
    private Button btnConfirm;
    private MaterialCardView card30m, card1h, card2h, card4h;
    private ImageView ivCheck30, ivCheck1h, ivCheck2h, ivCheck4h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_reassessment);

        patientId = getIntent().getIntExtra("patient_id", -1);

        btnConfirm = findViewById(R.id.btn_confirm);

        card30m = findViewById(R.id.card_30_min);
        card1h = findViewById(R.id.card_1_hour);
        card2h = findViewById(R.id.card_2_hour);
        card4h = findViewById(R.id.card_4_hour);

        ivCheck30 = findViewById(R.id.iv_check_30);
        ivCheck1h = findViewById(R.id.iv_check_1h);
        ivCheck2h = findViewById(R.id.iv_check_2h);
        ivCheck4h = findViewById(R.id.iv_check_4h);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        setupSelectionListeners();

        btnConfirm.setOnClickListener(v -> saveReassessmentToBackend());

        setupBottomNav();

        // Auto-select reassessment time based on risk
        autoSelectBasedOnRisk();
    }

    private void autoSelectBasedOnRisk() {
        if (patientId == -1) return;

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getPatientAIRisk(patientId).enqueue(new Callback<AIRiskResponse>() {
            @Override
            public void onResponse(Call<AIRiskResponse> call, Response<AIRiskResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String risk = response.body().getRiskLevel();
                    if ("HIGH".equalsIgnoreCase(risk)) {
                        handleSelection(card30m, 30);
                    } else if ("MODERATE".equalsIgnoreCase(risk)) {
                        handleSelection(card1h, 60);
                    } else {
                        handleSelection(card2h, 120);
                    }
                }
            }

            @Override
            public void onFailure(Call<AIRiskResponse> call, Throwable t) {
                // Silently fail - user can still manually select
            }
        });
    }

    private void saveReassessmentToBackend() {
        if (patientId == -1 || selectedMinutes == -1) {
            Toast.makeText(this, "Please select a reassessment time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate scheduled_time based on selectedMinutes
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.MINUTE, selectedMinutes);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        String scheduledTimeStr = sdf.format(calendar.getTime());

        // Get patient details from intent extras if available
        String patientName = getIntent().getStringExtra("patient_name");
        String bedNo = getIntent().getStringExtra("bed_no");
        String wardNo = getIntent().getStringExtra("ward_no");
        String reassessmentType = getIntent().getStringExtra("reassessment_type");
        if (reassessmentType == null || reassessmentType.isEmpty()) {
            reassessmentType = "SpO2"; // Default
        }

        Map<String, Object> body = new HashMap<>();
        body.put("patient_id", patientId);
        if (patientName != null && !patientName.isEmpty()) {
            body.put("patient_name", patientName);
        }
        if (bedNo != null && !bedNo.isEmpty()) {
            body.put("bed_no", bedNo);
        }
        if (wardNo != null && !wardNo.isEmpty()) {
            body.put("ward_no", wardNo);
        }
        body.put("reassessment_type", reassessmentType);
        body.put("scheduled_time", scheduledTimeStr);
        body.put("reassessment_minutes", selectedMinutes);

        // Send the role so backend knows who scheduled it
        SessionManager session = new SessionManager(this);
        body.put("scheduled_by", session.getRole());

        android.util.Log.d("SCHEDULE_REQ", body.toString());

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.saveScheduleReassessment(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("SCHEDULE_RES", "Success: " + response.body().getMessage());
                    Toast.makeText(ScheduleReassessmentActivity.this, "Scheduled Successfully", Toast.LENGTH_SHORT).show();
                    showSuccessBottomSheet();
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Unable to read error body";
                    }
                    android.util.Log.e("SCHEDULE_RES", "Failed code: " + response.code() + " body: " + errorBody);
                    Toast.makeText(ScheduleReassessmentActivity.this, "Failed to schedule reassessment", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                android.util.Log.e("SCHEDULE_RES", "Error: " + t.getMessage());
                Toast.makeText(ScheduleReassessmentActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_schedule_success, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false);

        bottomSheetView.findViewById(R.id.btn_done).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            SessionManager session = new SessionManager(this);
            String role = session.getRole();
            Intent intent;
            if ("staff".equals(role)) {
                intent = new Intent(this, StaffDashboardActivity.class);
            } else {
                intent = new Intent(this, DoctordashboardActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        bottomSheetDialog.show();
    }

    private void setupSelectionListeners() {
        card30m.setOnClickListener(v -> handleSelection((MaterialCardView) v, 30));
        card1h.setOnClickListener(v -> handleSelection((MaterialCardView) v, 60));
        card2h.setOnClickListener(v -> handleSelection((MaterialCardView) v, 120));
        card4h.setOnClickListener(v -> handleSelection((MaterialCardView) v, 240));
    }

    private void handleSelection(MaterialCardView card, int minutes) {
        if (selectedCard != null) {
            resetCardStyle(selectedCard);
        }

        selectedCard = card;
        selectedMinutes = minutes;
        applySelectedStyle(selectedCard);
        btnConfirm.setEnabled(true);
    }

    private void applySelectedStyle(MaterialCardView card) {
        card.setStrokeWidth(4);
        card.setStrokeColor(getResources().getColor(R.color.primary_teal));

        if (card.getId() == R.id.card_30_min) ivCheck30.setVisibility(View.VISIBLE);
        else if (card.getId() == R.id.card_1_hour) ivCheck1h.setVisibility(View.VISIBLE);
        else if (card.getId() == R.id.card_2_hour) ivCheck2h.setVisibility(View.VISIBLE);
        else if (card.getId() == R.id.card_4_hour) ivCheck4h.setVisibility(View.VISIBLE);
    }

    private void resetCardStyle(MaterialCardView card) {
        card.setStrokeWidth(0);

        if (card.getId() == R.id.card_30_min) ivCheck30.setVisibility(View.GONE);
        else if (card.getId() == R.id.card_1_hour) ivCheck1h.setVisibility(View.GONE);
        else if (card.getId() == R.id.card_2_hour) ivCheck2h.setVisibility(View.GONE);
        else if (card.getId() == R.id.card_4_hour) ivCheck4h.setVisibility(View.GONE);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_patients);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                SessionManager session = new SessionManager(this);
                String role = session.getRole();

                if (itemId == R.id.nav_home) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(this, StaffDashboardActivity.class));
                    } else {
                        startActivity(new Intent(this, DoctordashboardActivity.class));
                    }
                    finish();
                    return true;
                } else if (itemId == R.id.nav_patients) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(this, StaffPatientsActivity.class));
                    } else {
                        startActivity(new Intent(this, DoctorPatientsActivity.class));
                    }
                    finish();
                    return true;
                } else if (itemId == R.id.nav_alerts) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(this, StaffAlertsActivity.class));
                    } else {
                        startActivity(new Intent(this, DoctorAlertsActivity.class));
                    }
                    finish();
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }
}