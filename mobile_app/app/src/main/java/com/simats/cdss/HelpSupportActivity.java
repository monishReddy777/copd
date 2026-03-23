package com.simats.cdss;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class HelpSupportActivity extends AppCompatActivity {

    private MaterialCardView cardEmail;

    // Support email address (To field)
    private static final String SUPPORT_EMAIL = "sandhiyassenthil1408@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        initializeViews();
        setupEmailSupport();
        setupBottomNavigation();
    }

    private void initializeViews() {
        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());
        cardEmail = findViewById(R.id.cardEmail);
    }

    private void setupEmailSupport() {
        cardEmail.setOnClickListener(v -> {
            try {
                // Get the logged-in user's email from SessionManager
                SessionManager sessionManager = new SessionManager(this);
                String userEmail = sessionManager.getEmail();

                // Build the mailto URI with the support email in the To field
                String mailtoUri = "mailto:" + SUPPORT_EMAIL;

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse(mailtoUri));

                // Set the subject
                emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                        "Clinical Support Request - CDSS");

                // Set the body template
                String role = sessionManager.getRole();
                String name = sessionManager.getName();
                String bodyTemplate;
                if ("staff".equalsIgnoreCase(role)) {
                    bodyTemplate = "Staff Name: " + (name != null ? name : "") + "\n" +
                            "Hospital/Institution:\n" +
                            "Patient ID (if applicable):\n\n" +
                            "Describe the issue:\n";
                } else {
                    bodyTemplate = "Doctor Name: " + (name != null ? name : "") + "\n" +
                            "Hospital/Institution:\n" +
                            "Patient ID (if applicable):\n\n" +
                            "Describe the issue:\n";
                }
                emailIntent.putExtra(Intent.EXTRA_TEXT, bodyTemplate);

                // Set the From address as the logged-in user's email
                // Note: Android's email client will use the account selected by the user,
                // but we can set EXTRA_EMAIL array for the "To" field and use the from account
                if (userEmail != null && !userEmail.isEmpty()) {
                    emailIntent.putExtra("from", userEmail);
                }

                startActivity(Intent.createChooser(emailIntent,
                        "Contact Clinical Support"));

            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No email app found. Please install an email app.",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open email: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void setupBottomNavigation() {

        BottomNavigationView bottomNav =
                findViewById(R.id.bottom_navigation);

        bottomNav.setSelectedItemId(R.id.nav_settings);

        bottomNav.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this,
                        DoctordashboardActivity.class));
                finish();
                return true;

            } else if (itemId == R.id.nav_patients) {
                startActivity(new Intent(this,
                        PatientListActivity.class));
                finish();
                return true;

            } else if (itemId == R.id.nav_alerts) {
                startActivity(new Intent(this,
                        DoctorAlertsActivity.class));
                finish();
                return true;

            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this,
                        SettingsActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }
}