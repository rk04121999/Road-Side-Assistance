
package com.example.roadsideassistance;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyRequestAdapter extends RecyclerView.Adapter<MyRequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> requestList;
    private final FirebaseFirestore firestore;

    public MyRequestAdapter(Context context, List<DocumentSnapshot> requestList) {
        this.context = context;
        this.requestList = requestList;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        DocumentSnapshot document = requestList.get(position);

        String requestId = document.getId();
        String serviceType = getText(document, "serviceType", "Service not specified");
        String vehicleType = getText(document, "vehicleType", "Not specified");
        String issue = getText(document, "issue", "Not specified");
        String location = getText(document, "location", "Not specified");
        String status = getText(document, "status", "Pending");

        holder.requestService.setText(serviceType);
        holder.requestVehicle.setText("Vehicle: " + vehicleType);
        holder.requestIssue.setText("Issue: " + issue);
        holder.requestLocation.setText("Location: " + location);
        holder.requestStatus.setText(status);
        applyStatusStyle(holder.requestStatus, status);

        Object createdAt = document.get("createdAt");

        if (createdAt instanceof Timestamp) {
            Date date = ((Timestamp) createdAt).toDate();
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.requestDate.setText("Submitted: " + dateFormat.format(date));
        } else {
            holder.requestDate.setText("Submitted: Date unavailable");
        }

        String technicianName = document.getString("technicianName");
        String technicianPhone = document.getString("technicianPhone");
        String technicianVehicle = document.getString("technicianVehicle");

        boolean technicianAssigned =
                technicianName != null && !technicianName.trim().isEmpty()
                        && technicianPhone != null && !technicianPhone.trim().isEmpty()
                        && technicianVehicle != null && !technicianVehicle.trim().isEmpty()
                        && !status.equalsIgnoreCase("Pending")
                        && !status.equalsIgnoreCase("Cancelled");

        if (technicianAssigned) {
            holder.layoutTechnician.setVisibility(View.VISIBLE);
            holder.technicianName.setText("Name: " + technicianName);
            holder.technicianPhone.setText("Phone: " + technicianPhone);
            holder.technicianVehicle.setText("Vehicle: " + technicianVehicle);
        } else {
            holder.layoutTechnician.setVisibility(View.GONE);
            holder.technicianName.setText("");
            holder.technicianPhone.setText("");
            holder.technicianVehicle.setText("");
        }

        boolean isPending = status.equalsIgnoreCase("Pending");

        holder.btnEditRequest.setVisibility(isPending ? View.VISIBLE : View.GONE);
        holder.btnCancelRequest.setVisibility(isPending ? View.VISIBLE : View.GONE);

        boolean isCompleted = status.equalsIgnoreCase("Completed");
        holder.btnGiveFeedback.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
        holder.btnGiveFeedback.setText("Give Feedback");
        holder.btnGiveFeedback.setEnabled(false);

        if (isCompleted) {
            firestore.collection("feedback")
                    .document(requestId)
                    .get()
                    .addOnSuccessListener(feedbackDocument -> {
                        int currentPosition = holder.getBindingAdapterPosition();

                        if (currentPosition == RecyclerView.NO_POSITION
                                || !requestList.get(currentPosition).getId().equals(requestId)) {
                            return;
                        }

                        if (feedbackDocument.exists()) {
                            holder.btnGiveFeedback.setText("Feedback Submitted");
                            holder.btnGiveFeedback.setEnabled(false);
                        } else {
                            holder.btnGiveFeedback.setText("Give Feedback");
                            holder.btnGiveFeedback.setEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                            holder.btnGiveFeedback.setText("Retry Feedback");
                            holder.btnGiveFeedback.setEnabled(true);
                        }
                    });
        }

        holder.btnGiveFeedback.setOnClickListener(v -> {
            Intent intent = new Intent(context, Feedback_Activity.class);
            intent.putExtra("requestId", requestId);
            context.startActivity(intent);
        });

        holder.btnEditRequest.setOnClickListener(v -> {
            Intent intent = new Intent(context, Service_Req_Activity.class);
            intent.putExtra("requestId", requestId);
            intent.putExtra("serviceName", serviceType);
            intent.putExtra("vehicleType", vehicleType);
            intent.putExtra("issue", issue);
            intent.putExtra("location", location);
            context.startActivity(intent);
        });

        holder.btnCancelRequest.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Cancel Service Request")
                    .setMessage("Are you sure you want to cancel this request?")
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Yes, Cancel", (dialog, which) ->
                            firestore.collection("serviceRequests")
                                    .document(requestId)
                                    .update(
                                            "status", "Cancelled",
                                            "updatedAt", FieldValue.serverTimestamp()
                                    )
                                    .addOnSuccessListener(unused ->
                                            Toast.makeText(context,
                                                    "Request cancelled successfully",
                                                    Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context,
                                                    "Unable to cancel request: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show()))
                    .show();
        });
    }

    private String getText(DocumentSnapshot document, String field, String fallback) {
        String value = document.getString(field);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void applyStatusStyle(TextView textView, String status) {
        textView.setText(status);
        textView.setTextColor(android.graphics.Color.WHITE);

        int background;

        switch (status.toLowerCase(Locale.ROOT)) {
            case "assigned":
            case "accepted":
                background = android.graphics.Color.rgb(25, 135, 84);
                break;
            case "in progress":
                background = android.graphics.Color.rgb(230, 126, 34);
                break;
            case "completed":
                background = android.graphics.Color.rgb(46, 125, 50);
                break;
            case "cancelled":
            case "rejected":
                background = android.graphics.Color.rgb(198, 40, 40);
                break;
            default:
                background = android.graphics.Color.rgb(198, 40, 40);
                break;
        }

        textView.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(background));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView requestService;
        TextView requestVehicle;
        TextView requestIssue;
        TextView requestLocation;
        TextView requestStatus;
        TextView requestDate;
        TextView technicianName;
        TextView technicianPhone;
        TextView technicianVehicle;
        LinearLayout layoutTechnician;
        Button btnEditRequest;
        Button btnCancelRequest;
        Button btnGiveFeedback;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            requestService = itemView.findViewById(R.id.RequestService);
            requestVehicle = itemView.findViewById(R.id.RequestVehicle);
            requestIssue = itemView.findViewById(R.id.RequestIssue);
            requestLocation = itemView.findViewById(R.id.RequestLocation);
            requestStatus = itemView.findViewById(R.id.RequestStatus);
            requestDate = itemView.findViewById(R.id.RequestDate);

            layoutTechnician = itemView.findViewById(R.id.layoutTechnician);
            technicianName = itemView.findViewById(R.id.tvTechnicianName);
            technicianPhone = itemView.findViewById(R.id.tvTechnicianPhone);
            technicianVehicle = itemView.findViewById(R.id.tvTechnicianVehicle);

            btnEditRequest = itemView.findViewById(R.id.btnEditRequest);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
            btnGiveFeedback = itemView.findViewById(R.id.btnGiveFeedback);
        }
    }
}
