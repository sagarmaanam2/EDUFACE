package com.eduface.app.ui.meeting;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector; // Import CameraSelector
import androidx.camera.core.ImageAnalysis; // Import ImageAnalysis
import androidx.camera.core.Preview; // Import Preview
import androidx.camera.lifecycle.ProcessCameraProvider; // Import ProcessCameraProvider
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner; // Import LifecycleOwner
import androidx.camera.core.ImageAnalysis;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eduface.app.R;
import com.eduface.app.ui.dashboard.StudentDashboardActivity;
import com.eduface.app.ui.dashboard.TeacherDashboardActivity;
import com.eduface.app.utils.FaceDetectionHelper;
import com.eduface.app.utils.PreferenceManager; // Make sure this import is correct
import com.google.common.util.concurrent.ListenableFuture; // Import ListenableFuture
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.face.Face;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException; // Import ExecutionException
import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Executors; // Import Executors


public class MeetingActivity extends AppCompatActivity implements FaceDetectionHelper.FaceDetectionListener {

    private static final String TAG = "MeetingActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private TextView meetingTitleTextView, meetingCodeTextView, faceStatusTextView;
    private Button markAttendanceButton, leaveMeetingButton;
    private ProgressBar progressBar;
    private PreviewView previewView; // The view to show camera preview

    private String meetingId, meetingCode, meetingTitle;
    private boolean isTeacher;
    private boolean attendanceMarked = false;

    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private FaceDetectionHelper faceDetectionHelper;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture; // CameraX provider
    private ExecutorService cameraExecutor; // Executor for camera tasks


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Get data from intent
        Intent intent = getIntent();
        meetingId = intent.getStringExtra("MEETING_ID");
        meetingCode = intent.getStringExtra("MEETING_CODE");
        meetingTitle = intent.getStringExtra("MEETING_TITLE");

        // Check if user is teacher
        isTeacher = "teacher".equals(preferenceManager.getUserRole());

        // Initialize views
        meetingTitleTextView = findViewById(R.id.meeting_title_text_view);
        meetingCodeTextView = findViewById(R.id.meeting_code_text_view);
        faceStatusTextView = findViewById(R.id.face_status_text_view);
        markAttendanceButton = findViewById(R.id.mark_attendance_button);
        leaveMeetingButton = findViewById(R.id.leave_meeting_button);
        progressBar = findViewById(R.id.progress_bar);
        previewView = findViewById(R.id.preview_view); // Initialize PreviewView

        // Set meeting info
        if (meetingTitle != null) {
            meetingTitleTextView.setText(meetingTitle);
        }
        if (meetingCode != null) {
            meetingCodeTextView.setText(getString(R.string.meeting_code) + ": " + meetingCode);
        }

        // Set visibility based on role
        if (isTeacher) {
            markAttendanceButton.setVisibility(View.GONE);
        }

        // Set click listeners
        markAttendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Request camera permission before starting face detection
                checkCameraPermission();
            }
        });

        leaveMeetingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leaveMeeting();
            }
        });

        // Initialize CameraX Executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Start Jitsi meeting (This will open a separate window)
        startJitsiMeeting();

        // Note: Face detection and camera preview will run in the background
        // in this activity while the JitsiMeetActivity is in the foreground.
        // The PreviewView might not be visible to the user if Jitsi is full screen.
        // If you want the preview visible *within* this activity's layout,
        // you would need to integrate Jitsi differently (e.g., using JitsiMeetView).
    }

    private void startJitsiMeeting() {
        try {
            // Configure Jitsi meeting options
            JitsiMeetConferenceOptions.Builder optionsBuilder = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(meetingCode)
                    // You might want to keep video muted initially for students
                    // if attendance is marked separately via face detection
                    .setVideoMuted(!isTeacher) // Mute student video by default
                    .setAudioMuted(false)
                    .setAudioOnly(false)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setFeatureFlag("chat.enabled", true)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("meeting-name.enabled", false)
                    .setFeatureFlag("recording.enabled", isTeacher);

            // Set user info
            JitsiMeetUserInfo userInfo = new JitsiMeetUserInfo();
            userInfo.setDisplayName(preferenceManager.getUserName());
            userInfo.setEmail(preferenceManager.getUserEmail());
            optionsBuilder.setUserInfo(userInfo);

            // Build and launch the conference
            JitsiMeetConferenceOptions options = optionsBuilder.build();
            JitsiMeetActivity.launch(this, options);

        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL: " + e.getMessage());
            Toast.makeText(this, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed with camera setup and face detection
            startCameraAndFaceDetection();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed
                startCameraAndFaceDetection();
            } else {
                // Permission denied
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
                // Handle UI state if permission is denied (e.g., disable attendance button)
                markAttendanceButton.setEnabled(false);
                faceStatusTextView.setText(getString(R.string.camera_permission_needed)); // Add this string resource
                faceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.error));
            }
        }
    }

    // --- CameraX Setup and Face Detection Integration ---
    private void startCameraAndFaceDetection() {
        // Show preview and status indicators
        previewView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        faceStatusTextView.setVisibility(View.VISIBLE);
        faceStatusTextView.setText(getString(R.string.verifying_face));

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors
                Log.e(TAG, "Error getting camera provider", e);
                showFaceDetectionError("Error initializing camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this)); // Use main thread executor
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // Unbind any previously bound use cases
        cameraProvider.unbindAll();

        // Select a camera (front camera is usually preferred for face detection)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        // Setup Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Setup ImageAnalysis use case for face detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                 .build();

        // Initialize FaceDetectionHelper and set it as the analyzer
        // Pass the lifecycle owner and the listener
        faceDetectionHelper = new FaceDetectionHelper(this, this);
        imageAnalysis.setAnalyzer(cameraExecutor, faceDetectionHelper); // Set the analyzer

        try {
            // Bind use cases to the camera
            cameraProvider.bindToLifecycle(
                    (LifecycleOwner) this, // Use the Activity as LifecycleOwner
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
            Log.d(TAG, "Camera use cases bound successfully.");

        } catch (Exception e) {
            // Handle binding errors
            Log.e(TAG, "Error binding camera use cases", e);
            showFaceDetectionError("Error starting camera preview: " + e.getMessage());
        }
    }

    private void showFaceDetectionError(String message) {
        runOnUiThread(() -> {
            faceStatusTextView.setText("Face detection error: " + message);
            faceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.error));
            progressBar.setVisibility(View.GONE);
            // Optionally disable the mark attendance button
            markAttendanceButton.setEnabled(false);
        });
    }


    private void markAttendance() {
        // Prevent marking attendance if already marked
        if (attendanceMarked) {
            Toast.makeText(this, getString(R.string.attendance_already_marked), Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure required data is available
        String userId = preferenceManager.getUserId();
        String userEmail = preferenceManager.getUserEmail();

        if (userId == null || userEmail == null || meetingId == null || meetingTitle == null) {
            Toast.makeText(this, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Required data for attendance marking is null.");
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        faceStatusTextView.setText(getString(R.string.marking_attendance)); // Add this string resource


        // Create attendance data
        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("meetingId", meetingId);
        attendanceData.put("userId", userId);
        attendanceData.put("studentEmail", userEmail);
        attendanceData.put("joinedAt", new Date()); // Use current time as join time
        attendanceData.put("present", true);
        attendanceData.put("meetingTitle", meetingTitle); // Include meeting title
        attendanceData.put("studentName", preferenceManager.getUserName()); // Include student name


        // Save attendance to Firestore
        db.collection("attendance")
                .add(attendanceData)
                .addOnSuccessListener(documentReference -> {
                    // Hide progress
                    progressBar.setVisibility(View.GONE);
                    faceStatusTextView.setVisibility(View.GONE); // Hide status text after marking

                    // Mark as recorded
                    attendanceMarked = true;

                    // Update UI
                    markAttendanceButton.setEnabled(false);
                    markAttendanceButton.setText(getString(R.string.attendance_marked));

                    // Show success message
                    Toast.makeText(MeetingActivity.this, getString(R.string.attendance_marked),
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Attendance marked successfully for user: " + userId + " in meeting: " + meetingId);

                    // Optional: Stop camera preview and face detection after marking attendance
                    stopCameraAndFaceDetection();

                })
                .addOnFailureListener(e -> {
                    // Hide progress
                    progressBar.setVisibility(View.GONE);
                    faceStatusTextView.setText("Attendance marking failed: " + e.getMessage());
                    faceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.error));


                    // Show error message
                    Toast.makeText(MeetingActivity.this, "Error marking attendance: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error marking attendance", e);
                    // Re-enable button to try again if needed
                    markAttendanceButton.setEnabled(true);
                });
    }

    private void leaveMeeting() {
        // If teacher, ask if they want to end the meeting
        if (isTeacher) {
            new AlertDialog.Builder(this)
                    .setTitle("End Meeting")
                    .setMessage("Do you want to end this meeting for all participants?")
                    .setPositiveButton("End Meeting", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            endMeeting();
                        }
                    })
                    .setNegativeButton("Just Leave", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Increment teacher attendance count when they choose to just leave
                            preferenceManager.incrementTotalMeetingsAttended();
                            Log.i(TAG, "Teacher chose to just leave. Incrementing attendance count.");
                            exitMeeting();
                        }
                    })
                    .show();
        } else {
            // If student, just leave
            exitMeeting();
        }
    }

    private void endMeeting() {
        // Update meeting status in Firestore
        if (meetingId != null) {
            DocumentReference meetingRef = db.collection("meetings").document(meetingId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("active", false);
            updates.put("endedAt", new Date());

            meetingRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MeetingActivity.this, getString(R.string.meeting_ended),
                                Toast.LENGTH_SHORT).show();
                        // Increment teacher attendance count after successfully ending the meeting
                        if (isTeacher) {
                            preferenceManager.incrementTotalMeetingsAttended();
                            Log.i(TAG, "Teacher ended the meeting. Incrementing attendance count.");
                        }
                        exitMeeting();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MeetingActivity.this, "Error ending meeting: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error ending meeting", e);
                    });
        } else {
            Log.w(TAG, "Meeting ID is null, cannot end meeting.");
        }
    }

    private void exitMeeting() {
        // Stop camera and face detection before exiting
        stopCameraAndFaceDetection();

        // Check if student did not mark attendance
        if (!isTeacher && !attendanceMarked) {
            new AlertDialog.Builder(this)
                    .setTitle("Attendance Not Marked")
                    .setMessage("You haven't marked your attendance yet. Do you want to mark it now?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Restart camera and face detection for attendance
                            checkCameraPermission();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            navigateToDashboard();
                        }
                    })
                    .show();
        } else {
            navigateToDashboard();
        }
    }

    private void navigateToDashboard() {
        // Navigate to appropriate dashboard based on role
        if (isTeacher) {
            startActivity(new Intent(this, TeacherDashboardActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            startActivity(new Intent(this, StudentDashboardActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        finish(); // Finish the MeetingActivity
    }

    // --- FaceDetectionHelper Callbacks ---
    @Override
    public void onFaceDetected(Face face, android.graphics.Rect boundingBox, float confidence) {
        // This callback is now triggered by the actual FaceDetectionHelper
        Log.d(TAG, "Face detected! Confidence: " + confidence);
        runOnUiThread(() -> {
            faceStatusTextView.setText(getString(R.string.face_detected));
            faceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.success));
            progressBar.setVisibility(View.GONE);

            // After face detection success, mark attendance
            markAttendance();
        });
    }

    @Override
    public void onFaceDetectionFailed(Exception e) {
        // This callback is now triggered by the actual FaceDetectionHelper
        Log.e(TAG, "Face detection failed", e);
        runOnUiThread(() -> {
            faceStatusTextView.setText("Face detection failed: " + e.getMessage());
            faceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.error));
            progressBar.setVisibility(View.GONE);
            // Re-enable the mark attendance button if needed
            markAttendanceButton.setEnabled(true);
        });
    }

    @Override
    public void onNoFaceDetected() {
        // This callback is now triggered by the actual FaceDetectionHelper
        Log.d(TAG, "No face detected.");
        runOnUiThread(() -> {
            faceStatusTextView.setText(getString(R.string.face_not_detected));
            faceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.warning));
            progressBar.setVisibility(View.GONE); // Hide progress when no face is detected
            // Keep the mark attendance button enabled so the user can try again
            markAttendanceButton.setEnabled(true);
        });
    }

    // --- Lifecycle Methods ---
    @Override
    protected void onStart() {
        super.onStart();
        // Start camera and face detection when the activity becomes visible
        // This will run in the background even if JitsiMeetActivity is in foreground
        // checkCameraPermission(); // We will start this when the button is clicked instead
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop camera and face detection when the activity goes to background
        stopCameraAndFaceDetection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the camera executor
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        // Release face detection resources
        if (faceDetectionHelper != null) {
            faceDetectionHelper.shutdown();
        }
        Log.d(TAG, "MeetingActivity destroyed.");
    }

    // --- Helper to stop CameraX and Face Detection ---
    private void stopCameraAndFaceDetection() {
        ProcessCameraProvider cameraProvider = null;
        try {
            // Get the camera provider instance
            cameraProvider = cameraProviderFuture.get();
            // Unbind all use cases from the lifecycle
            cameraProvider.unbindAll();
            Log.d(TAG, "Camera use cases unbound.");
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error unbinding camera use cases", e);
        } catch (NullPointerException e) {
            // Handle case where cameraProviderFuture is null (e.g., if permission was denied early)
            Log.w(TAG, "cameraProviderFuture is null, cannot unbind.", e);
        }

        // Reset UI state related to camera preview and status
        previewView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        faceStatusTextView.setVisibility(View.GONE);
        // Keep mark attendance button state as is
    }
}
