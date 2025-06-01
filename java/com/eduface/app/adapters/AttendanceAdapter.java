package com.eduface.app.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eduface.app.R;
import com.eduface.app.models.Attendance; // Import your Attendance model
// Removed Timestamp import as your model uses Date
// import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date; // Import Date
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of Attendance objects in a RecyclerView,
 * using the item_attendance.xml layout and your Attendance model.
 */
public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {

    private List<Attendance> attendanceList;
    private Context context;

    // Constructor
    public AttendanceAdapter(List<Attendance> attendanceList, Context context) {
        this.attendanceList = attendanceList;
        this.context = context;
    }

    // ViewHolder class to hold the views for a single list item (item_attendance.xml)
    public static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView meetingTitleTextView; // Corresponds to meeting_title in XML
        TextView attendanceStatusTextView; // Corresponds to status_badge in XML
        TextView studentNameTextView; // Corresponds to student_name in XML
        TextView studentEmailTextView; // Corresponds to student_email in XML
        TextView joinTimeTextView; // Corresponds to join_time in XML
        TextView durationTextView; // Corresponds to duration in XML
        TextView joinTimeLabelTextView; // Corresponds to join_time_label in XML
        TextView durationLabelTextView; // Corresponds to duration_label in XML


        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link the views from item_attendance.xml to the ViewHolder
            meetingTitleTextView = itemView.findViewById(R.id.meeting_title);
            attendanceStatusTextView = itemView.findViewById(R.id.status_badge); // Link to status_badge
            studentNameTextView = itemView.findViewById(R.id.student_name); // Link to student_name
            studentEmailTextView = itemView.findViewById(R.id.student_email); // Link to student_email
            joinTimeTextView = itemView.findViewById(R.id.join_time); // Link to join_time
            durationTextView = itemView.findViewById(R.id.duration); // Link to duration
            joinTimeLabelTextView = itemView.findViewById(R.id.join_time_label); // Link to label
            durationLabelTextView = itemView.findViewById(R.id.duration_label); // Link to label
        }
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_attendance.xml layout for each list item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        // Get the Attendance object for the current position
        Attendance attendance = attendanceList.get(position);

        // --- Bind the data from the Attendance object to the views ---

        if (attendance != null) {
            // Set meeting title
            holder.meetingTitleTextView.setText(attendance.getMeetingTitle());

            // Set student name and email
            holder.studentNameTextView.setText(attendance.getStudentName());
            holder.studentEmailTextView.setText(attendance.getStudentEmail());


            // Set attendance status and style based on the 'present' boolean
            if (attendance.isPresent()) {
                holder.attendanceStatusTextView.setText("Present");
                holder.attendanceStatusTextView.setBackgroundResource(R.drawable.status_present_background); // Assuming you have this drawable
                holder.attendanceStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.white)); // Assuming white text on background
            } else {
                holder.attendanceStatusTextView.setText("Absent");
                holder.attendanceStatusTextView.setBackgroundResource(R.drawable.status_absent_background); // Assuming you have this drawable
                holder.attendanceStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.white)); // Assuming white text on background
            }


            // Set join time
            Date joinedAt = attendance.getJoinedAt(); // Use Date from your model
            if (joinedAt != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US); // Matches format in XML tools:text
                holder.joinTimeTextView.setText(dateFormat.format(joinedAt));
                holder.joinTimeLabelTextView.setVisibility(View.VISIBLE);
                holder.joinTimeTextView.setVisibility(View.VISIBLE);
            } else {
                holder.joinTimeTextView.setText("N/A");
                holder.joinTimeLabelTextView.setVisibility(View.GONE); // Hide label if no time
                holder.joinTimeTextView.setVisibility(View.GONE);
            }

            // Set duration (calculated in your Attendance model)
            String duration = attendance.getFormattedDuration(); // Use the formatted duration from your model
            if (!TextUtils.isEmpty(duration)) {
                holder.durationTextView.setText(duration);
                holder.durationLabelTextView.setVisibility(View.VISIBLE);
                holder.durationTextView.setVisibility(View.VISIBLE);
            } else {
                holder.durationTextView.setText("N/A");
                holder.durationLabelTextView.setVisibility(View.GONE); // Hide label if no duration
                holder.durationTextView.setVisibility(View.GONE);
            }


        } else {
            // Handle case where attendance object is null
            holder.meetingTitleTextView.setText("Error loading attendance");
            holder.attendanceStatusTextView.setText("");
            holder.studentNameTextView.setText("");
            holder.studentEmailTextView.setText("");
            holder.joinTimeTextView.setText("");
            holder.durationTextView.setText("");
            holder.joinTimeLabelTextView.setVisibility(View.GONE);
            holder.durationLabelTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        // Return the number of items in the list
        return attendanceList != null ? attendanceList.size() : 0;
    }

    // Method to update the data in the adapter
    public void updateAttendance(List<Attendance> newAttendanceList) {
        this.attendanceList = newAttendanceList;
        notifyDataSetChanged(); // Notify the RecyclerView that the data has changed
    }
}
