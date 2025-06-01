package com.eduface.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eduface.app.R;
import com.eduface.app.models.Meeting; // Import your Meeting model
import com.eduface.app.ui.meeting.MeetingActivity; // Import your MeetingActivity
import com.eduface.app.ui.dashboard.TeacherDashboardActivity; // Import TeacherDashboardActivity if needed for context checks
import com.google.firebase.Timestamp; // Import Timestamp

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Adapter for displaying a list of Meeting objects in a RecyclerView
public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder> {

    private List<Meeting> meetingList;
    private Context context;
    private OnMeetingActionListener listener; // Listener for button clicks

    // Interface to handle button clicks in the adapter
    public interface OnMeetingActionListener {
        void onJoinMeetingClick(Meeting meeting);
        void onEndMeetingClick(Meeting meeting);
    }

    // Constructor
    public MeetingAdapter(List<Meeting> meetingList, Context context, OnMeetingActionListener listener) {
        this.meetingList = meetingList;
        this.context = context;
        this.listener = listener;
    }

    // ViewHolder class to hold the views for a single list item (item_meeting.xml)
    public static class MeetingViewHolder extends RecyclerView.ViewHolder {
        TextView meetingTitleTextView;
        TextView meetingStatusTextView;
        TextView meetingCodeTextView;
        TextView dateTimeTextView; // Changed from createdAtTextView to be more general
        Button joinMeetingButton;
        Button endMeetingButton;

        public MeetingViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link the views from item_meeting.xml to the ViewHolder
            meetingTitleTextView = itemView.findViewById(R.id.meeting_title);
            meetingStatusTextView = itemView.findViewById(R.id.meeting_status);
            meetingCodeTextView = itemView.findViewById(R.id.meeting_code);
            dateTimeTextView = itemView.findViewById(R.id.created_at); // Using the same ID from item_meeting.xml
            joinMeetingButton = itemView.findViewById(R.id.btn_join_meeting);
            endMeetingButton = itemView.findViewById(R.id.btn_end_meeting);
        }
    }

    @NonNull
    @Override
    public MeetingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_meeting.xml layout for each list item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meeting, parent, false);
        return new MeetingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MeetingViewHolder holder, int position) {
        // Get the Meeting object for the current position
        Meeting meeting = meetingList.get(position);

        // --- Crucial part: Bind the data from the Meeting object to the views ---

        // Check if meeting object and its fields are not null
        if (meeting != null) {
            // Set meeting title
            holder.meetingTitleTextView.setText(meeting.getTitle());

            // Set meeting code
            holder.meetingCodeTextView.setText(meeting.getMeetingCode());

            // Set meeting status (assuming Meeting model has an 'active' boolean)
            if (meeting.isActive()) {
                holder.meetingStatusTextView.setText("Active");
                // You might also change the background color here based on status
                holder.meetingStatusTextView.setBackgroundResource(R.drawable.status_active_background); // Assuming you have this drawable
            } else {
                holder.meetingStatusTextView.setText("Ended");
                // holder.meetingStatusTextView.setBackgroundResource(R.drawable.status_ended_background); // Assuming you have this drawable
                // TODO: Add or create the drawable resource R.drawable.status_ended_background
                // or handle the styling for ended meetings differently.
            }

            // Display scheduled time if available, otherwise display creation time
            Timestamp scheduledTime = meeting.getScheduledTime();
            Date displayDate = null;
            String label = "Created: ";

            if (scheduledTime != null) {
                displayDate = scheduledTime.toDate();
                label = "Scheduled: ";
            } else {
                // Fallback to createdAt if scheduledTime is missing
                if (meeting.getCreatedAt() != null) {
                    displayDate = meeting.getCreatedAt();
                    label = "Created: ";
                }
            }

            if (displayDate != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);
                holder.dateTimeTextView.setText(label + dateFormat.format(displayDate));
            } else {
                holder.dateTimeTextView.setText("Date/Time N/A");
            }


            // --- Set click listeners for buttons and handle visibility ---

            // Join button click listener
            holder.joinMeetingButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onJoinMeetingClick(meeting);
                }
            });

            // End button visibility and click listener
            // The end button should only be visible for the teacher who created the meeting
            // and if the meeting is still active.
            // You need a way to get the current logged-in user's ID here.
            // This typically comes from PreferenceManager in the Activity/Fragment.
            // We'll assume the listener handles the logic for showing/hiding the end button
            // based on the current user's role and ID in the Activity/Fragment.
            // The listener will be responsible for setting the visibility of the end button
            // for each item after the adapter is created or updated.

            // Example: Hide end button by default, let the Activity/Fragment control visibility
            holder.endMeetingButton.setVisibility(View.GONE); // Hide by default

            // Note: The actual logic to show/hide the end button based on the current user
            // and meeting creator should be handled in the Activity/Fragment that uses this adapter,
            // or you can pass the current user's ID to the adapter and handle it here.
            // For simplicity in this adapter, we'll rely on the listener to manage button actions.
        } else {
            // Handle case where meeting object is null (shouldn't happen if list is populated correctly)
            holder.meetingTitleTextView.setText("Error loading meeting");
            holder.meetingStatusTextView.setText("");
            holder.meetingCodeTextView.setText("");
            holder.dateTimeTextView.setText("");
            holder.joinMeetingButton.setEnabled(false);
            holder.endMeetingButton.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        // Return the number of items in the list
        return meetingList != null ? meetingList.size() : 0;
    }

    // Method to update the data in the adapter and refresh the RecyclerView
    public void updateMeetings(List<Meeting> newMeetingList) {
        this.meetingList = newMeetingList;
        notifyDataSetChanged(); // Notify the RecyclerView that the data has changed
    }
}
