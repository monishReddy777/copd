package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ABGEntryActivity extends AppCompatActivity {

    private EditText etPh, etPao2, etPaco2, etHco3, etFio2;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abg_entry);

        etPh = findViewById(R.id.et_ph);
        etPao2 = findViewById(R.id.et_pao2);
        etPaco2 = findViewById(R.id.et_paco2);
        etHco3 = findViewById(R.id.et_hco3);
        etFio2 = findViewById(R.id.et_fio2);
        btnSubmit = findViewById(R.id.btn_submit);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkValidation();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etPh.addTextChangedListener(watcher);
        etPao2.addTextChangedListener(watcher);
        etPaco2.addTextChangedListener(watcher);
        etHco3.addTextChangedListener(watcher);
        etFio2.addTextChangedListener(watcher);

        btnSubmit.setEnabled(false);
        btnSubmit.setAlpha(0.5f);

        int patientId = getIntent().getIntExtra("patient_id", -1);

        btnSubmit.setOnClickListener(v -> {
            try {
                double ph = Double.parseDouble(etPh.getText().toString().trim());
                double pao2 = Double.parseDouble(etPao2.getText().toString().trim());
                double paco2 = Double.parseDouble(etPaco2.getText().toString().trim());
                double hco3 = Double.parseDouble(etHco3.getText().toString().trim());
                double fio2 = Double.parseDouble(etFio2.getText().toString().trim());

                Map<String, Object> request = new HashMap<>();
                request.put("patient_id", patientId);
                request.put("ph", ph);
                request.put("pao2", pao2);
                request.put("paco2", paco2);
                request.put("hco3", hco3);
                request.put("fio2", fio2);

                btnSubmit.setEnabled(false);
                btnSubmit.setText("Submitting...");

                boolean isUpdate = getIntent().getBooleanExtra("is_update", false);
                ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
                Call<GenericResponse> call;
                if (isUpdate) {
                    call = apiService.updateStaffAbg(patientId, request);
                } else {
                    call = apiService.addAbgEntry(request);
                }

                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("SUBMIT & FINISH");
                        if (response.isSuccessful()) {
                            if (isUpdate) {
                                Toast.makeText(ABGEntryActivity.this, "Patient vitals and ABG updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ABGEntryActivity.this, "Patient details added successfully", Toast.LENGTH_SHORT).show();
                            }

                            // Check if this is a reassessment flow from alerts
                            boolean isReassessment = getIntent().getBooleanExtra("is_reassessment", false);
                            Intent intent;
                            if (isReassessment) {
                                // Continue to Reassessment Checklist
                                intent = new Intent(ABGEntryActivity.this, ReassessmentChecklistActivity.class);
                                intent.putExtra("patient_id", patientId);
                                intent.putExtra("reassessment_id", getIntent().getIntExtra("reassessment_id", -1));
                                intent.putExtra("patient_name", getIntent().getStringExtra("patient_name"));
                                intent.putExtra("bed_no", getIntent().getStringExtra("bed_no"));
                                intent.putExtra("ward_no", getIntent().getStringExtra("ward_no"));
                                intent.putExtra("reassessment_type", "scheduled");
                            } else if (isUpdate) {
                                // Redirect back to list on update
                                intent = new Intent(ABGEntryActivity.this, StaffPatientsActivity.class);
                            } else {
                                // Redirect to dashboard on add
                                intent = new Intent(ABGEntryActivity.this, StaffDashboardActivity.class);
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ABGEntryActivity.this, "Failed to save ABG data", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("SUBMIT & FINISH");
                        Toast.makeText(ABGEntryActivity.this, "Network error. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show();
            }
        });

        setupBottomNav();
    }

    private void checkValidation() {
        boolean isValid = !etPh.getText().toString().trim().isEmpty() &&
                         !etPao2.getText().toString().trim().isEmpty() &&
                         !etPaco2.getText().toString().trim().isEmpty() &&
                         !etHco3.getText().toString().trim().isEmpty() &&
                         !etFio2.getText().toString().trim().isEmpty();
        
        btnSubmit.setEnabled(isValid);
        btnSubmit.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                // Return to Staff Dashboard for any bottom navigation click
                Intent intent = new Intent(this, StaffDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            });
        }
    }
}