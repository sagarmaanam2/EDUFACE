package com.eduface.app.models;

import com.google.firebase.Timestamp; // Import Timestamp
import com.google.firebase.firestore.Exclude; // Keep Exclude if you need it for 'id'

import java.util.Date; // Import Date

/**
 * Model class for Meeting
 */
public class Meeting {

    // @Exclude is used here because 'id' is the document ID and is set separately
    @Exclude
    private String id;

    private String meetingCode;
    private String title;
    private String createdBy; // Teacher's name who created the meeting
    private String subject;
    private String teacherId; // ID of the teacher who created the meeting
    private boolean active;
    private Date createdAt; // Timestamp of when the meeting document was created
    private Date endedAt;
    private Timestamp scheduledTime; // Added: Timestamp for the scheduled start time

    // Added: Flag to indicate if the current user is the creator of this meeting (for UI logic)
    @Exclude // Exclude this field from Firestore mapping as it's only for client-side UI state
    private boolean currentUserIsCreator;


    // Required empty constructor for Firestore
    public Meeting() {
        // Default constructor required for calls to DataSnapshot.getValue(Meeting.class)
    }

    // Constructor with fields (optional, but can be useful)
    // Update this constructor if needed based on your CreateMeetingActivity
    public Meeting(String id, String meetingCode, String title, String createdBy, String subject, String teacherId, boolean active, Date createdAt, Timestamp scheduledTime) {
        this.id = id;
        this.meetingCode = meetingCode;
        this.title = title;
        this.createdBy = createdBy;
        this.subject = subject;
        this.teacherId = teacherId;
        this.active = active;
        this.createdAt = createdAt;
        this.scheduledTime = scheduledTime;
        // currentUserIsCreator is not part of the data from Firestore, so don't initialize here
    }

    // --- Getters and Setters ---
    // Firestore uses getters and setters (or public fields) for mapping

    @Exclude // Exclude the ID field from Firestore mapping
    public String getId() {
        return id;
    }

    @Exclude // Exclude the ID field from Firestore mapping
    public void setId(String id) {
        this.id = id;
    }

    public String getMeetingCode() {
        return meetingCode;
    }

    public void setMeetingCode(String meetingCode) {
        this.meetingCode = meetingCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // Added getter and setter for subject
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }

    // Added getter and setter for scheduledTime
    public Timestamp getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Timestamp scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    // --- Getter and Setter for currentUserIsCreator flag ---
    @Exclude // Exclude this field from Firestore mapping
    public boolean isCurrentUserIsCreator() {
        return currentUserIsCreator;
    }

    @Exclude // Exclude this field from Firestore mapping
    public void setCurrentUserIsCreator(boolean currentUserIsCreator) {
        this.currentUserIsCreator = currentUserIsCreator;
    }

    // Optional: Override toString() for logging/debugging
    @Override
    public String toString() {
        return "Meeting{" +
                "id='" + id + '\'' +
                ", meetingCode='" + meetingCode + '\'' +
                ", title='" + title + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", subject='" + subject + '\'' +
                ", teacherId='" + teacherId + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", endedAt=" + endedAt +
                ", scheduledTime=" + scheduledTime +
                ", currentUserIsCreator=" + currentUserIsCreator + // Include in toString
                '}';
    }
}
