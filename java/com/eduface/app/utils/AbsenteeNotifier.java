package com.eduface.app.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.eduface.app.models.Attendance;
import com.eduface.app.models.Meeting;
import com.eduface.app.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for handling notifications to absent students
 */
public class AbsenteeNotifier {
    
    private static final String TAG = "AbsenteeNotifier";
    
    private final Context context;
    private final FirebaseFirestore db;
    
    /**
     * Interface for notifier callbacks
     */
    public interface NotifierCallback {
        void onSuccess(int notifiedCount);
        void onFailure(String error);
    }
    
    /**
     * Constructor
     * 
     * @param context Current context
     */
    public AbsenteeNotifier(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Send WhatsApp messages to absent students for a specific meeting
     * 
     * @param meetingId Meeting ID
     * @param callback Callback to handle results
     */
    public void notifyAbsentees(String meetingId, final NotifierCallback callback) {
        // First, get the meeting details
        db.collection("meetings")
                .document(meetingId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Meeting meeting = document.toObject(Meeting.class);
                                if (meeting != null) {
                                    meeting.setId(document.getId());
                                    // Now get the attendance records for this meeting
                                    getAttendanceRecords(meeting, callback);
                                } else {
                                    callback.onFailure("Failed to load meeting data");
                                }
                            } else {
                                callback.onFailure("Meeting not found");
                            }
                        } else {
                            callback.onFailure("Failed to load meeting: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        }
                    }
                });
    }
    
    /**
     * Get attendance records for a meeting
     */
    private void getAttendanceRecords(final Meeting meeting, final NotifierCallback callback) {
        db.collection("attendance")
                .whereEqualTo("meetingId", meeting.getId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> attendedUserIds = new ArrayList<>();
                            
                            // Extract user IDs of students who attended
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Attendance attendance = document.toObject(Attendance.class);
                                if (attendance != null && attendance.isPresent()) {
                                    attendedUserIds.add(attendance.getUserId());
                                }
                            }
                            
                            // Now get all students to find who was absent
                            findAbsentStudents(meeting, attendedUserIds, callback);
                        } else {
                            callback.onFailure("Failed to load attendance: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        }
                    }
                });
    }
    
    /**
     * Find students who were absent from the meeting
     */
    private void findAbsentStudents(final Meeting meeting, final List<String> attendedUserIds, 
                                  final NotifierCallback callback) {
        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<User> absentStudents = new ArrayList<>();
                            
                            // Find students who did not attend
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User student = document.toObject(User.class);
                                if (student != null) {
                                    student.setId(document.getId());
                                    if (!attendedUserIds.contains(student.getId())) {
                                        absentStudents.add(student);
                                    }
                                }
                            }
                            
                            // Notify absent students
                            notifyAbsentStudents(meeting, absentStudents, callback);
                        } else {
                            callback.onFailure("Failed to load students: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        }
                    }
                });
    }
    
    /**
     * Send WhatsApp messages to absent students and their guardians
     */
    private void notifyAbsentStudents(Meeting meeting, List<User> absentStudents, NotifierCallback callback) {
        int notifiedCount = 0;
        
        // Format date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.US);
        String meetingDate = dateFormat.format(meeting.getCreatedAt());
        
        for (User student : absentStudents) {
            // Create message for student
            String studentMessage = "Dear " + student.getName() + ",\n\n" +
                    "This is to inform you that you were marked absent for the following class:\n" +
                    "Class: " + meeting.getTitle() + "\n" +
                    "Date & Time: " + meetingDate + "\n\n" +
                    "Please contact your teacher for more information.\n\n" +
                    "Regards,\nEduFace Attendance System";
            
            // Send message to student
            if (student.getPhoneNumber() != null) {
                boolean sent = WhatsAppHelper.sendMessage(context, student.getPhoneNumber(), studentMessage);
                if (sent) {
                    notifiedCount++;
                    Log.d(TAG, "Sent WhatsApp message to student: " + student.getName());
                }
            }
            
            // Create message for guardian
            String guardianMessage = "Dear Parent/Guardian,\n\n" +
                    "This is to inform you that " + student.getName() + " was marked absent for the following class:\n" +
                    "Class: " + meeting.getTitle() + "\n" +
                    "Date & Time: " + meetingDate + "\n\n" +
                    "Please ensure regular attendance for better academic performance.\n\n" +
                    "Regards,\nEduFace Attendance System";
            
            // Send message to guardian if phone number exists
            if (student.getGuardianPhoneNumber() != null) {
                boolean sent = WhatsAppHelper.sendMessage(context, student.getGuardianPhoneNumber(), guardianMessage);
                if (sent) {
                    notifiedCount++;
                    Log.d(TAG, "Sent WhatsApp message to guardian of: " + student.getName());
                }
            }
        }
        
        // Callback with results
        callback.onSuccess(notifiedCount);
    }
}