package com.eduface.app.ui.meeting;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.eduface.app.utils.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class to start meetings with proper configuration
 */
public class MeetingStarter {
    private static final String TAG = "MeetingStarter";

    /**
     * Start a new meeting as a teacher (host)
     * @param context The current context
     * @param title Optional custom meeting title (if null, one will be generated)
     */
    public static void startNewMeeting(Context context, String title) {
        PreferenceManager preferenceManager = new PreferenceManager(context);

        // Verify user is a teacher
        if (!"teacher".equals(preferenceManager.getUserRole())) {
            Toast.makeText(context, "Only teachers can start new meetings", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a unique meeting code
        String meetingCode = generateUniqueMeetingCode();

        // Generate title if not provided
        if (title == null || title.isEmpty()) {
            title = "EduFace Meeting " + meetingCode.substring(0, 5);
        }

        // Launch meeting activity
        Intent intent = new Intent(context, MeetingActivity.class);
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("MEETING_TITLE", title);
        context.startActivity(intent);
    }

    /**
     * Join an existing meeting as a student
     * @param context The current context
     * @param meetingId The Firestore meeting ID
     * @param meetingCode The meeting code to join
     * @param meetingTitle The meeting title
     */
    public static void joinMeeting(Context context, String meetingId, String meetingCode, String meetingTitle) {
        // Validate inputs
        if (meetingCode == null || meetingCode.isEmpty()) {
            Toast.makeText(context, "Invalid meeting code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Launch meeting activity with meeting details
        Intent intent = new Intent(context, MeetingActivity.class);
        intent.putExtra("MEETING_ID", meetingId);
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("MEETING_TITLE", meetingTitle);
        context.startActivity(intent);
    }

    /**
     * Check if a meeting exists and join it
     * @param context The current context
     * @param meetingCode The meeting code to check and join
     */
    public static void checkAndJoinMeeting(Context context, String meetingCode) {
        if (meetingCode == null || meetingCode.isEmpty()) {
            Toast.makeText(context, "Please enter a valid meeting code", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("meetings")
                .whereEqualTo("code", meetingCode)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(context, "No active meeting found with this code", Toast.LENGTH_SHORT).show();
                    } else {
                        // Get the first matching meeting
                        String meetingId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String meetingTitle = queryDocumentSnapshots.getDocuments().get(0).getString("title");

                        // Join the meeting
                        joinMeeting(context, meetingId, meetingCode, meetingTitle);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error finding meeting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error checking meeting", e);
                });
    }

    /**
     * Generate a unique meeting code
     * @return A unique meeting code
     */
    private static String generateUniqueMeetingCode() {
        // Get current timestamp to add uniqueness
        long timestamp = System.currentTimeMillis();

        // Generate a UUID and get first 8 characters
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // Combine for uniqueness
        return uuid + (timestamp % 1000);
    }
}