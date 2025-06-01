package com.eduface.app.models;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

/**
 * Model class for Attendance records
 */
public class Attendance {

    @Exclude
    private String id;

    private String meetingId;
    private String userId;
    private String studentEmail;
    private Date joinedAt;
    private Date leftAt;
    private boolean present;
    private String meetingTitle;
    private String studentName;

    // Required empty constructor for Firestore
    public Attendance() {
    }

    // Constructor with params
    public Attendance(String meetingId, String userId, String studentEmail, Date joinedAt, boolean present) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.studentEmail = studentEmail;
        this.joinedAt = joinedAt;
        this.present = present;
    }

    // Getters and setters
    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Date getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Date leftAt) {
        this.leftAt = leftAt;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public String getMeetingTitle() {
        return meetingTitle;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    /**
     * Calculate duration of attendance (in minutes)
     */
    @Exclude
    public long getDurationMinutes() {
        if (joinedAt == null) {
            return 0;
        }

        Date endTime = leftAt != null ? leftAt : new Date();
        return (endTime.getTime() - joinedAt.getTime()) / (60 * 1000);
    }

    /**
     * Get formatted duration string
     */
    @Exclude
    public String getFormattedDuration() {
        long minutes = getDurationMinutes();

        if (minutes < 60) {
            return minutes + " min";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + " hr " + remainingMinutes + " min";
        }
    }
}