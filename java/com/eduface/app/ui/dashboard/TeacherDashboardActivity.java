package com.eduface.app.ui.dashboard;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eduface.app.R;
import com.eduface.app.models.Meeting;
import com.eduface.app.ui.attendance.AttendanceActivity;
import com.eduface.app.ui.auth.LoginActivity;
import com.eduface.app.ui.meeting.CreateMeetingActivity;
import com.eduface.app.ui.meeting.MeetingActivity;
import com.eduface.app.utils.AbsenteeNotifier;
import com.eduface.app.utils.PreferenceManager;
import com.eduface.app.utils.WhatsAppHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Import the MeetingAdapter
import com.eduface.app.adapters.MeetingAdapter;


// Implement the MeetingAdapter.OnMeetingActionListener interface
public class TeacherDashboardActivity extends AppCompatActivity implements MeetingAdapter.OnMeetingActionListener {

    private static final String TAG = "TeacherDashboard";

    private TextView welcomeTextView, noMeetingsTextView;
    private TextView totalMeetingsAttendedTextView;
    private RecyclerView meetingsRecyclerView;
    private CardView createMeetingCard, viewAttendanceCard, notifyCard;
    private Button createMeetingButton, viewAttendanceButton, notifyButton, logoutButton;

    private PreferenceManager preferenceManager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private MeetingAdapter meetingsAdapter; // Declare the adapter variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Check if user is logged in
        if (!preferenceManager.isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views
        welcomeTextView = findViewById(R.id.welcome_text_view);
        noMeetingsTextView = findViewById(R.id.no_meetings_text_view);
        meetingsRecyclerView = findViewById(R.id.meetings_recycler_view);
        createMeetingCard = findViewById(R.id.create_meeting_card);
        viewAttendanceCard = findViewById(R.id.view_attendance_card);
        notifyCard = findViewById(R.id.notify_card);
        createMeetingButton = findViewById(R.id.create_meeting_button);
        viewAttendanceButton = findViewById(R.id.view_attendance_button);
        notifyButton = findViewById(R.id.notify_button);
        logoutButton = findViewById(R.id.logout_button);
        totalMeetingsAttendedTextView = findViewById(R.id.total_meetings_attended_text_view);

        // Set welcome message
        String welcomeMsg = String.format(getString(R.string.welcome_message),
                preferenceManager.getUserName());
        welcomeTextView.setText(welcomeMsg);

        // Set up recycler view
        meetingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with an empty list and pass 'this' as the listener
        meetingsAdapter = new MeetingAdapter(new ArrayList<>(), this, this);
        meetingsRecyclerView.setAdapter(meetingsAdapter); // Set the adapter

        // Set click listeners
        createMeetingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TeacherDashboardActivity.this,  CreateMeetingActivity.class));
            }
        });

        createMeetingCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TeacherDashboardActivity.this, CreateMeetingActivity.class));
            }
        });

        viewAttendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TeacherDashboardActivity.this, AttendanceActivity.class));
            }
        });

        viewAttendanceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TeacherDashboardActivity.this, AttendanceActivity.class));
            }
        });

        notifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMeetingSelectionDialog();
            }
        });

        notifyCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMeetingSelectionDialog();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        // Load upcoming meetings
        loadUpcomingMeetings();

        // Display total meetings attended
        displayTotalMeetingsAttended();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh meetings and attendance count when returning to dashboard
        loadUpcomingMeetings(); // Refresh upcoming meetings
        displayTotalMeetingsAttended(); // Refresh attendance count
    }

    /**
     * Loads active meetings that are scheduled for a future time.
     */
    private void loadUpcomingMeetings() {
        String userId = preferenceManager.getUserId();
        if (userId == null) {
            Log.w(TAG, "User ID is null, cannot load upcoming meetings.");
            // Clear the adapter if user ID is null
            if (meetingsAdapter != null) {
                meetingsAdapter.updateMeetings(new ArrayList<>());
            }
            noMeetingsTextView.setVisibility(View.VISIBLE);
            noMeetingsTextView.setText(getString(R.string.no_upcoming_meetings));
            meetingsRecyclerView.setVisibility(View.GONE);
            return;
        }

        // Get the current time as a Firestore Timestamp
        Timestamp now = new Timestamp(new Date());

        // Query active meetings for this teacher that are scheduled in the future
        db.collection("meetings")
                .whereEqualTo("teacherId", userId)
                .whereEqualTo("active", true)
                .whereGreaterThanOrEqualTo("scheduledTime", now) // Filter for meetings in the future
                .orderBy("scheduledTime", Query.Direction.ASCENDING) // Order by scheduled time
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Meeting> meetingList = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Meeting meeting = document.toObject(Meeting.class);
                        if (meeting != null) {
                            meeting.setId(document.getId());
                            // Optional: Add a flag to the Meeting object if current user is the creator
                            // This can be used in the adapter to show/hide the end button
                            // meeting.setCurrentUserIsCreator(meeting.getTeacherId().equals(preferenceManager.getUserId()));
                            meetingList.add(meeting);
                        }
                    }

                    if (meetingList.isEmpty()) {
                        meetingsRecyclerView.setVisibility(View.GONE);
                        noMeetingsTextView.setVisibility(View.VISIBLE);
                        noMeetingsTextView.setText(getString(R.string.no_upcoming_meetings));
                        // Clear the adapter if no meetings are found
                        if (meetingsAdapter != null) {
                            meetingsAdapter.updateMeetings(new ArrayList<>());
                        }
                        Log.d(TAG, "No upcoming meetings found.");
                    } else {
                        meetingsRecyclerView.setVisibility(View.VISIBLE);
                        noMeetingsTextView.setVisibility(View.GONE);

                        // Update the adapter with the new list of meetings
                        if (meetingsAdapter != null) {
                            meetingsAdapter.updateMeetings(meetingList);
                        } else {
                            // Should not happen if adapter is initialized in onCreate, but as a fallback
                            meetingsAdapter = new MeetingAdapter(meetingList, this, this);
                            meetingsRecyclerView.setAdapter(meetingsAdapter);
                        }

                        Log.d(TAG, "Loaded " + meetingList.size() + " upcoming meetings.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load upcoming meetings", e);
                    Toast.makeText(TeacherDashboardActivity.this,
                            "Failed to load upcoming meetings: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Clear the adapter on failure
                    if (meetingsAdapter != null) {
                        meetingsAdapter.updateMeetings(new ArrayList<>());
                    }
                    noMeetingsTextView.setVisibility(View.VISIBLE);
                    noMeetingsTextView.setText(getString(R.string.error_loading_meetings)); // Assuming you add this string resource
                    meetingsRecyclerView.setVisibility(View.GONE);
                });
    }

    /**
     * Fetches the total number of meetings attended and displays it.
     * This assumes the count is stored in PreferenceManager.
     * You need to implement the logic to increment this count in MeetingActivity.
     */
    private void displayTotalMeetingsAttended() {
        int totalAttended = preferenceManager.getTotalMeetingsAttended(); // This method is in your updated PreferenceManager

        String attendedText = getString(R.string.meetings_attended_count, totalAttended);
        totalMeetingsAttendedTextView.setText(attendedText);
        Log.d(TAG, "Displayed total meetings attended: " + totalAttended);
    }


    /**
     * Shows a dialog for selecting which meeting to send notifications for
     */
    private void showMeetingSelectionDialog() {
        // Check if WhatsApp is installed
        if (!WhatsAppHelper.isWhatsAppInstalled(this)) {
            Toast.makeText(this, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = preferenceManager.getUserId();
        if (userId == null) {
            Log.w(TAG, "User ID is null, cannot show meeting selection dialog.");
            return;
        }

        // Show loading dialog
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Loading Meetings")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // Query all meetings for this teacher (both active and ended)
        // This query remains the same as you might want to notify for past meetings too
        db.collection("meetings")
                .whereEqualTo("teacherId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingDialog.dismiss();

                    List<Meeting> meetingList = new ArrayList<>();
                    List<String> meetingTitles = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Meeting meeting = document.toObject(Meeting.class);
                        if (meeting != null) {
                            meeting.setId(document.getId());
                            meetingList.add(meeting);
                            meetingTitles.add(meeting.getTitle());
                        }
                    }

                    if (meetingList.isEmpty()) {
                        Toast.makeText(TeacherDashboardActivity.this,
                                "No meetings found", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "No meetings found for notification selection.");
                        return;
                    }

                    // Show meeting selection dialog
                    new AlertDialog.Builder(TeacherDashboardActivity.this)
                            .setTitle("Select Meeting")
                            .setItems(meetingTitles.toArray(new String[0]), (dialog, which) -> {
                                Meeting selectedMeeting = meetingList.get(which);
                                sendNotificationsForMeeting(selectedMeeting);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    Log.d(TAG, "Showing meeting selection dialog with " + meetingTitles.size() + " meetings.");
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Log.e(TAG, "Failed to load meetings for notification selection", e);
                    Toast.makeText(TeacherDashboardActivity.this,
                            "Failed to load meetings: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Send notifications for a specific meeting
     */
    private void sendNotificationsForMeeting(Meeting meeting) {
        // Create AbsenteeNotifier
        AbsenteeNotifier notifier = new AbsenteeNotifier(this);

        // Show loading dialog
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Sending Notifications")
                .setMessage("Please wait while we send notifications to absent students...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // Send notifications
        notifier.notifyAbsentees(meeting.getId(), new AbsenteeNotifier.NotifierCallback() {
            @Override
            public void onSuccess(int notifiedCount) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();

                    // Show success message
                    new AlertDialog.Builder(TeacherDashboardActivity.this)
                            .setTitle("Notifications Sent")
                            .setMessage(String.format(getString(R.string.notifications_sent_count), notifiedCount))
                            .setPositiveButton("OK", null)
                            .show();
                    Log.d(TAG, "Notifications sent successfully to " + notifiedCount + " absentees for meeting: " + meeting.getId());
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();

                    // Show error message
                    new AlertDialog.Builder(TeacherDashboardActivity.this)
                            .setTitle("Error")
                            .setMessage(getString(R.string.notification_failed) + ": " + error)
                            .setPositiveButton("OK", null)
                            .show();
                    Log.e(TAG, "Failed to send notifications for meeting: " + meeting.getId() + " Error: " + error);
                });
            }
        });
    }

    private void logout() {
        // Sign out from Firebase
        mAuth.signOut();

        // Clear user session
        preferenceManager.clearUserSession();

        // Show message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "User logged out.");

        // Navigate to login screen
        startActivity(new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    // --- Implementation of MeetingAdapter.OnMeetingActionListener ---

    @Override
    public void onJoinMeetingClick(Meeting meeting) {
        // Logic to start MeetingActivity when Join is clicked
        Log.d(TAG, "Join Meeting clicked for: " + meeting.getTitle());
        if (meeting != null && meeting.getId() != null && meeting.getMeetingCode() != null) {
            Intent intent = new Intent(this, MeetingActivity.class);
            intent.putExtra("MEETING_ID", meeting.getId());
            intent.putExtra("MEETING_CODE", meeting.getMeetingCode());
            intent.putExtra("MEETING_TITLE", meeting.getTitle());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Meeting details incomplete.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEndMeetingClick(Meeting meeting) {
        // Logic to end the meeting when End is clicked (for teachers)
        // This method will be called from the adapter when the end button is clicked.
        Log.d(TAG, "End Meeting clicked for: " + meeting.getTitle());
        if (meeting != null && meeting.getId() != null) {
            // Call the end meeting logic, perhaps from a method in this Activity
            // You need to implement the actual end meeting logic here or call a method that does.
            // For example:
            // endMeetingInFirestore(meeting.getId()); // Implement this method

            // For now, show a placeholder toast
            Toast.makeText(this, "End Meeting logic to be implemented for: " + meeting.getTitle(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Cannot end meeting, ID is missing.", Toast.LENGTH_SHORT).show();
        }
    }

    // You might need a method to implement the actual Firestore update to end the meeting
    // private void endMeetingInFirestore(String meetingId) {
    //     // Implement the Firestore update logic to set 'active' to false and 'endedAt'
    //     // Similar to the endMeeting method in MeetingActivity, but called from here.
    // }
}
