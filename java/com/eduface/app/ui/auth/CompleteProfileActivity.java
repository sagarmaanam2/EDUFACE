package com.eduface.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.eduface.app.R;
import com.eduface.app.ui.dashboard.StudentDashboardActivity;
import com.eduface.app.ui.dashboard.TeacherDashboardActivity;
import com.eduface.app.utils.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private static final String TAG = "CompleteProfileActivity";

    private EditText nameEditText, emailEditText, phoneEditText, guardianPhoneEditText;
    private RadioGroup roleRadioGroup;
    private RadioButton studentRadioButton, teacherRadioButton;
    private Button completeProfileButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Get current authenticated user
        currentUser = mAuth.getCurrentUser();

        // If user is not authenticated, redirect to login
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views
        nameEditText = findViewById(R.id.name_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        phoneEditText = findViewById(R.id.phone_edit_text);
        guardianPhoneEditText = findViewById(R.id.guardian_phone_edit_text);
        roleRadioGroup = findViewById(R.id.role_radio_group);
        studentRadioButton = findViewById(R.id.student_radio_button);
        teacherRadioButton = findViewById(R.id.teacher_radio_button);
        completeProfileButton = findViewById(R.id.complete_profile_button);
        progressBar = findViewById(R.id.progress_bar);

        // Pre-fill available information from FirebaseUser
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            nameEditText.setText(currentUser.getDisplayName());
            nameEditText.setEnabled(false); // Prevent editing if pre-filled
        }
        if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            emailEditText.setText(currentUser.getEmail());
            emailEditText.setEnabled(false); // Prevent editing if pre-filled
        }
        if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
            // Note: Phone number from FirebaseUser might not be WhatsApp number
            // You might want to keep this field editable or clarify.
            phoneEditText.setText(currentUser.getPhoneNumber());
            // phoneEditText.setEnabled(false); // Decide if editable
        }


        // Set default role
        studentRadioButton.setChecked(true);

        // Set click listener for complete profile button
        completeProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileData();
            }
        });

        // Optional: Handle back button press - maybe sign out the user
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
    }

    // Optional: Handle back button press
    // @Override
    // public boolean onSupportNavigateUp() {
    //     // Sign out user if they go back without completing profile
    //     mAuth.signOut();
    //     preferenceManager.clearUserSession();
    //     startActivity(new Intent(this, LoginActivity.class));
    //     finish();
    //     return true;
    // }


    private void saveProfileData() {
        // Get input values
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phoneNumber = phoneEditText.getText().toString().trim();
        String guardianPhoneNumber = guardianPhoneEditText.getText().toString().trim();
        String role = teacherRadioButton.isChecked() ? "teacher" : "student";

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            return;
        }

        // Email is required if not pre-filled
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }


        if (TextUtils.isEmpty(phoneNumber)) {
            phoneEditText.setError("WhatsApp phone number is required");
            return;
        }

        // Guardian phone is optional for teachers, but required for students
        if (role.equals("student") && TextUtils.isEmpty(guardianPhoneNumber)) {
            guardianPhoneEditText.setError("Guardian's phone number is required for students");
            return;
        }

        // Show progress
        showProgress(true);

        // Prepare user data for Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("phoneNumber", phoneNumber);
        if (!TextUtils.isEmpty(guardianPhoneNumber)) {
            userData.put("guardianPhoneNumber", guardianPhoneNumber);
        }
        // Add createdAt timestamp if it doesn't exist (might already exist for email/password users)
        // Or update it if needed
        // userData.put("createdAt", System.currentTimeMillis());


        // Save user data to Firestore for the current user's UID
        db.collection("users").document(currentUser.getUid())
                .set(userData, com.google.firebase.firestore.SetOptions.merge()) // Use merge to update existing fields
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile data saved to Firestore for: " + currentUser.getUid());

                            // Save user session with the completed data
                            preferenceManager.saveUserSession(currentUser.getUid(), name, email, role);

                            // Show success message
                            Toast.makeText(CompleteProfileActivity.this,
                                    "Profile updated successfully", Toast.LENGTH_SHORT).show();

                            // Navigate based on role
                            navigateBasedOnRole(role);
                        } else {
                            // Failed to save user data
                            Log.e(TAG, "Failed to save user profile data to Firestore", task.getException());
                            Toast.makeText(CompleteProfileActivity.this, "Failed to save profile: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            completeProfileButton.setEnabled(true); // Re-enable button
                        }
                    }
                });
    }

    private void navigateBasedOnRole(String role) {
        if ("teacher".equals(role)) {
            // Navigate to teacher dashboard
            startActivity(new Intent(CompleteProfileActivity.this, TeacherDashboardActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        } else {
            // Navigate to student dashboard
            startActivity(new Intent(CompleteProfileActivity.this, StudentDashboardActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finish(); // Finish CompleteProfileActivity
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        completeProfileButton.setEnabled(!show);
        nameEditText.setEnabled(!show);
        emailEditText.setEnabled(!show);
        phoneEditText.setEnabled(!show);
        guardianPhoneEditText.setEnabled(!show);
        roleRadioGroup.setEnabled(!show);
        studentRadioButton.setEnabled(!show);
        teacherRadioButton.setEnabled(!show);
    }
}
