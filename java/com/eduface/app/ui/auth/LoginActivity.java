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

import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001; // Request code for Google Sign-In

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView, forgotPasswordTextView;
    private CheckBox rememberMeCheckbox;
    private Button googleSignInButton;
    // private Button phoneLoginButton; // Conceptual Phone Login button

    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private GoogleSignInClient mGoogleSignInClient; // Google Sign-In Client
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks; // Phone Auth Callbacks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Check if user is already logged in and has a complete profile
        if (preferenceManager.isUserLoggedIn() && preferenceManager.getUserRole() != null) {
            navigateBasedOnRole(preferenceManager.getUserRole());
            return;
        }

        // Initialize views
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        registerTextView = findViewById(R.id.register_text_view);
        forgotPasswordTextView = findViewById(R.id.forgot_password_text_view);
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        // phoneLoginButton = findViewById(R.id.phone_login_button);


        progressBar = findViewById(R.id.progress_bar);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Get Web Client ID from google-services.json
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up Phone Auth Callbacks (Conceptual)
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull com.google.firebase.FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                showLoginFailed("Phone verification failed: " + e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                // TODO: Prompt user to enter verification code (e.g., show a dialog or new fragment)
                Toast.makeText(LoginActivity.this, "Verification code sent to phone number.", Toast.LENGTH_LONG).show();
                showProgress(false); // Hide progress after code sent
            }
        };


        // Set click listeners
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUserWithEmail(); // Use email login by default
            }
        });

        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPasswordResetEmail(); // Call the new method
            }
        });

        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        // Conceptual Phone Login Button Listener
        // phoneLoginButton.setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View v) {
        //         // TODO: Prompt user for phone number and initiate phone verification
        //         // Example: show a dialog with an EditText for phone number input
        //         showPhoneInputDialog();
        //     }
        // });
    }

    // --- Email/Password Login ---
    private void loginUserWithEmail() {
        // Get input values
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        // Show progress
        showProgress(true);

        // Sign in with Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Get current user
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check if user data exists and is complete
                                checkUserProfileCompletion(user);
                            } else {
                                showLoginFailed("User authentication failed");
                            }
                        } else {
                            // Login failed
                            showLoginFailed(task.getException() != null ?
                                    task.getException().getMessage() : "Authentication failed");
                        }
                    }
                });
    }

    // --- Forgot Password Implementation ---
    private void sendPasswordResetEmail() {
        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required to reset password");
            Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent to " + email);
                            Toast.makeText(LoginActivity.this, "Password reset email sent to " + email, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Failed to send password reset email", task.getException());
                            Toast.makeText(LoginActivity.this, "Failed to send password reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    // --- Google Sign-In ---
    private void signInWithGoogle() {
        Log.d(TAG, "Initiating Google Sign-In");
        showProgress(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...)
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "onActivityResult: RC_SIGN_IN");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign in successful, authenticating with Firebase. Account ID: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                showLoginFailed("Google Sign-In failed: " + e.getMessage());
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
                            Log.d(TAG, "Firebase Google sign-in successful. User ID: " + mAuth.getCurrentUser().getUid());
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check if user profile is complete
                                checkUserProfileCompletion(user);
                            } else {
                                showLoginFailed("Firebase Google sign-in failed (user is null)");
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "Firebase Google sign-in failed", task.getException());
                            showLoginFailed("Firebase Google Sign-In failed: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // --- Phone Authentication (Conceptual) ---
    // This is a simplified start. Full implementation requires more UI/logic.
    private void startPhoneNumberVerification(String phoneNumber) {
        showProgress(true);
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "Phone authentication successful");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check if user profile is complete
                                checkUserProfileCompletion(user);
                            } else {
                                showLoginFailed("Phone authentication failed (user is null)");
                            }
                        } else {
                            // Sign in failed
                            Log.w(TAG, "Phone authentication failed", task.getException());
                            showLoginFailed("Phone authentication failed: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // --- Check and Navigate to Complete Profile ---
    private void checkUserProfileCompletion(FirebaseUser user) {
        Log.d(TAG, "Checking user profile completion for user: " + user.getUid());
        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        showProgress(false); // Hide progress after checking user details
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // User data exists, check if required fields are present
                                String name = document.getString("name");
                                String email = document.getString("email");
                                String role = document.getString("role");
                                String phoneNumber = document.getString("phoneNumber");
                                String guardianPhoneNumber = document.getString("guardianPhoneNumber"); // Check for guardian number

                                // Check if any required field is missing
                                // Note: Email might be null for Phone Auth users, but we collect it in CompleteProfileActivity
                                // Role is also collected in CompleteProfileActivity if missing.
                                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(role) || TextUtils.isEmpty(phoneNumber) || (role != null && role.equals("student") && TextUtils.isEmpty(guardianPhoneNumber))) {
                                    Log.d(TAG, "User profile incomplete, navigating to CompleteProfileActivity");
                                    // Navigate to CompleteProfileActivity
                                    Intent intent = new Intent(LoginActivity.this, CompleteProfileActivity.class);
                                    // Pass existing data to pre-fill
                                    intent.putExtra("USER_ID", user.getUid());
                                    intent.putExtra("USER_NAME", name);
                                    intent.putExtra("USER_EMAIL", email);
                                    intent.putExtra("USER_PHONE", phoneNumber);
                                    intent.putExtra("USER_GUARDIAN_PHONE", guardianPhoneNumber);
                                    intent.putExtra("USER_ROLE", role); // Pass existing role if any
                                    startActivity(intent);
                                    finish(); // Finish LoginActivity
                                } else {
                                    // Profile is complete, save session and navigate to dashboard
                                    Log.d(TAG, "User profile complete, navigating to dashboard");
                                    preferenceManager.saveUserSession(user.getUid(), name, email, role);
                                    navigateBasedOnRole(role);
                                }

                            } else {
                                // User document does NOT exist (brand new user from Google/Phone)
                                Log.d(TAG, "New user, navigating to CompleteProfileActivity");
                                // Navigate to CompleteProfileActivity to create the profile
                                Intent intent = new Intent(LoginActivity.this, CompleteProfileActivity.class);
                                intent.putExtra("USER_ID", user.getUid());
                                intent.putExtra("USER_NAME", user.getDisplayName()); // Pass name from Google/FirebaseUser
                                intent.putExtra("USER_EMAIL", user.getEmail()); // Pass email from Google/FirebaseUser
                                intent.putExtra("USER_PHONE", user.getPhoneNumber()); // Pass phone from FirebaseUser (if phone auth)
                                // Guardian phone and role will be collected in CompleteProfileActivity
                                startActivity(intent);
                                finish(); // Finish LoginActivity
                            }
                        } else {
                            Log.e(TAG, "Failed to check user data", task.getException());
                            // Sign out the user if fetching data fails
                            mAuth.signOut();
                            showLoginFailed("Failed to check user data.");
                        }
                    }
                });
    }


    private void navigateBasedOnRole(String role) {
        showProgress(false); // Hide progress before navigating
        if ("teacher".equals(role)) {
            // Navigate to teacher dashboard
            startActivity(new Intent(LoginActivity.this, TeacherDashboardActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        } else {
            // Navigate to student dashboard
            startActivity(new Intent(LoginActivity.this, StudentDashboardActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finish(); // Finish LoginActivity
    }

    private void showLoginFailed(String message) {
        showProgress(false); // Hide progress on failure
        // Re-enable buttons if they were disabled
        loginButton.setEnabled(true);
        googleSignInButton.setEnabled(true);
        // if (phoneLoginButton != null) phoneLoginButton.setEnabled(true);
        registerTextView.setEnabled(true);
        forgotPasswordTextView.setEnabled(true);
        rememberMeCheckbox.setEnabled(true);
        emailEditText.setEnabled(true);
        passwordEditText.setEnabled(true);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        googleSignInButton.setEnabled(!show);
        // if (phoneLoginButton != null) phoneLoginButton.setEnabled(!show);
        registerTextView.setEnabled(!show);
        forgotPasswordTextView.setEnabled(!show);
        rememberMeCheckbox.setEnabled(!show);
        emailEditText.setEnabled(!show);
        passwordEditText.setEnabled(!show);
    }

    // TODO: Implement showPhoneInputDialog() method to get phone number from user
    // and call startPhoneNumberVerification(phoneNumber)
}
