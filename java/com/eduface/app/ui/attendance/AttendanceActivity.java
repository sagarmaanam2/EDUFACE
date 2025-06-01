package com.eduface.app.ui.attendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eduface.app.R;
import com.eduface.app.models.Attendance;
import com.eduface.app.utils.AbsenteeNotifier;
import com.eduface.app.utils.PreferenceManager;
import com.eduface.app.utils.WhatsAppHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceActivity extends AppCompatActivity {

    private RecyclerView attendanceRecyclerView;
    private TextView noRecordsTextView;
    private ProgressBar progressBar;
    private FloatingActionButton notifyFab;
    
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private boolean isTeacher;
    private String currentMeetingId; // For storing the current meeting ID if viewing specific meeting
    private List<Attendance> attendanceList = new ArrayList<>(); // Store attendance records

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        
        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);
        
        // Check user role
        isTeacher = "teacher".equals(preferenceManager.getUserRole());
        
        // Check if we're viewing a specific meeting
        if (getIntent().hasExtra("MEETING_ID")) {
            currentMeetingId = getIntent().getStringExtra("MEETING_ID");
        }
        
        // Initialize views
        attendanceRecyclerView = findViewById(R.id.attendance_recycler_view);
        noRecordsTextView = findViewById(R.id.no_records_text_view);
        progressBar = findViewById(R.id.progress_bar);
        notifyFab = findViewById(R.id.notify_fab);
        
        // Set up recycler view
        attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.attendance_history);
        }
        
        // Set up FAB visibility and click listener
        if (isTeacher && currentMeetingId != null) {
            notifyFab.setVisibility(View.VISIBLE);
            notifyFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmNotifyAbsentees();
                }
            });
        } else {
            notifyFab.setVisibility(View.GONE);
        }
        
        // Load attendance records
        loadAttendanceRecords();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show menu options for teachers
        if (isTeacher) {
            getMenuInflater().inflate(R.menu.attendance_menu, menu);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_notify) {
            confirmNotifyAbsentees();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    private void loadAttendanceRecords() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        
        // Get user ID
        String userId = preferenceManager.getUserId();
        
        if (userId == null) {
            Toast.makeText(this, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Clear existing list
        attendanceList.clear();
        
        // Query attendance records based on role and current meeting ID
        if (currentMeetingId != null) {
            // Load attendance for a specific meeting
            db.collection("attendance")
                    .whereEqualTo("meetingId", currentMeetingId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            handleAttendanceResults(task);
                        }
                    });
        } else if (isTeacher) {
            // For teachers, load all attendance records
            db.collection("attendance")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            handleAttendanceResults(task);
                        }
                    });
        } else {
            // For students, load only their own records
            db.collection("attendance")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            handleAttendanceResults(task);
                        }
                    });
        }
    }
    
    private void handleAttendanceResults(Task<QuerySnapshot> task) {
        // Hide progress
        progressBar.setVisibility(View.GONE);
        
        if (task.isSuccessful()) {
            // Process attendance records
            for (QueryDocumentSnapshot document : task.getResult()) {
                // Create Attendance object from document
                Attendance attendance = new Attendance();
                attendance.setId(document.getId());
                attendance.setMeetingId(document.getString("meetingId"));
                attendance.setUserId(document.getString("userId"));
                attendance.setStudentEmail(document.getString("studentEmail"));
                attendance.setJoinedAt(document.getDate("joinedAt"));
                attendance.setLeftAt(document.getDate("leftAt"));
                attendance.setPresent(Boolean.TRUE.equals(document.getBoolean("present")));
                attendance.setMeetingTitle(document.getString("meetingTitle"));
                attendance.setStudentName(document.getString("studentName"));
                
                attendanceList.add(attendance);
            }
            
            if (attendanceList.isEmpty()) {
                // No records found
                attendanceRecyclerView.setVisibility(View.GONE);
                noRecordsTextView.setVisibility(View.VISIBLE);
            } else {
                // Display records
                attendanceRecyclerView.setVisibility(View.VISIBLE);
                noRecordsTextView.setVisibility(View.GONE);
                
                // Set adapter for recycler view
                // AttendanceAdapter adapter = new AttendanceAdapter(attendanceList, isTeacher);
                // attendanceRecyclerView.setAdapter(adapter);
            }
        } else {
            // Query failed
            Toast.makeText(AttendanceActivity.this, getString(R.string.error_occurred), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show confirmation dialog before sending WhatsApp notifications
     */
    private void confirmNotifyAbsentees() {
        // Check if WhatsApp is installed
        if (!WhatsAppHelper.isWhatsAppInstalled(this)) {
            Toast.makeText(this, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Notify Absent Students")
                .setMessage("Send WhatsApp messages to all absent students and their guardians?")
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        notifyAbsentStudents();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Send WhatsApp notifications to absent students
     */
    private void notifyAbsentStudents() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        
        // Create AbsenteeNotifier
        AbsenteeNotifier notifier = new AbsenteeNotifier(this);
        
        // Send notifications
        notifier.notifyAbsentees(currentMeetingId, new AbsenteeNotifier.NotifierCallback() {
            @Override
            public void onSuccess(int notifiedCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Hide progress
                        progressBar.setVisibility(View.GONE);
                        
                        // Show success message
                        Toast.makeText(AttendanceActivity.this, 
                                String.format(getString(R.string.notifications_sent_count), notifiedCount), 
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            
            @Override
            public void onFailure(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Hide progress
                        progressBar.setVisibility(View.GONE);
                        
                        // Show error message
                        Toast.makeText(AttendanceActivity.this, 
                                getString(R.string.notification_failed) + ": " + error, 
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}