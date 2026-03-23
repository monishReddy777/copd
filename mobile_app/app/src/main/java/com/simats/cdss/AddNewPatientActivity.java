package com.simats.cdss;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.simats.cdss.models.PatientRequest;
import com.simats.cdss.models.PatientResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddNewPatientActivity extends AppCompatActivity {

    private static final String TAG = "AddNewPatient";

    private EditText etFullName, etWard, etBedNumber;
    private TextView tvDob;
    private MaterialCardView cardMale, cardFemale, cardOther;
    private View btnNext;
    private String selectedSex = "";
    private MaterialCardView selectedSexCard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_patient);

        // Initialize Views
        etFullName = findViewById(R.id.et_full_name);
        tvDob = findViewById(R.id.tv_dob);
        etWard = findViewById(R.id.et_ward);
        etBedNumber = findViewById(R.id.et_bed_number);
        cardMale = findViewById(R.id.card_male);
        cardFemale = findViewById(R.id.card_female);
        cardOther = findViewById(R.id.card_other);
        btnNext = findViewById(R.id.btn_next);

        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // Date of Birth Picker
        findViewById(R.id.rl_dob).setOnClickListener(v -> showDatePicker());

        // Sex selection logic
        cardMale.setOnClickListener(v -> updateSexSelection("Male", cardMale));
        cardFemale.setOnClickListener(v -> updateSexSelection("Female", cardFemale));
        cardOther.setOnClickListener(v -> updateSexSelection("Other", cardOther));

        // Next Button — send data to API
        btnNext.setOnClickListener(v -> {
            if (validateInput()) {
                addPatientToServer();
            }
        });

        setupBottomNav();
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = (monthOfYear + 1) + "/" + dayOfMonth + "/" + year1;
                    tvDob.setText(date);
                    tvDob.setTextColor(getResources().getColor(R.color.text_dark));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void updateSexSelection(String sex, MaterialCardView card) {
        if (selectedSexCard != null) {
            selectedSexCard.setStrokeWidth(0);
        }

        selectedSex = sex;
        selectedSexCard = card;

        selectedSexCard.setStrokeWidth(4);
        selectedSexCard.setStrokeColor(getResources().getColor(R.color.primary_teal));
    }

    private boolean validateInput() {
        if (etFullName.getText().toString().trim().isEmpty()) {
            etFullName.setError("Full name is required");
            return false;
        }
        if (selectedSex.isEmpty()) {
            Toast.makeText(this, "Please select sex", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (tvDob.getText().toString().equals("mm/dd/yyyy")) {
            Toast.makeText(this, "Please select date of birth", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etWard.getText().toString().trim().isEmpty()) {
            etWard.setError("Ward is required");
            return false;
        }
        if (etBedNumber.getText().toString().trim().isEmpty()) {
            etBedNumber.setError("Bed Number is required");
            return false;
        }
        return true;
    }

    private String convertDateFormat(String displayDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("M/d/yyyy", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = inputFormat.parse(displayDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Date parse error: " + e.getMessage());
            return displayDate;
        }
    }

    private void addPatientToServer() {
        btnNext.setEnabled(false);

        String fullName = etFullName.getText().toString().trim();
        String dob = convertDateFormat(tvDob.getText().toString());
        String ward = etWard.getText().toString().trim();
        String bedNumber = etBedNumber.getText().toString().trim();

        PatientRequest request = new PatientRequest(fullName, dob, selectedSex, ward, bedNumber);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        Call<PatientResponse> call = apiService.addPatient(request);

        call.enqueue(new Callback<PatientResponse>() {
            @Override
            public void onResponse(Call<PatientResponse> call, Response<PatientResponse> response) {
                btnNext.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    PatientResponse body = response.body();
                    Toast.makeText(AddNewPatientActivity.this,
                            "Patient added successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AddNewPatientActivity.this, BaselineDetailsActivity.class);
                    intent.putExtra("patient_id", body.getPatientId());
                    intent.putExtra("patient_name", body.getName());
                    startActivity(intent);
                } else {
                    String errorMsg = "Failed to add patient";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(AddNewPatientActivity.this,
                            "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PatientResponse> call, Throwable t) {
                btnNext.setEnabled(true);
                Toast.makeText(AddNewPatientActivity.this,
                        "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
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