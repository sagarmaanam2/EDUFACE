package com.eduface.app.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast; // Import Toast

/**
 * Helper class for WhatsApp interactions
 */
public class WhatsAppHelper {

    private static final String TAG = "WhatsAppHelper";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    // Optional: WhatsApp Business package if you need to support it
    // private static final String WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b";


    /**
     * Check if WhatsApp is installed on the device
     *
     * @param context Current context
     * @return true if WhatsApp is installed, false otherwise
     */
    public static boolean isWhatsAppInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(WHATSAPP_PACKAGE, PackageManager.GET_ACTIVITIES);
            return true;  // WhatsApp is installed
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "WhatsApp is not installed");
            return false; // WhatsApp is not installed
        }
    }


    /**
     * Send a WhatsApp message directly to a phone number using the whatsapp://send URI scheme.
     * This method attempts to open a chat with the specified number.
     *
     * @param context Current context
     * @param phoneNumber Recipient phone number with country code (e.g., +919876543210). Must start with '+'.
     * @param message Message to send (optional)
     * @return true if intent was launched successfully, false otherwise
     */
    public static boolean sendMessage(Context context, String phoneNumber, String message) {
        // First, check if WhatsApp is installed
        if (!isWhatsAppInstalled(context)) {
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "WhatsApp is not installed, cannot send message.");
            return false;
        }

        try {
            // Ensure phone number is properly formatted for the URI
            // Remove '+' initially for the URI scheme, but keep it for the message if needed
            String formattedPhoneNumber = phoneNumber;
            if (formattedPhoneNumber.startsWith("+")) {
                formattedPhoneNumber = formattedPhoneNumber.substring(1);
            }

            // Construct the whatsapp://send URI
            // Use Uri.encode for the message text to handle special characters
            String uri = "whatsapp://send?phone=" + formattedPhoneNumber;
            if (message != null && !message.isEmpty()) {
                uri += "&text=" + Uri.encode(message);
            }

            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setData(Uri.parse(uri));
            sendIntent.setPackage(WHATSAPP_PACKAGE); // Explicitly target WhatsApp

            // Verify that WhatsApp can handle this intent before starting the activity
            if (sendIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(sendIntent);
                Log.d(TAG, "Launched WhatsApp intent for phone number: " + phoneNumber);
                return true; // Successfully launched intent
            } else {
                Log.e(TAG, "No WhatsApp app found to handle the whatsapp://send intent");
                // This case should ideally be covered by isWhatsAppInstalled(), but as a fallback
                Toast.makeText(context, "Could not open WhatsApp chat.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending WhatsApp message via URI scheme", e);
            Toast.makeText(context, "Error sending WhatsApp message.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
