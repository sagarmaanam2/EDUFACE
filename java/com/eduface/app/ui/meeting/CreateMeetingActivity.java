package com.eduface.app.ui.meeting;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eduface.app.R;
import com.eduface.app.utils.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp; // Import Timestamp
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar; // Import Calendar
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class CreateMeetingActivity extends AppCompatActivity {

    private EditText meetingTitleEditText;
    private EditText teacherNameEditText;
    private EditText subjectEditText;
    private EditText classNameEditText;
    private EditText scheduledDateEditText; // Added for scheduled date input
    private EditText scheduledTimeEditText; // Added for scheduled time input

    private Button createMeetingButton, startMeetingButton, copyCodeButton;
    private TextView meetingCodeTextView;
    private ProgressBar progressBar;
    private View successLayout;

    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private String generatedMeetingCode;
    private String createdMeetingId;

    // Calendar instance to hold the selected date and time
    private Calendar scheduledDateTime = Calendar.getInstance();

    // Define APP Name for meeting code generation
    private static final String APP_NAME_PREFIX = "EduFace";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_meeting);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Initialize views
        meetingTitleEditText = findViewById(R.id.meeting_title_edit_text);
        teacherNameEditText = findViewById(R.id.teacher_name_edit_text);
        subjectEditText = findViewById(R.id.subject_edit_text);
        classNameEditText = findViewById(R.id.class_name_edit_text);
        scheduledDateEditText = findViewById(R.id.scheduled_date_edit_text); // Initialize date EditText
        scheduledTimeEditText = findViewById(R.id.scheduled_time_edit_text); // Initialize time EditText

        createMeetingButton = findViewById(R.id.create_meeting_button);
        startMeetingButton = findViewById(R.id.start_meeting_button);
        copyCodeButton = findViewById(R.id.copy_code_button);
        meetingCodeTextView = findViewById(R.id.meeting_code_text_view);
        progressBar = findViewById(R.id.progress_bar);
        successLayout = findViewById(R.id.success_layout);

        // Set click listeners for date and time pickers
        scheduledDateEditText.setOnClickListener(v -> showDatePickerDialog());
        scheduledTimeEditText.setOnClickListener(v -> showTimePickerDialog());


        // Set click listeners for buttons
        createMeetingButton.setOnClickListener(v -> createMeeting());
        startMeetingButton.setOnClickListener(v -> startMeeting());
        copyCodeButton.setOnClickListener(v -> copyMeetingCode());


        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Shows a DatePickerDialog to select the meeting date.
     */
    private void showDatePickerDialog() {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // Store the selected date in the Calendar instance
                    scheduledDateTime.set(Calendar.YEAR, year1);
                    scheduledDateTime.set(Calendar.MONTH, monthOfYear);
                    scheduledDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Format and display the selected date
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                    scheduledDateEditText.setText(dateFormat.format(scheduledDateTime.getTime()));
                }, year, month, day);

        // Optional: Set a minimum date to prevent scheduling in the past
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        datePickerDialog.show();
    }

    /**
     * Shows a TimePickerDialog to select the meeting time.
     */
    private void showTimePickerDialog() {
        // Use the current time as the default time in the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    // Store the selected time in the Calendar instance
                    scheduledDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    scheduledDateTime.set(Calendar.MINUTE, minute1);
                    scheduledDateTime.set(Calendar.SECOND, 0); // Reset seconds to 0

                    // Format and display the selected time
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                    scheduledTimeEditText.setText(timeFormat.format(scheduledDateTime.getTime()));
                }, hour, minute, false); // 'false' for 12-hour format, 'true' for 24-hour format

        timePickerDialog.show();
    }


    /**
     * Generates a unique meeting title including teacher name and subject.
     * Format: EduFace - [Teacher Name] - [Subject] -YYYYMMDD-HHMMSS-XXX
     * (where XXX is a random 3-digit number)
     */
    private String generateUniqueMeetingTitle(String teacherName, String subject) {
        // Format: EduFace - [Teacher Name] - [Subject] -YYYYMMDD-HHMMSS-XXX
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        String dateStr = dateFormat.format(new Date()); // Use current date for title uniqueness

        // Generate random 3-digit number
        Random random = new Random();
        int randomNum = random.nextInt(900) + 100; // Generates a number between 100 and 999

        StringBuilder titleBuilder = new StringBuilder("EduFace");

        if (!TextUtils.isEmpty(teacherName)) {
            titleBuilder.append(" - ").append(teacherName.trim());
        }

        if (!TextUtils.isEmpty(subject)) {
            titleBuilder.append(" - ").append(subject.trim());
        }

        titleBuilder.append(" - ").append(dateStr).append("-").append(randomNum);

        return titleBuilder.toString();
    }

    private void createMeeting() {
        // Get required inputs
        String meetingTitle = meetingTitleEditText.getText().toString().trim();
        String teacherName = teacherNameEditText.getText().toString().trim();
        String subject = subjectEditText.getText().toString().trim();
        String className = classNameEditText.getText().toString().trim(); // Get class name input
        String scheduledDate = scheduledDateEditText.getText().toString().trim(); // Get scheduled date
        String scheduledTime = scheduledTimeEditText.getText().toString().trim(); // Get scheduled time


        // Basic validation
        if (TextUtils.isEmpty(teacherName)) {
            teacherNameEditText.setError("Teacher name is required");
            return;
        }
        if (TextUtils.isEmpty(subject)) {
            subjectEditText.setError("Subject is required");
            return;
        }
        if (TextUtils.isEmpty(className)) {
            classNameEditText.setError("Class name is required for meeting code");
            return;
        }
        if (TextUtils.isEmpty(scheduledDate)) {
            scheduledDateEditText.setError("Scheduled date is required");
            Toast.makeText(this, "Please select a scheduled date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(scheduledTime)) {
            scheduledTimeEditText.setError("Scheduled time is required");
            Toast.makeText(this, "Please select a scheduled time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the scheduled time is in the past
        if (scheduledDateTime.getTimeInMillis() < System.currentTimeMillis()) {
            Toast.makeText(this, "Scheduled time cannot be in the past", Toast.LENGTH_SHORT).show();
            scheduledDateEditText.setError("Time is in the past"); // Or set error on time field
            scheduledTimeEditText.setError("Time is in the past");
            return;
        }


        // If title is empty, generate a unique one using teacher name and subject
        if (TextUtils.isEmpty(meetingTitle)) {
            meetingTitle = generateUniqueMeetingTitle(teacherName, subject);
            meetingTitleEditText.setText(meetingTitle); // Set the generated title back to the EditText
        }


        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        createMeetingButton.setEnabled(false);

        // Generate a unique meeting code based on the new format
        generateMeetingCode(className); // Pass class name to code generation

        // Get user data (assuming userId is the teacher's ID)
        String userId = preferenceManager.getUserId();

        if (userId == null) {
            Toast.makeText(this, getString(R.string.error_occurred) + ": User ID not found", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            createMeetingButton.setEnabled(true);
            return;
        }

        // Create meeting data
        Map<String, Object> meetingData = new HashMap<>();
        meetingData.put("meetingCode", generatedMeetingCode);
        meetingData.put("title", meetingTitle);
        meetingData.put("createdBy", teacherName); // Use entered teacher name
        meetingData.put("subject", subject); // Add subject to meeting data
        meetingData.put("teacherId", userId); // Use the logged-in user's ID as teacherId
        meetingData.put("active", true);
        meetingData.put("createdAt", new Date()); // Timestamp of creation
        // Add the scheduled time as a Firestore Timestamp
        meetingData.put("scheduledTime", new Timestamp(scheduledDateTime.getTime()));


        // Save meeting to Firestore
        db.collection("meetings")
                .add(meetingData)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful() && task.getResult() != null) {
                            // Get the created meeting ID
                            createdMeetingId = task.getResult().getId();

                            // Show success UI
                            showMeetingCreatedUI();

                            // Show success message
                            Toast.makeText(CreateMeetingActivity.this, getString(R.string.meeting_created),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Log the error
                            Log.e("CreateMeeting", "Error creating meeting: ", task.getException());

                            // Show error message
                            Toast.makeText(CreateMeetingActivity.this, getString(R.string.error_occurred) + ": " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            createMeetingButton.setEnabled(true);
                        }
                    }
                });
    }

    /**
     * Generates a unique meeting code in the format APPName-ClassName-MixedAlphaNumeric.
     * The MixedAlphaNumeric part is a random 6-character alphanumeric string.
     */
    private void generateMeetingCode(String className) {
        // Generate a random 6-character alphanumeric string
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPartBuilder = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            randomPartBuilder.append(chars.charAt(random.nextInt(chars.length())));
        }

        // Construct the meeting code
        // Sanitize class name for use in code (remove spaces, special characters)
        String sanitizedClassName = className.replaceAll("[^a-zA-Z0-9]", "");

        generatedMeetingCode = APP_NAME_PREFIX + "-" + sanitizedClassName + "-" + randomPartBuilder.toString();
    }

    private void showMeetingCreatedUI() {
        // Hide create UI inputs and button
        meetingTitleEditText.setVisibility(View.GONE);
        teacherNameEditText.setVisibility(View.GONE);
        subjectEditText.setVisibility(View.GONE);
        classNameEditText.setVisibility(View.GONE);
        scheduledDateEditText.setVisibility(View.GONE); // Hide date EditText
        scheduledTimeEditText.setVisibility(View.GONE); // Hide time EditText
        createMeetingButton.setVisibility(View.GONE);

        // Show success UI
        successLayout.setVisibility(View.VISIBLE);
        meetingCodeTextView.setText(generatedMeetingCode);
    }

    private void copyMeetingCode() {
        // Copy meeting code to clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Meeting Code", generatedMeetingCode);
        clipboard.setPrimaryClip(clip);

        // Show message
        Toast.makeText(this, "Meeting code copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void startMeeting() {
        if (createdMeetingId != null && generatedMeetingCode != null) {
            // Navigate to meeting room
            Intent intent = new Intent(this, MeetingActivity.class);
            intent.putExtra("MEETING_ID", createdMeetingId);
            intent.putExtra("MEETING_CODE", generatedMeetingCode);
            // Pass the potentially generated title to the MeetingActivity
            intent.putExtra("MEETING_TITLE", meetingTitleEditText.getText().toString().trim());
            // Optionally pass the scheduled time if needed in MeetingActivity
            // intent.putExtra("SCHEDULED_TIME", scheduledDateTime.getTimeInMillis());
            startActivity(intent);

            finish();
        } else {
            Toast.makeText(this, getString(R.string.error_occurred) + ": Meeting not created yet.", Toast.LENGTH_SHORT).show();
        }
    }
}
