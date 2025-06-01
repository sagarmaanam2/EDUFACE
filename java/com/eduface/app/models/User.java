package com.eduface.app.models;

import com.google.firebase.firestore.Exclude;

/**
 * Model class for User
 */
public class User {
    
    @Exclude
    private String id;
    
    private String name;
    private String email;
    private String role;  // "student" or "teacher"
    private String phoneNumber;  // Added for WhatsApp messaging
    private String guardianPhoneNumber;  // For notifying guardians of student absences
    private long createdAt;
    
    // Required empty constructor for Firestore
    public User() {
    }
    
    // Constructor with params
    public User(String name, String email, String role, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Constructor with all params
    public User(String name, String email, String role, String phoneNumber, String guardianPhoneNumber) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.guardianPhoneNumber = guardianPhoneNumber;
        this.createdAt = System.currentTimeMillis();
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getGuardianPhoneNumber() {
        return guardianPhoneNumber;
    }
    
    public void setGuardianPhoneNumber(String guardianPhoneNumber) {
        this.guardianPhoneNumber = guardianPhoneNumber;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Exclude
    public boolean isTeacher() {
        return "teacher".equals(role);
    }
    
    @Exclude
    public boolean isStudent() {
        return "student".equals(role);
    }
}