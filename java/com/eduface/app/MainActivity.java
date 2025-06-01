package com.eduface.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import com.eduface.app.ui.auth.LoginActivity;
import com.eduface.app.ui.dashboard.StudentDashboardActivity;
import com.eduface.app.ui.dashboard.TeacherDashboardActivity;
import com.eduface.app.utils.PreferenceManager;

/**
 * Splash Screen activity - the entry point of the application
 */
public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set full screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_main);
        
        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);
        
        // Delayed navigation
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToNextScreen();
            }
        }, SPLASH_DURATION);
    }
    
    /**
     * Navigate to the appropriate screen based on login status
     */
    private void navigateToNextScreen() {
        if (preferenceManager.isUserLoggedIn()) {
            // User is logged in, check role
            String role = preferenceManager.getUserRole();
            
            if ("teacher".equals(role)) {
                // Navigate to teacher dashboard
                startActivity(new Intent(MainActivity.this, TeacherDashboardActivity.class));
            } else {
                // Navigate to student dashboard
                startActivity(new Intent(MainActivity.this, StudentDashboardActivity.class));
            }
        } else {
            // User is not logged in, go to login screen
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
        
        // Finish this activity
        finish();
    }
}