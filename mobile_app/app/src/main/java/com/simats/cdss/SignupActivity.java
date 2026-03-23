package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.models.SignupRequest;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerRole;
    private MaterialButton btnSignup;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize standard EditTexts to match XML
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spinnerRole = findViewById(R.id.spinner_role);
        btnSignup = findViewById(R.id.btn_signup);

        // Setup Role Spinner
        String[] roles = {"Select Role", "Doctor", "Clinical Staff"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.tv_login_link).setOnClickListener(v -> finish());

        // Initialize Retrofit
        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        btnSignup.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });
    }

    private void registerUser() {

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String selectedRole = spinnerRole.getSelectedItem().toString();
        String password = etPassword.getText().toString();

        // Convert spinner role to backend format and determine call
        String roleKey;
        Call<GenericResponse> call;
        SignupRequest request;

        if (selectedRole.equals("Doctor")) {
            roleKey = "doctor";
            request = new SignupRequest(fullName, email, password, roleKey);
            call = apiService.doctorSignup(request);
        } else {
            roleKey = "staff";
            request = new SignupRequest(fullName, email, password, roleKey);
            call = apiService.staffSignup(request);
        }

        // Disable button to prevent double tap
        btnSignup.setEnabled(false);
        btnSignup.setText("Creating Account...");

        call.enqueue(new Callback<GenericResponse>() {

            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {

                btnSignup.setEnabled(true);
                btnSignup.setText("Sign Up");

                if (response.isSuccessful() && response.body() != null) {

                    String message = response.body().getMessage();

                    Toast.makeText(SignupActivity.this,
                            message != null ? message : "Registration successful! Wait for admin approval.",
                            Toast.LENGTH_LONG).show();

                    // go back to login screen
                    finish();

                } else {

                    Log.e(TAG, "Signup failed: " + response.code());

                    Toast.makeText(SignupActivity.this,
                            "Registration Failed. Please check your details.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {

                btnSignup.setEnabled(true);
                btnSignup.setText("Sign Up");

                Log.e(TAG, "Signup API failed: " + t.getMessage());

                Toast.makeText(SignupActivity.this,
                        "Network error. Please try again.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String role = spinnerRole.getSelectedItem().toString();

        if (name.isEmpty()) {
            etFullName.setError("Name is required");
            etFullName.requestFocus();
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }
        if (role.equals("Select Role")) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Strict Password Validation
        if (!isPasswordValid(password)) {
            // Error message handled inside isPasswordValid
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }
        
        return true;
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            etPassword.requestFocus();
            return false;
        }
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            etPassword.setError("At least 1 uppercase letter required");
            etPassword.requestFocus();
            return false;
        }
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            etPassword.setError("At least 1 lowercase letter required");
            etPassword.requestFocus();
            return false;
        }
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            etPassword.setError("At least 1 digit required");
            etPassword.requestFocus();
            return false;
        }
        if (!Pattern.compile("[!@#$%^&*(),.?\":{}|<>_]").matcher(password).find()) {
            etPassword.setError("At least 1 special character required");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }
}
