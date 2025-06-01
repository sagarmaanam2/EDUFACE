package com.eduface.app.ui.dashboard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager; // Import LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView; // Import RecyclerView


import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eduface.app.R;
import com.eduface.app.models.Meeting; // Import Meeting model
import com.eduface.app.ui.attendance.AttendanceActivity;
import com.eduface.app.ui.auth.LoginActivity;
import com.eduface.app.ui.meeting.JoinMeetingActivity;
import com.eduface.app.ui.meeting.MeetingActivity;
import com.eduface.app.utils.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore; // Import FirebaseFirestore
import com.google.firebase.firestore.Query; // Import Query
import com.google.firebase.Timestamp; // Import Timestamp

import java.util.ArrayList; // Import ArrayList
import java.util.Date; // Import Date
import java.util.List; // Import List

// Import the MeetingAdapter (assuming it's in com.eduface.app.adapters)
import com.eduface.app.adapters.MeetingAdapter;


// Implement the MeetingAdapter.OnMeetingActionListener interface to handle join clicks
public class StudentDashboardActivity extends AppCompatActivity implements MeetingAdapter.OnMeetingActionListener {

    private static final String TAG = "StudentDashboard";

    private TextView welcomeTextView;
    private RecyclerView upcomingMeetingsRecyclerView; // Renamed for clarity
    private TextView noUpcomingMeetingsTextView; // Renamed for clarity
    private CardView joinMeetingCard, viewAttendanceCard;
    private Button joinMeetingButton, viewAttendanceButton, logoutButton;

    private PreferenceManager preferenceManager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Initialize Firestore

    private MeetingAdapter upcomingMeetingsAdapter; // Adapter for upcoming meetings

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

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
        upcomingMeetingsRecyclerView = findViewById(R.id.upcoming_meetings_recycler_view); // Initialize RecyclerView
        noUpcomingMeetingsTextView = findViewById(R.id.no_upcoming_meetings_text_view); // Initialize TextView
        joinMeetingCard = findViewById(R.id.join_meeting_card);
        viewAttendanceCard = findViewById(R.id.view_attendance_card);
        joinMeetingButton = findViewById(R.id.join_meeting_button);
        viewAttendanceButton = findViewById(R.id.view_attendance_button);
        logoutButton = findViewById(R.id.logout_button);

        // Set welcome message
        String welcomeMsg = String.format(getString(R.string.welcome_message),
                preferenceManager.getUserName());
        welcomeTextView.setText(welcomeMsg);

        // Set up upcoming meetings recycler view
        upcomingMeetingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with an empty list and pass 'this' as the listener
        // Students only need the Join action, so the End button will be hidden in the adapter
        upcomingMeetingsAdapter = new MeetingAdapter(new ArrayList<>(), this, this);
        upcomingMeetingsRecyclerView.setAdapter(upcomingMeetingsAdapter);

        // Set click listeners
        joinMeetingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentDashboardActivity.this, JoinMeetingActivity.class));
            }
        });

        joinMeetingCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentDashboardActivity.this, JoinMeetingActivity.class));
            }
        });

        viewAttendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentDashboardActivity.this, AttendanceActivity.class));
            }
        });

        viewAttendanceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentDashboardActivity.this, AttendanceActivity.class));
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        // Load upcoming meetings for the student
        loadUpcomingMeetingsForStudent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh upcoming meetings when returning to dashboard
        loadUpcomingMeetingsForStudent();
    }

    /**
     * Loads active meetings that are scheduled for a future time, relevant to students.
     * For simplicity, this loads ALL active upcoming meetings.
     * You might refine this query to only show meetings the student is enrolled in.
     */
    private void loadUpcomingMeetingsForStudent() {
        // Get the current time as a Firestore Timestamp
        Timestamp now = new Timestamp(new Date());

        // Query active meetings that are scheduled in the future
        // For a more complex app, you might filter by class ID, student enrollment, etc.
        db.collection("meetings")
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
                            // For students, hide the End Meeting button in the adapter
                            meeting.setCurrentUserIsCreator(false); // Assuming you add this boolean field and setter to Meeting model
                            meetingList.add(meeting);
                        }
                    }

                    if (meetingList.isEmpty()) {
                        upcomingMeetingsRecyclerView.setVisibility(View.GONE);
                        noUpcomingMeetingsTextView.setVisibility(View.VISIBLE);
                        noUpcomingMeetingsTextView.setText(getString(R.string.no_upcoming_meetings));
                        // Clear the adapter if no meetings are found
                        if (upcomingMeetingsAdapter != null) {
                            upcomingMeetingsAdapter.updateMeetings(new ArrayList<>());
                        }
                        Log.d(TAG, "No upcoming meetings found for student.");
                    } else {
                        upcomingMeetingsRecyclerView.setVisibility(View.VISIBLE);
                        noUpcomingMeetingsTextView.setVisibility(View.GONE);

                        // Update the adapter with the new list of meetings
                        if (upcomingMeetingsAdapter != null) {
                            upcomingMeetingsAdapter.updateMeetings(meetingList);
                        } else {
                            // Should not happen if adapter is initialized in onCreate, but as a fallback
                            upcomingMeetingsAdapter = new MeetingAdapter(meetingList, this, this);
                            upcomingMeetingsRecyclerView.setAdapter(upcomingMeetingsAdapter);
                        }

                        Log.d(TAG, "Loaded " + meetingList.size() + " upcoming meetings for student.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load upcoming meetings for student", e);
                    Toast.makeText(StudentDashboardActivity.this,
                            "Failed to load upcoming meetings: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Clear the adapter on failure
                    if (upcomingMeetingsAdapter != null) {
                        upcomingMeetingsAdapter.updateMeetings(new ArrayList<>());
                    }
                    noUpcomingMeetingsTextView.setVisibility(View.VISIBLE);
                    noUpcomingMeetingsTextView.setText(getString(R.string.error_loading_meetings));
                    upcomingMeetingsRecyclerView.setVisibility(View.GONE);
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
    // Students only need to handle the Join action

    @Override
    public void onJoinMeetingClick(Meeting meeting) {
        // Logic to start MeetingActivity when Join is clicked by a student
        Log.d(TAG, "Student Join Meeting clicked for: " + meeting.getTitle());
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
        // Students do not have the option to end a meeting from this list.
        // This method is required by the interface but can be left empty or show a message.
        Log.d(TAG, "Student attempted to click End Meeting (should be hidden).");
        // Toast.makeText(this, "Only teachers can end meetings.", Toast.LENGTH_SHORT).show(); // Optional message
    }
}
