package com.simats.cdss;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        try {
            setContentView(R.layout.activity_settings);
            Log.d(TAG, "setContentView done");
        } catch (Throwable t) {
            Log.e(TAG, "setContentView FAILED: " + t.getMessage(), t);
            Toast.makeText(this, "Layout Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            setupProfileData();
            Log.d(TAG, "setupProfileData done");
        } catch (Throwable t) {
            Log.e(TAG, "setupProfileData FAILED: " + t.getMessage(), t);
        }

        try {
            setupClickListeners();
            Log.d(TAG, "setupClickListeners done");
        } catch (Throwable t) {
            Log.e(TAG, "setupClickListeners FAILED: " + t.getMessage(), t);
        }

        try {
            setupBottomNav();
            Log.d(TAG, "setupBottomNav done");
        } catch (Throwable t) {
            Log.e(TAG, "setupBottomNav FAILED: " + t.getMessage(), t);
        }

        Log.d(TAG, "onCreate completed successfully");
    }

    private void setupClickListeners() {
        // Navigation to Profile Information
        View cardProfile = findViewById(R.id.card_profile);
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, ProfileActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Profile nav error", e);
                    Toast.makeText(this, "Cannot open Profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Navigation to Clinical Guidelines
        View cardGuidelines = findViewById(R.id.card_guidelines);
        if (cardGuidelines != null) {
            cardGuidelines.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, ClinicalGuidelinesActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Guidelines nav error", e);
                    Toast.makeText(this, "Cannot open Guidelines", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Navigation to Help & Support
        View cardHelp = findViewById(R.id.card_help);
        if (cardHelp != null) {
            cardHelp.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, HelpSupportActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Help nav error", e);
                    Toast.makeText(this, "Cannot open Help", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            setupProfileData();
        } catch (Throwable t) {
            Log.e(TAG, "onResume setupProfileData error", t);
        }
    }

    private void setupProfileData() {
        SessionManager session = new SessionManager(this);
        String role = session.getRole();
        String name = session.getName();
        
        TextView tvInitials = findViewById(R.id.tv_initials);
        TextView tvName = findViewById(R.id.tv_name);
        TextView tvSpecialty = findViewById(R.id.tv_specialty);
        TextView tvProfileSubtitle = findViewById(R.id.tv_profile_subtitle);
        
        if (name != null && !name.trim().isEmpty()) {
            if ("doctor".equalsIgnoreCase(role)) {
                if (tvName != null) tvName.setText("Dr. " + name);
                if (tvProfileSubtitle != null) tvProfileSubtitle.setText("Dr. " + name);
                if (tvSpecialty != null) tvSpecialty.setText("Pulmonologist");
            } else {
                if (tvName != null) tvName.setText(name);
                if (tvProfileSubtitle != null) tvProfileSubtitle.setText(name);
                if (tvSpecialty != null) tvSpecialty.setText("Clinical Staff");
            }
            
            // Set initials
            String[] parts = name.trim().split("\\s+");
            String initials = "";
            if (parts.length > 0 && parts[0].length() > 0) {
                initials += parts[0].substring(0, 1).toUpperCase();
                if (parts.length > 1 && parts[1].length() > 0) {
                    initials += parts[1].substring(0, 1).toUpperCase();
                }
            }
            if (tvInitials != null) tvInitials.setText(initials);
        } else {
            // Fallback
            if ("doctor".equalsIgnoreCase(role)) {
                if (tvName != null) tvName.setText("Doctor");
                if (tvProfileSubtitle != null) tvProfileSubtitle.setText("Doctor");
                if (tvSpecialty != null) tvSpecialty.setText("Pulmonologist");
                if (tvInitials != null) tvInitials.setText("DR");
            } else {
                if (tvName != null) tvName.setText("Staff");
                if (tvProfileSubtitle != null) tvProfileSubtitle.setText("Staff");
                if (tvSpecialty != null) tvSpecialty.setText("Clinical Staff");
                if (tvInitials != null) tvInitials.setText("ST");
            }
        }

        ImageView ivProfileImage = findViewById(R.id.iv_profile_image);
        if (ivProfileImage != null) {
            String imageUriStr = session.getProfileImageUri();
            if (imageUriStr != null && !imageUriStr.isEmpty()) {
                // Use BitmapFactory instead of setImageURI to avoid cache issues.
                // setImageURI caches by URI string — when the file changes but the
                // path stays the same, it shows the OLD image (stale cache).
                java.io.File file = new java.io.File(getFilesDir(), "profile_image.jpg");
                if (file.exists()) {
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bmp != null) {
                        ivProfileImage.setImageBitmap(bmp);
                        ivProfileImage.setVisibility(View.VISIBLE);
                        if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                    } else {
                        ivProfileImage.setVisibility(View.GONE);
                        if (tvInitials != null) tvInitials.setVisibility(View.VISIBLE);
                    }
                } else {
                    ivProfileImage.setVisibility(View.GONE);
                    if (tvInitials != null) tvInitials.setVisibility(View.VISIBLE);
                }
            } else {
                ivProfileImage.setImageDrawable(null);
                ivProfileImage.setVisibility(View.GONE);
                if (tvInitials != null) tvInitials.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    SessionManager session = new SessionManager(this);
                    session.logout();
                    Intent intent = new Intent(this, RoleActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null) {
            Log.w(TAG, "bottom_navigation not found");
            return;
        }
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_settings) {
                // Already on Settings, do nothing
                return true;
            }

            try {
                SessionManager session = new SessionManager(SettingsActivity.this);
                String role = session.getRole();

                if (itemId == R.id.nav_home) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(SettingsActivity.this, StaffDashboardActivity.class));
                    } else {
                        startActivity(new Intent(SettingsActivity.this, DoctordashboardActivity.class));
                    }
                    finish();
                    return true;
                } else if (itemId == R.id.nav_patients) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(SettingsActivity.this, StaffPatientsActivity.class));
                    } else {
                        startActivity(new Intent(SettingsActivity.this, DoctorPatientsActivity.class));
                    }
                    finish();
                    return true;
                } else if (itemId == R.id.nav_alerts) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(SettingsActivity.this, StaffAlertsActivity.class));
                    } else {
                        startActivity(new Intent(SettingsActivity.this, DoctorAlertsActivity.class));
                    }
                    finish();
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Bottom nav error", e);
                Toast.makeText(SettingsActivity.this, "Navigation error", Toast.LENGTH_SHORT).show();
            }
            return false;
        });
        
        // Set selected item AFTER listener is set
        bottomNav.setSelectedItemId(R.id.nav_settings);
    }
}