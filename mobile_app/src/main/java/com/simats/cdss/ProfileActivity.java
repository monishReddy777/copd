package com.simats.cdss;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private EditText etFullName, etEmailAddress, etRole;
    private TextView tvInitials;
    private ImageView ivProfileImage;
    private SessionManager session;
    private String role;
    private Uri selectedImageUri;

    // Step 2: receive cropped result (declared first to avoid forward reference)
    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String croppedUriStr = result.getData().getStringExtra("cropped_uri");
                    if (croppedUriStr != null) {
                        selectedImageUri = Uri.parse(croppedUriStr);
                        session.saveProfileImageUri(selectedImageUri.toString());
                        loadProfileImage();
                    }
                }
            }
    );

    // Step 1: gallery picker → copy temp file → launch crop
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri originalUri = result.getData().getData();
                    if (originalUri != null) {
                        try {
                            // Copy to a temp file so the crop activity can access it
                            java.io.InputStream in = getContentResolver().openInputStream(originalUri);
                            java.io.File tempFile = new java.io.File(getFilesDir(), "temp_crop_image.jpg");
                            java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);
                            byte[] buffer = new byte[1024];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                            in.close();
                            out.flush();
                            out.close();

                            // Launch crop activity
                            Intent cropIntent = new Intent(this, ImageCropActivity.class);
                            cropIntent.putExtra("image_uri", Uri.fromFile(tempFile).toString());
                            cropLauncher.launch(cropIntent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        etFullName = findViewById(R.id.et_full_name);
        etEmailAddress = findViewById(R.id.et_email_address);
        etRole = findViewById(R.id.et_role);
        tvInitials = findViewById(R.id.tv_initials);
        ivProfileImage = findViewById(R.id.iv_profile_image);

        session = new SessionManager(this);
        loadProfileData();

        // Handle Camera click
        findViewById(R.id.btn_camera).setOnClickListener(v -> {
            String[] options = {"Edit Image", "Remove Image"};
            new AlertDialog.Builder(this)
                    .setTitle("Profile Image")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            galleryLauncher.launch(intent);
                        } else if (which == 1) {
                            selectedImageUri = null;
                            session.saveProfileImageUri(null);
                            
                            java.io.File file = new java.io.File(getFilesDir(), "profile_image.jpg");
                            if (file.exists()) file.delete();
                            
                            ivProfileImage.setVisibility(View.GONE);
                            tvInitials.setVisibility(View.VISIBLE);
                        }
                    })
                    .show();
        });

        // Handle Save Profile button click
        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfile());

        setupBottomNav();
    }

    private void loadProfileData() {
        role = session.getRole();
        String name = session.getName();
        String email = session.getEmail();

        if (name != null) {
            if ("doctor".equalsIgnoreCase(role)) {
                etFullName.setText("Dr. " + name);
            } else {
                etFullName.setText(name);
            }
            updateInitials(name);
        }

        if (email != null) {
            etEmailAddress.setText(email);
        }

        if ("doctor".equalsIgnoreCase(role)) {
            etRole.setText("Pulmonologist");
        } else {
            etRole.setText("Clinical Staff");
        }

        String imageUriStr = session.getProfileImageUri();
        if (imageUriStr != null && !imageUriStr.isEmpty()) {
            selectedImageUri = Uri.parse(imageUriStr);
            loadProfileImage();
        }
    }

    /**
     * Loads the profile image using BitmapFactory instead of setImageURI().
     * setImageURI caches by URI — when the file path stays the same but content
     * changes (after crop), it shows the OLD image. BitmapFactory always reads fresh.
     */
    private void loadProfileImage() {
        java.io.File file = new java.io.File(getFilesDir(), "profile_image.jpg");
        if (file.exists()) {
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bmp != null) {
                ivProfileImage.setImageBitmap(bmp);
                ivProfileImage.setVisibility(View.VISIBLE);
                tvInitials.setVisibility(View.GONE);
                return;
            }
        }
        // No valid image — show initials
        ivProfileImage.setVisibility(View.GONE);
        tvInitials.setVisibility(View.VISIBLE);
    }

    private void updateInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        String initials = "";
        if (parts.length > 0 && parts[0].length() > 0) {
            initials += parts[0].substring(0, 1).toUpperCase();
            if (parts.length > 1 && parts[1].length() > 0) {
                initials += parts[1].substring(0, 1).toUpperCase();
            }
        }
        tvInitials.setText(initials);
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmailAddress.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove "Dr. " if saving for doctor so DB stores plain name
        String nameToSave = fullName;
        if ("doctor".equalsIgnoreCase(role) && fullName.toLowerCase().startsWith("dr. ")) {
            nameToSave = fullName.substring(4).trim();
        }

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        Map<String, String> request = new HashMap<>();
        request.put("email", session.getEmail()); // original email to update
        request.put("role", role);
        request.put("name", nameToSave);
        request.put("new_email", email);

        String finalNameToSave = nameToSave;
        api.updateProfile(request).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    session.saveName(finalNameToSave);
                    session.saveEmail(email);
                    updateInitials(finalNameToSave);
                    Toast.makeText(ProfileActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DoctordashboardActivity.class));
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