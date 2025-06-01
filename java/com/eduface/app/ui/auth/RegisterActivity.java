package com.eduface.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int RC_SIGN_UP_GOOGLE = 9002; // Request code for Google Sign-Up

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private EditText phoneEditText, guardianPhoneEditText;
    private RadioGroup roleRadioGroup;
    private RadioButton studentRadioButton, teacherRadioButton;
    private CheckBox termsCheckbox;
    private Button registerButton;
    private TextView loginTextView;
    private Button googleSignUpButton;

    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private GoogleSignInClient mGoogleSignInClient; // Google Sign-In Client
    // Phone Auth Callbacks are typically handled during the verification flow,
    // which might be in a separate dialog/fragment.
    // private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks; // Phone Auth Callbacks


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Initialize views
        nameEditText = findViewById(R.id.name_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        phoneEditText = findViewById(R.id.phone_edit_text);
        guardianPhoneEditText = findViewById(R.id.guardian_phone_edit_text);
        roleRadioGroup = findViewById(R.id.role_radio_group);
        studentRadioButton = findViewById(R.id.student_radio_button);
        teacherRadioButton = findViewById(R.id.teacher_radio_button);
        termsCheckbox = findViewById(R.id.terms_checkbox);
        registerButton = findViewById(R.id.register_button);
        loginTextView = findViewById(R.id.login_text_view);
        googleSignUpButton = findViewById(R.id.google_sign_up_button);

        progressBar = findViewById(R.id.progress_bar);

        // Set default role
        studentRadioButton.setChecked(true);

        // Configure Google Sign-In for registration
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Get Web Client ID
                .requestEmail()
                .requestProfile() // Request profile info like name
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        // Set click listeners
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUserWithEmail(); // Use email registration by default
            }
        });

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        googleSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpWithGoogle();
            }
        });
    }

    // --- Email/Password Registration ---
    private void registerUserWithEmail() {
        // Get input values
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String phoneNumber = phoneEditText.getText().toString().trim();
        String guardianPhoneNumber = guardianPhoneEditText.getText().toString().trim();
        String role = teacherRadioButton.isChecked() ? "teacher" : "student";

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError("Confirm password is required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
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

        // Check if terms are accepted
        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please accept the policy and terms", Toast.LENGTH_SHORT).show();
            return;
        }


        // Show progress
        showProgress(true);

        // Create user with Firebase Auth (Email and Password)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Get current user
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Save user data to Firestore directly as all fields are collected
                                saveUserDataToFirestore(user.getUid(), name, email, phoneNumber, guardianPhoneNumber, role);
                            } else {
                                showRegistrationFailed("User creation failed");
                            }
                        } else {
                            // Registration failed
                            showRegistrationFailed(task.getException() != null ?
                                    task.getException().getMessage() : "Registration failed");
                        }
                    }
                });
    }

    // --- Google Sign-Up ---
    private void signUpWithGoogle() {
        Log.d(TAG, "Initiating Google Sign-Up");
        showProgress(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_UP_GOOGLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...)
        if (requestCode == RC_SIGN_UP_GOOGLE) {
            Log.d(TAG, "onActivityResult: RC_SIGN_UP_GOOGLE");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign up successful, authenticating with Firebase. Account ID: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign up failed", e);
                showRegistrationFailed("Google Sign-Up failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "Authenticating with Firebase using Google ID Token");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "Firebase Google sign-up successful. User ID: " + mAuth.getCurrentUser().getUid());
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Navigate to CompleteProfileActivity to collect remaining info
                                Log.d(TAG, "Google user authenticated, navigating to CompleteProfileActivity");
                                Intent intent = new Intent(RegisterActivity.this, CompleteProfileActivity.class);
                                intent.putExtra("USER_ID", user.getUid());
                                intent.putExtra("USER_NAME", user.getDisplayName()); // Pass name from Google
                                intent.putExtra("USER_EMAIL", user.getEmail()); // Pass email from Google
                                // Phone number and role will be collected in CompleteProfileActivity
                                startActivity(intent);
                                finish(); // Finish RegisterActivity
                            } else {
                                showRegistrationFailed("Firebase Google sign-up failed (user is null)");
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "Firebase Google sign-up failed", task.getException());
                            showRegistrationFailed("Firebase Google Sign-Up failed: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // --- Save User Data to Firestore (for Email/Password Registration) ---
    private void saveUserDataToFirestore(String userId, String name, String email, String phoneNumber, String guardianPhoneNumber, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("phoneNumber", phoneNumber);
        if (!TextUtils.isEmpty(guardianPhoneNumber)) {
            userData.put("guardianPhoneNumber", guardianPhoneNumber);
        }
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showProgress(false); // Hide progress after Firestore operation
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User data saved to Firestore for: " + userId);
                            // Save user session
                            preferenceManager.saveUserSession(userId, name, email, role);

                            // Show success message
                            Toast.makeText(RegisterActivity.this,
                                    "Registration successful", Toast.LENGTH_SHORT).show();

                            // Navigate based on role
                            navigateBasedOnRole(role);
                        } else {
                            // Failed to save user data
                            Log.e(TAG, "Failed to save user data to Firestore", task.getException());
                            // Consider deleting the Firebase Auth user if Firestore save fails
                            userCleanupOnFirestoreFailure(userId);
                            showRegistrationFailed("Failed to save user data");
                        }
                    }
                });
    }


    // Helper to clean up Firebase Auth user if Firestore save fails
    private void userCleanupOnFirestoreFailure(String userId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getUid().equals(userId)) {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase Auth user deleted due to Firestore save failure: " + userId);
                        } else {
                            Log.w(TAG, "Failed to delete Firebase Auth user after Firestore save failure: " + userId, task.getException());
                        }
                    });
        }
    }


    private void navigateBasedOnRole(String role) {
        showProgress(false); // Hide progress before navigating
        if ("teacher".equals(role)) {
            // Navigate to teacher dashboard
            startActivity(new Intent(RegisterActivity.this, TeacherDashboardActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        } else {
            // Navigate to student dashboard
            startActivity(new Intent(RegisterActivity.this, StudentDashboardActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finish(); // Finish RegisterActivity
    }

    private void showRegistrationFailed(String message) {
        showProgress(false); // Hide progress on failure
        // Re-enable buttons if they were disabled
        registerButton.setEnabled(true);
        googleSignUpButton.setEnabled(true);
        loginTextView.setEnabled(true);
        nameEditText.setEnabled(true);
        emailEditText.setEnabled(true);
        passwordEditText.setEnabled(true);
        confirmPasswordEditText.setEnabled(true);
        phoneEditText.setEnabled(true);
        guardianPhoneEditText.setEnabled(true);
        roleRadioGroup.setEnabled(true);
        studentRadioButton.setEnabled(true);
        teacherRadioButton.setEnabled(true);
        termsCheckbox.setEnabled(true);


        // Log error
        Log.e(TAG, "Registration failed: " + message);

        // Show error message
        Toast.makeText(RegisterActivity.this, "Registration failed: " + message,
                Toast.LENGTH_SHORT).show();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
        googleSignUpButton.setEnabled(!show);
        loginTextView.setEnabled(!show);
        nameEditText.setEnabled(!show);
        emailEditText.setEnabled(!show);
        passwordEditText.setEnabled(!show);
        confirmPasswordEditText.setEnabled(!show);
        phoneEditText.setEnabled(!show);
        guardianPhoneEditText.setEnabled(!show);
        roleRadioGroup.setEnabled(!show);
        studentRadioButton.setEnabled(!show);
        teacherRadioButton.setEnabled(!show);
        termsCheckbox.setEnabled(!show);
    }
}
