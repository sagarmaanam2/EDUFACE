package com.eduface.app.ui.meeting;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.eduface.app.R;
import com.eduface.app.models.Meeting;
import com.eduface.app.utils.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class JoinMeetingActivity extends AppCompatActivity {

    private EditText meetingCodeEditText;
    private Button joinMeetingButton;
    private ProgressBar progressBar;
    
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_meeting);
        
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        
        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);
        
        // Initialize views
        meetingCodeEditText = findViewById(R.id.meeting_code_edit_text);
        joinMeetingButton = findViewById(R.id.join_meeting_button);
        progressBar = findViewById(R.id.progress_bar);
        
        // Set click listener
        joinMeetingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinMeeting();
            }
        });
        
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
    
    private void joinMeeting() {
        // Get meeting code
        String meetingCode = meetingCodeEditText.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(meetingCode)) {
            meetingCodeEditText.setError("Meeting code is required");
            return;
        }
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        joinMeetingButton.setEnabled(false);
        
        // Find meeting with the provided code
        db.collection("meetings")
                .whereEqualTo("meetingCode", meetingCode)
                .whereEqualTo("active", true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Get the meeting document
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            Meeting meeting = document.toObject(Meeting.class);
                            
                            if (meeting != null) {
                                meeting.setId(document.getId());
                                
                                // Log attendance (later)
                                
                                // Navigate to meeting room
                                Intent intent = new Intent(JoinMeetingActivity.this, MeetingActivity.class);
                                intent.putExtra("MEETING_ID", meeting.getId());
                                intent.putExtra("MEETING_CODE", meeting.getMeetingCode());
                                intent.putExtra("MEETING_TITLE", meeting.getTitle());
                                startActivity(intent);
                                
                                // Show success message
                                Toast.makeText(JoinMeetingActivity.this, getString(R.string.meeting_joined),
                                        Toast.LENGTH_SHORT).show();
                                
                                finish();
                            } else {
                                showError();
                            }
                        } else {
                            // Meeting not found or not active
                            Toast.makeText(JoinMeetingActivity.this, getString(R.string.meeting_not_found),
                                    Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            joinMeetingButton.setEnabled(true);
                        }
                    }
                });
    }
    
    private void showError() {
        progressBar.setVisibility(View.GONE);
        joinMeetingButton.setEnabled(true);
        Toast.makeText(this, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
    }
}