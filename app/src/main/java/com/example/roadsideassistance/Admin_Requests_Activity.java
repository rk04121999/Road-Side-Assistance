
package com.example.roadsideassistance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class Admin_Requests_Activity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration registration;
    private final List<DocumentSnapshot> requests = new ArrayList<>();
    private RecyclerView recyclerAdminRequests;
    private TextView tvAdminRequestMessage;
    private RequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_requests);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || user.getEmail() == null
                || !user.getEmail().equalsIgnoreCase("admin@rsa.com")) {
            Toast.makeText(this, "Authorized admin account required",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        recyclerAdminRequests = findViewById(R.id.recyclerAdminRequests);
        tvAdminRequestMessage = findViewById(R.id.tvAdminRequestMessage);

        recyclerAdminRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RequestAdapter();
        recyclerAdminRequests.setAdapter(adapter);

        findViewById(R.id.btnManageMechanics).setOnClickListener(v ->
                startActivity(new android.content.Intent(
                        Admin_Requests_Activity.this,
                        Admin_Mechanics_Activity.class
                ))
        );

        tvAdminRequestMessage.setText("Loading requests...");

        user.getIdToken(true)
                .addOnSuccessListener(token -> loadRequests())
                .addOnFailureListener(e ->
                        showMessage("Unable to verify admin account: " + e.getMessage()));
    }

    private void loadRequests() {
        registration = db.collection("serviceRequests")
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        tvAdminRequestMessage.setText("Unable to load requests.");
                        showMessage(error.getMessage());
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    requests.clear();
                    requests.addAll(snapshot.getDocuments());
                    adapter.notifyDataSetChanged();

                    tvAdminRequestMessage.setText(requests.isEmpty()
                            ? "No service requests found."
                            : requests.size() + " service requests");
                });
    }

    private void showMechanicSelection(DocumentSnapshot request) {
        String status = value(request, "status");

        if (!status.equals("Pending")
                && !status.equals("Assigned")
                && !status.equals("Rejected")) {
            showMessage("This request cannot be assigned in its current status");
            return;
        }

        showMessage("Loading available mechanics...");

        db.collection("mechanics")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    List<DocumentSnapshot> validMechanics = new ArrayList<>();
                    List<String> mechanicOptions = new ArrayList<>();

                    for (DocumentSnapshot mechanic : snapshot.getDocuments()) {
                        String name = value(mechanic, "name");
                        String email = value(mechanic, "email");
                        String uid = value(mechanic, "uid");
                        String phone = value(mechanic, "phone");
                        String specialization = value(mechanic, "specialization");
                        String vehicle = value(mechanic, "vehicle");

                        if (name.isEmpty() || email.isEmpty() || uid.isEmpty()
                                || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                || phone.isEmpty() || vehicle.isEmpty()) {
                            continue;
                        }

                        validMechanics.add(mechanic);

                        mechanicOptions.add(
                                name
                                        + "\n" + specialization
                                        + "\nEmail: " + email
                                        + "\nPhone: " + phone
                                        + "\nVehicle: " + vehicle
                        );
                    }

                    if (validMechanics.isEmpty()) {
                        showMessage("No registered and available mechanics found. "
                                + "Add a mechanic account first.");
                        return;
                    }

                    String[] options = mechanicOptions.toArray(new String[0]);

                    new AlertDialog.Builder(this)
                            .setTitle("Select Available Mechanic")
                            .setItems(options, (dialog, which) ->
                                    assignMechanic(request, validMechanics.get(which)))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        showMessage("Unable to load mechanics: " + e.getMessage()));
    }

    private void assignMechanic(DocumentSnapshot request, DocumentSnapshot mechanic) {
        String technicianName = value(mechanic, "name");
        String technicianEmail = value(mechanic, "email").toLowerCase(Locale.ROOT);
        String technicianUid = value(mechanic, "uid");
        String technicianPhone = value(mechanic, "phone");
        String technicianVehicle = value(mechanic, "vehicle");

        if (technicianName.isEmpty() || technicianEmail.isEmpty()
                || technicianUid.isEmpty() || technicianPhone.isEmpty()
                || technicianVehicle.isEmpty()
                || !android.util.Patterns.EMAIL_ADDRESS.matcher(technicianEmail).matches()) {
            showMessage("Selected mechanic has incomplete account details.");
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("technicianName", technicianName);
        update.put("technicianEmail", technicianEmail);
        update.put("technicianUid", technicianUid);
        update.put("technicianPhone", technicianPhone);
        update.put("technicianVehicle", technicianVehicle);
        update.put("status", "Assigned");
        update.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("serviceRequests")
                .document(request.getId())
                .update(update)
                .addOnSuccessListener(result ->
                        showMessage(technicianName + " assigned successfully"))
                .addOnFailureListener(e ->
                        showMessage("Assignment failed: " + e.getMessage()));
    }

    private String value(DocumentSnapshot document, String key) {
        String result = document.getString(key);
        return result == null ? "" : result.trim();
    }

    private void showMessage(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

        @NonNull
        @Override
        public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_request, parent, false);
            return new RequestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
            DocumentSnapshot request = requests.get(position);

            String service = value(request, "serviceType");
            String status = value(request, "status");
            String location = value(request, "location");
            String technician = value(request, "technicianName");

            holder.tvService.setText(service.isEmpty() ? "Service Request" : service);
            holder.tvStatus.setText("Status: " + (status.isEmpty() ? "Unknown" : status));
            holder.tvLocation.setText("Location: "
                    + (location.isEmpty() ? "Not provided" : location));
            holder.tvTechnician.setText("Technician: "
                    + (technician.isEmpty() ? "Not assigned" : technician));
            holder.tvRequestId.setText("Request ID: " + request.getId());

            if (status.equals("Completed")) {
                holder.tvStatus.setTextColor(0xFF198754);
            } else if (status.equals("Pending") || status.equals("Rejected")) {
                holder.tvStatus.setTextColor(0xFFD32F2F);
            } else {
                holder.tvStatus.setTextColor(0xFFEF6C00);
            }

            boolean canAssign = status.equals("Pending")
                    || status.equals("Assigned")
                    || status.equals("Rejected");

            holder.btnAssign.setEnabled(canAssign);
            holder.btnAssign.setText(
                    canAssign ? "Assign Technician" : "Assignment Unavailable");

            holder.btnAssign.setOnClickListener(v -> showMechanicSelection(request));
            holder.itemView.setOnClickListener(v -> showMechanicSelection(request));
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        class RequestViewHolder extends RecyclerView.ViewHolder {
            TextView tvService, tvStatus, tvLocation, tvTechnician, tvRequestId;
            Button btnAssign;

            RequestViewHolder(@NonNull View itemView) {
                super(itemView);
                tvService = itemView.findViewById(R.id.tvRequestService);
                tvStatus = itemView.findViewById(R.id.tvRequestStatus);
                tvLocation = itemView.findViewById(R.id.tvRequestLocation);
                tvTechnician = itemView.findViewById(R.id.tvRequestTechnician);
                tvRequestId = itemView.findViewById(R.id.tvRequestId);
                btnAssign = itemView.findViewById(R.id.btnAssignTechnician);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (registration != null) {
            registration.remove();
        }
        super.onDestroy();
    }
}
