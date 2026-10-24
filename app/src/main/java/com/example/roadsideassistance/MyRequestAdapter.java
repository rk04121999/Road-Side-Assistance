package com.example.roadsideassistance;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MyRequestAdapter extends RecyclerView.Adapter<MyRequestAdapter.RequestViewHolder> {

    private final List<DocumentSnapshot> requests;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public MyRequestAdapter(List<DocumentSnapshot> requests) {
        this.requests = requests;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        DocumentSnapshot request = requests.get(position);

        String service = request.getString("serviceType");
        String vehicle = getValue(request, "vehicleType");
        String issue = getValue(request, "issue");
        String location = getValue(request, "location");
        String status = request.getString("status");

        if (service == null || service.trim().isEmpty()) {
            service = "Service Request";
        }

        if (status == null || status.trim().isEmpty()) {
            status = "Pending";
        }

        holder.Service.setText(service);
        holder.Vehicle.setText("Vehicle: " + vehicle);
        holder.Issue.setText("Issue: " + issue);
        holder.Location.setText("Location: " + location);
        holder.Status.setText(status);


        String technicianName = "";
        String technicianPhone = "";
        String technicianVehicle = "";

        if (status.equalsIgnoreCase("Assigned")
                || status.equalsIgnoreCase("Accepted")
                || status.equalsIgnoreCase("In Progress")) {

            technicianName = "Rahul Patel";
            technicianPhone = "9876543210";
            technicianVehicle = "Service Van - GJ01AB1234";
        }

        boolean technicianAssigned =
                status.equalsIgnoreCase("Assigned")
                        || status.equalsIgnoreCase("Accepted")
                        || status.equalsIgnoreCase("In Progress");

        holder.layoutTechnician.setVisibility(
                technicianAssigned ? View.VISIBLE : View.GONE
        );

        if (technicianAssigned) {
            holder.tvTechnicianName.setText("Name: " + technicianName);
            holder.tvTechnicianPhone.setText("Phone: " + technicianPhone);
            holder.tvTechnicianVehicle.setText("Vehicle: " + technicianVehicle);
        }

        if (status.equalsIgnoreCase("Completed")) {
            holder.Status.setBackgroundColor(0xFF2E7D32);
        } else if (status.equalsIgnoreCase("Accepted")
                || status.equalsIgnoreCase("Assigned")
                || status.equalsIgnoreCase("In Progress")) {
            holder.Status.setBackgroundColor(0xFF1565C0);
        } else if (status.equalsIgnoreCase("Rejected")
                || status.equalsIgnoreCase("Cancelled")) {
            holder.Status.setBackgroundColor(0xFFB71C1C);
        } else {
            holder.Status.setBackgroundResource(R.drawable.bg_service_red_gradient);
        }

        Timestamp timestamp = request.getTimestamp("createdAt");

        if (timestamp != null) {
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.Date.setText("Submitted: " + dateFormat.format(timestamp.toDate()));
        } else {
            holder.Date.setText("Submitted: Date unavailable");
        }

        boolean isPending = status.equalsIgnoreCase("Pending");

        holder.EditButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
        holder.CancelButton.setVisibility(isPending ? View.VISIBLE : View.GONE);

        String finalService = service;
        holder.EditButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), Service_Req_Activity.class);
            intent.putExtra("requestId", request.getId());
            intent.putExtra("serviceName", finalService);
            intent.putExtra("vehicleType", request.getString("vehicleType"));
            intent.putExtra("issue", request.getString("issue"));
            intent.putExtra("location", request.getString("location"));
            v.getContext().startActivity(intent);
        });

        holder.CancelButton.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Cancel Request")
                    .setMessage("Are you sure you want to cancel this service request?")
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                        db.collection("serviceRequests")
                                .document(request.getId())
                                .update(
                                        "status", "Cancelled",
                                        "updatedAt", FieldValue.serverTimestamp()
                                )
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(
                                                v.getContext(),
                                                "Request cancelled successfully",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                v.getContext(),
                                                "Cancellation failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show()
                                );
                    })
                    .show();
        });
    }

    private String getValue(DocumentSnapshot request, String field) {
        String value = request.getString(field);
        return value == null || value.trim().isEmpty() ? "Not provided" : value;
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView Service, Status, Vehicle, Issue, Location, Date;
        Button EditButton, CancelButton;

        View layoutTechnician;

        TextView tvTechnicianName, tvTechnicianPhone, tvTechnicianVehicle;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            Service = itemView.findViewById(R.id.RequestService);
            Status = itemView.findViewById(R.id.RequestStatus);
            Vehicle = itemView.findViewById(R.id.RequestVehicle);
            Issue = itemView.findViewById(R.id.RequestIssue);
            Location = itemView.findViewById(R.id.RequestLocation);
            Date = itemView.findViewById(R.id.RequestDate);
            EditButton = itemView.findViewById(R.id.btnEditRequest);
            CancelButton = itemView.findViewById(R.id.btnCancelRequest);
            layoutTechnician = itemView.findViewById(R.id.layoutTechnician);
            tvTechnicianName = itemView.findViewById(R.id.tvTechnicianName);
            tvTechnicianPhone = itemView.findViewById(R.id.tvTechnicianPhone);
            tvTechnicianVehicle = itemView.findViewById(R.id.tvTechnicianVehicle);

        }
    }
}