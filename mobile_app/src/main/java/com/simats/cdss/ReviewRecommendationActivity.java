package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.simats.cdss.models.ClinicalReviewResponse;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewRecommendationActivity extends AppCompatActivity {

    private int patientId = -1;
    private MaterialCardView cardAccept, cardOverride;
    private ImageView ivCheckAccept, ivIconOverride;
    private LinearLayout layoutOverrideReason;
    private EditText etOverrideReason;
    private Button btnConfirm;
    private TextView tvDeviceName, tvDeviceDesc;
    private boolean isOverrideSelected = false;

    // Values from API
    private String recommendedDevice = "";
    private String fio2 = "";
    private String flowRate = "";

    // Doctor's selected device from DeviceSelectionActivity
    private String doctorSelectedDevice = "";
    private String doctorSelectedFlowRange = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_recommendation);

        patientId = getIntent().getIntExtra("patient_id", -1);

        // Get doctor's device selection from previous screen
        doctorSelectedDevice = getIntent().getStringExtra("selected_device");
        doctorSelectedFlowRange = getIntent().getStringExtra("selected_flow_range");
        if (doctorSelectedDevice == null) doctorSelectedDevice = "";
        if (doctorSelectedFlowRange == null) doctorSelectedFlowRange = "";

        // Initialize Views
        cardAccept = findViewById(R.id.card_accept);
        cardOverride = findViewById(R.id.card_override);
        ivCheckAccept = findViewById(R.id.iv_check_accept);
        ivIconOverride = findViewById(R.id.iv_icon_override);
        layoutOverrideReason = findViewById(R.id.layout_override_reason);
        etOverrideReason = findViewById(R.id.et_override_reason);
        btnConfirm = findViewById(R.id.btn_confirm);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvDeviceDesc = findViewById(R.id.tv_device_desc);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Default state: Accept Selected
        selectAccept();

        cardAccept.setOnClickListener(v -> selectAccept());
        cardOverride.setOnClickListener(v -> selectOverride());

        etOverrideReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateConfirmButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnConfirm.setOnClickListener(v -> saveDecisionToBackend());

        setupBottomNav();
        fetchRecommendation();
    }

    private void fetchRecommendation() {
        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getClinicalReview(patientId).enqueue(new Callback<ClinicalReviewResponse>() {
            @Override
            public void onResponse(Call<ClinicalReviewResponse> call, Response<ClinicalReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ClinicalReviewResponse data = response.body();
                    if (data.isHasData()) {
                        recommendedDevice = data.getRecommendedDevice();
                        fio2 = data.getFio2();
                        flowRate = data.getFlowRate();

                        tvDeviceName.setText(recommendedDevice + " (" + fio2 + ")");
                        tvDeviceDesc.setText("Flow Rate: " + flowRate + " | Current SpO\u2082: " + (int) data.getSpo2() + "%");
                    } else {
                        tvDeviceName.setText("No clinical data available");
                        tvDeviceDesc.setText("");
                    }
                } else {
                    Toast.makeText(ReviewRecommendationActivity.this, "Failed to load recommendation", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ClinicalReviewResponse> call, Throwable t) {
                Toast.makeText(ReviewRecommendationActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveDecisionToBackend() {
        if (patientId == -1 || recommendedDevice.isEmpty()) {
            Toast.makeText(this, "No recommendation to confirm", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        // When overriding, send the doctor's selected device; when accepting, send AI recommendation
        String finalDevice = isOverrideSelected && !doctorSelectedDevice.isEmpty() ? doctorSelectedDevice : recommendedDevice;
        String finalFlowRate = isOverrideSelected && !doctorSelectedFlowRange.isEmpty() ? doctorSelectedFlowRange : flowRate;
        body.put("device", finalDevice);
        body.put("fio2", fio2);
        body.put("flow_rate", finalFlowRate);
        body.put("decision", isOverrideSelected ? "override" : "accepted");
        if (isOverrideSelected) {
            body.put("override_reason", etOverrideReason.getText().toString().trim());
        }

        apiService.saveClinicalReview(patientId, body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReviewRecommendationActivity.this, "Decision saved successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ReviewRecommendationActivity.this, TherapyRecommendationActivity.class);
                    intent.putExtra("patient_id", patientId);
                    // Pass the doctor's selected device forward
                    intent.putExtra("selected_device", doctorSelectedDevice);
                    intent.putExtra("selected_flow_range", doctorSelectedFlowRange);
                    intent.putExtra("is_override", isOverrideSelected);
                    startActivity(intent);
                } else {
                    Toast.makeText(ReviewRecommendationActivity.this, "Failed to save decision", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(ReviewRecommendationActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectAccept() {
        isOverrideSelected = false;
        cardAccept.setStrokeWidth(4);
        cardAccept.setStrokeColor(getResources().getColor(R.color.primary_teal));
        ivCheckAccept.setVisibility(View.VISIBLE);
        cardOverride.setStrokeWidth(0);
        ivIconOverride.setVisibility(View.GONE);
        layoutOverrideReason.setVisibility(View.GONE);
        updateConfirmButtonState();
    }

    private void selectOverride() {
        isOverrideSelected = true;
        cardAccept.setStrokeWidth(0);
        ivCheckAccept.setVisibility(View.GONE);
        cardOverride.setStrokeWidth(4);
        cardOverride.setStrokeColor(getResources().getColor(R.color.orange_warning));
        ivIconOverride.setVisibility(View.VISIBLE);
        layoutOverrideReason.setVisibility(View.VISIBLE);
        updateConfirmButtonState();
    }

    private void updateConfirmButtonState() {
        if (!isOverrideSelected) {
            btnConfirm.setEnabled(true);
            btnConfirm.setAlpha(1.0f);
        } else {
            String reason = etOverrideReason.getText().toString().trim();
            boolean hasReason = !reason.isEmpty();
            btnConfirm.setEnabled(hasReason);
            btnConfirm.setAlpha(hasReason ? 1.0f : 0.5f);
        }
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