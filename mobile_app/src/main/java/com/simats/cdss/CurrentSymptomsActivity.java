package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CurrentSymptomsActivity extends AppCompatActivity {

    private RadioGroup rgMmrc;
    private TextView tvMmrcDesc;
    private Button btnNext;
    private CheckBox cbCough, cbSputum, cbWheezing, cbFever, cbChestTightness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_symptoms);

        rgMmrc = findViewById(R.id.rg_mmrc);
        tvMmrcDesc = findViewById(R.id.tv_mmrc_desc);
        btnNext = findViewById(R.id.btn_next);

        // Initialize Checkboxes
        cbCough = findViewById(R.id.cb_cough);
        cbSputum = findViewById(R.id.cb_sputum);
        cbWheezing = findViewById(R.id.cb_wheezing);
        cbFever = findViewById(R.id.cb_fever);
        cbChestTightness = findViewById(R.id.cb_chest_tightness);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Listener for mMRC Scale
        rgMmrc.setOnCheckedChangeListener((group, checkedId) -> {
            updateMmrcDescription(checkedId);
            updateRadioButtonStyles(checkedId);
            checkValidation();
        });

        // Listeners for Symptoms Checkboxes
        CompoundButton.OnCheckedChangeListener symptomListener = (buttonView, isChecked) -> checkValidation();
        cbCough.setOnCheckedChangeListener(symptomListener);
        cbSputum.setOnCheckedChangeListener(symptomListener);
        cbWheezing.setOnCheckedChangeListener(symptomListener);
        cbFever.setOnCheckedChangeListener(symptomListener);
        cbChestTightness.setOnCheckedChangeListener(symptomListener);

        int patientId = getIntent().getIntExtra("patient_id", -1);

        btnNext.setOnClickListener(v -> {
            int selectedMmrcId = rgMmrc.getCheckedRadioButtonId();
            if (selectedMmrcId == -1) {
                android.widget.Toast.makeText(this, "Please select an mMRC scale", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            int mmrcScore = 0;
            if (selectedMmrcId == R.id.rb_mmrc_0) mmrcScore = 0;
            else if (selectedMmrcId == R.id.rb_mmrc_1) mmrcScore = 1;
            else if (selectedMmrcId == R.id.rb_mmrc_2) mmrcScore = 2;
            else if (selectedMmrcId == R.id.rb_mmrc_3) mmrcScore = 3;
            else if (selectedMmrcId == R.id.rb_mmrc_4) mmrcScore = 4;

            boolean increasedCough = cbCough.isChecked();
            boolean increasedSputum = cbSputum.isChecked();
            boolean wheezing = cbWheezing.isChecked();
            boolean fever = cbFever.isChecked();
            boolean chestTightness = cbChestTightness.isChecked();

            btnNext.setEnabled(false);

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("patient_id", patientId);
            body.put("mmrc_score", mmrcScore);
            body.put("increased_cough", increasedCough);
            body.put("increased_sputum", increasedSputum);
            body.put("wheezing", wheezing);
            body.put("fever", fever);
            body.put("chest_tightness", chestTightness);

            com.simats.cdss.network.ApiService apiService = com.simats.cdss.network.RetrofitClient.getClient(this).create(com.simats.cdss.network.ApiService.class);
            retrofit2.Call<com.simats.cdss.models.GenericResponse> call = apiService.addCurrentSymptoms(body);

            call.enqueue(new retrofit2.Callback<com.simats.cdss.models.GenericResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.simats.cdss.models.GenericResponse> call, retrofit2.Response<com.simats.cdss.models.GenericResponse> response) {
                    btnNext.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        android.widget.Toast.makeText(CurrentSymptomsActivity.this, "Current symptoms saved successfully", android.widget.Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(CurrentSymptomsActivity.this, VitalsActivity.class);
                        intent.putExtra("patient_id", patientId);
                        startActivity(intent);
                    } else {
                        android.widget.Toast.makeText(CurrentSymptomsActivity.this, "Failed to save current symptoms", android.widget.Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.simats.cdss.models.GenericResponse> call, Throwable t) {
                    btnNext.setEnabled(true);
                    android.widget.Toast.makeText(CurrentSymptomsActivity.this, "Network error. Please try again.", android.widget.Toast.LENGTH_LONG).show();
                }
            });
        });

        setupBottomNav();
    }

    private void checkValidation() {
        boolean isScaleSelected = rgMmrc.getCheckedRadioButtonId() != -1;
        boolean isAnySymptomSelected = cbCough.isChecked() || cbSputum.isChecked() || 
                                     cbWheezing.isChecked() || cbFever.isChecked() || 
                                     cbChestTightness.isChecked();

        boolean isValid = isScaleSelected && isAnySymptomSelected;
        
        btnNext.setEnabled(isValid);
        btnNext.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void updateMmrcDescription(int checkedId) {
        if (checkedId == R.id.rb_mmrc_0) tvMmrcDesc.setText("Breathless only with strenuous exercise");
        else if (checkedId == R.id.rb_mmrc_1) tvMmrcDesc.setText("Short of breath when hurrying on the level or walking up a slight hill");
        else if (checkedId == R.id.rb_mmrc_2) tvMmrcDesc.setText("Walks slower than people of the same age on the level because of breathlessness");
        else if (checkedId == R.id.rb_mmrc_3) tvMmrcDesc.setText("Stops for breath after walking about 100 yards or after a few minutes on the level");
        else if (checkedId == R.id.rb_mmrc_4) tvMmrcDesc.setText("Too breathless to leave the house or breathless when dressing");
    }

    private void updateRadioButtonStyles(int checkedId) {
        for (int i = 0; i < rgMmrc.getChildCount(); i++) {
            if (rgMmrc.getChildAt(i) instanceof RadioButton) {
                RadioButton rb = (RadioButton) rgMmrc.getChildAt(i);
                if (rb.getId() == checkedId) {
                    rb.setTextColor(getResources().getColor(R.color.white));
                    rb.setBackground(getResources().getDrawable(R.drawable.chip_bg_teal));
                } else {
                    rb.setTextColor(getResources().getColor(R.color.text_secondary));
                    rb.setBackground(getResources().getDrawable(R.drawable.otp_box_bg));
                }
            }
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, StaffDashboardActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_patients) {
                    startActivity(new Intent(this, PatientListActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_alerts) {
                    startActivity(new Intent(this, DoctorAlertsActivity.class));
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