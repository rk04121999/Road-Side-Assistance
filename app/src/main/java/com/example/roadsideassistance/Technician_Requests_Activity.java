
package com.example.roadsideassistance;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Technician_Requests_Activity extends AppCompatActivity {

    private TextView tvTechnicianName;
    private TextView tvTechnicianEmail;
    private TextView tvTotalJobs;
    private TextView tvActiveJobs;
    private TextView tvCompletedJobs;
    private TextView tvTechnicianMessage;

    private RecyclerView recyclerTechnicianRequests;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String technicianEmail;
    private String technicianUid;

    private TechnicianRequestAdapter adapter;
    private final List<DocumentSnapshot> requestList = new ArrayList<>();
    private ListenerRegistration requestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_requests);

        tvTechnicianName = findViewById(R.id.tvTechnicianName);
        tvTechnicianEmail = findViewById(R.id.tvTechnicianEmail);
        tvTotalJobs = findViewById(R.id.tvTotalJobs);
        tvActiveJobs = findViewById(R.id.tvActiveJobs);
        tvCompletedJobs = findViewById(R.id.tvCompletedJobs);
        tvTechnicianMessage = findViewById(R.id.tvTechnicianMessage);
        recyclerTechnicianRequests = findViewById(R.id.recyclerTechnicianRequests);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        technicianEmail = user.getEmail().trim().toLowerCase(Locale.ROOT);
        technicianUid = user.getUid();

        tvTechnicianEmail.setText(technicianEmail);
        tvTechnicianName.setText("Welcome, Technician");

        recyclerTechnicianRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianRequestAdapter(requestList, this::showJobActions);
        recyclerTechnicianRequests.setAdapter(adapter);

        loadTechnicianProfile();
        loadAssignedRequests();
    }

    private void loadTechnicianProfile() {
        db.collection("mechanics")
                .whereEqualTo("uid", technicianUid)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String name = querySnapshot.getDocuments()
                                .get(0).getString("name");

                        if (name != null && !name.trim().isEmpty()) {
                            tvTechnicianName.setText("Welcome, " + name.trim());
                        }
                    }
                });
    }

    private void loadAssignedRequests() {
        tvTechnicianMessage.setVisibility(View.VISIBLE);
        tvTechnicianMessage.setText("Loading your assigned jobs...");

        requestListener = db.collection("serviceRequests")
                .whereEqualTo("technicianEmail", technicianEmail)
                .addSnapshotListener((querySnapshot, error) -> {

                    if (error != null) {
                        tvTechnicianMessage.setVisibility(View.VISIBLE);
                        tvTechnicianMessage.setText("Unable to load jobs. Please try again.");
                        Toast.makeText(
                                this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (querySnapshot == null) {
                        return;
                    }

                    requestList.clear();
                    requestList.addAll(querySnapshot.getDocuments());

                    adapter.notifyDataSetChanged();
                    updateJobCounts();

                    if (requestList.isEmpty()) {
                        tvTechnicianMessage.setVisibility(View.VISIBLE);
                        tvTechnicianMessage.setText("No assigned jobs available right now.");
                    } else {
                        tvTechnicianMessage.setVisibility(View.GONE);
                    }
                });
    }

    private void updateJobCounts() {
        int total = requestList.size();
        int active = 0;
        int completed = 0;

        for (DocumentSnapshot document : requestList) {
            String status = document.getString("status");

            if ("Completed".equalsIgnoreCase(status)) {
                completed++;
            } else if ("Assigned".equalsIgnoreCase(status)
                    || "Accepted".equalsIgnoreCase(status)
                    || "In Progress".equalsIgnoreCase(status)) {
                active++;
            }
        }

        tvTotalJobs.setText(String.valueOf(total));
        tvActiveJobs.setText(String.valueOf(active));
        tvCompletedJobs.setText(String.valueOf(completed));
    }

    private void showJobActions(DocumentSnapshot document) {
        String status = document.getString("status");

        if (status == null) {
            return;
        }

        if ("Assigned".equalsIgnoreCase(status)) {
            String[] options = {"Accept Job", "Reject Job"};

            new AlertDialog.Builder(this)
                    .setTitle("Manage Service Request")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            updateJobStatus(document, "Accepted");
                        } else {
                            updateJobStatus(document, "Rejected");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        } else if ("Accepted".equalsIgnoreCase(status)) {
            confirmStatusChange(
                    document,
                    "Start Work",
                    "Are you ready to start this service?",
                    "In Progress"
            );

        } else if ("In Progress".equalsIgnoreCase(status)) {
            confirmStatusChange(
                    document,
                    "Complete Job",
                    "Are you sure this service has been completed?",
                    "Completed"
            );
        }
    }

    private void confirmStatusChange(
            DocumentSnapshot document,
            String title,
            String message,
            String newStatus
    ) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) ->
                        updateJobStatus(document, newStatus))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateJobStatus(DocumentSnapshot document, String newStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("serviceRequests")
                .document(document.getId())
                .update(updates)
                .addOnSuccessListener(unused ->
                        Toast.makeText(
                                this,
                                "Job status updated to " + newStatus,
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not update status: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (requestListener != null) {
            requestListener.remove();
        }
    }

    interface JobActionListener {
        void onJobAction(DocumentSnapshot document);
    }

    static class TechnicianRequestAdapter
            extends RecyclerView.Adapter<TechnicianRequestAdapter.JobViewHolder> {

        private final List<DocumentSnapshot> requests;
        private final JobActionListener listener;

        TechnicianRequestAdapter(
                List<DocumentSnapshot> requests,
                JobActionListener listener
        ) {
            this.requests = requests;
            this.listener = listener;
        }

        @NonNull
        @Override
        public JobViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_technician_request, parent, false);

            return new JobViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull JobViewHolder holder,
                int position
        ) {
            DocumentSnapshot document = requests.get(position);

            String service = getValue(document, "serviceType", "Service Request");
            String vehicle = getValue(document, "vehicleType", "Not provided");
            String issue = getValue(document, "issue", "Not provided");
            String location = getValue(document, "location", "Not provided");
            String customer = getValue(
                    document,
                    "userName",
                    getValue(document, "customerName",
                            getValue(document, "userEmail", "Not provided"))
            );
            String phone = getValue(
                    document,
                    "userPhone",
                    getValue(document, "customerPhone", "")
            );
            String status = getValue(document, "status", "Unknown");

            holder.tvJobService.setText(service);
            holder.tvJobVehicle.setText("Vehicle: " + vehicle);
            holder.tvJobIssue.setText("Issue: " + issue);
            holder.tvJobLocation.setText("Location: " + location);
            holder.tvJobCustomer.setText("Customer: " + customer);

            if (phone.trim().isEmpty()) {
                holder.tvJobPhone.setVisibility(View.GONE);
            } else {
                holder.tvJobPhone.setVisibility(View.VISIBLE);
                holder.tvJobPhone.setText("Phone: " + phone);
            }

            holder.tvJobStatus.setText(status);
            setStatusColor(holder.tvJobStatus, status);

            if ("Assigned".equalsIgnoreCase(status)) {
                holder.btnJobAction.setText("Review Job");
                holder.btnJobAction.setEnabled(true);
            } else if ("Accepted".equalsIgnoreCase(status)) {
                holder.btnJobAction.setText("Start Work");
                holder.btnJobAction.setEnabled(true);
            } else if ("In Progress".equalsIgnoreCase(status)) {
                holder.btnJobAction.setText("Complete Job");
                holder.btnJobAction.setEnabled(true);
            } else {
                holder.btnJobAction.setText(status);
                holder.btnJobAction.setEnabled(false);
            }

            holder.btnJobAction.setOnClickListener(v ->
                    listener.onJobAction(document));
        }

        private static String getValue(
                DocumentSnapshot document,
                String key,
                String fallback
        ) {
            String value = document.getString(key);

            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }

            return value.trim();
        }

        private static void setStatusColor(TextView textView, String status) {
            if ("Completed".equalsIgnoreCase(status)) {
                textView.setTextColor(Color.parseColor("#198754"));
            } else if ("Pending".equalsIgnoreCase(status)
                    || "Rejected".equalsIgnoreCase(status)
                    || "Cancelled".equalsIgnoreCase(status)) {
                textView.setTextColor(Color.parseColor("#D32F2F"));
            } else if ("In Progress".equalsIgnoreCase(status)
                    || "Accepted".equalsIgnoreCase(status)) {
                textView.setTextColor(Color.parseColor("#1976D2"));
            } else {
                textView.setTextColor(Color.parseColor("#E6A000"));
            }
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        static class JobViewHolder extends RecyclerView.ViewHolder {

            TextView tvJobService;
            TextView tvJobStatus;
            TextView tvJobVehicle;
            TextView tvJobIssue;
            TextView tvJobLocation;
            TextView tvJobCustomer;
            TextView tvJobPhone;
            Button btnJobAction;
            CardView cardJob;

            JobViewHolder(@NonNull View itemView) {
                super(itemView);

                cardJob = itemView.findViewById(R.id.cardTechnicianJob);
                tvJobService = itemView.findViewById(R.id.tvJobService);
                tvJobStatus = itemView.findViewById(R.id.tvJobStatus);
                tvJobVehicle = itemView.findViewById(R.id.tvJobVehicle);
                tvJobIssue = itemView.findViewById(R.id.tvJobIssue);
                tvJobLocation = itemView.findViewById(R.id.tvJobLocation);
                tvJobCustomer = itemView.findViewById(R.id.tvJobCustomer);
                tvJobPhone = itemView.findViewById(R.id.tvJobPhone);
                btnJobAction = itemView.findViewById(R.id.btnJobAction);
            }
        }
    }
}
