
package com.example.roadsideassistance;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    private FirebaseFirestore db;
    private ListenerRegistration registration;
    private final List<DocumentSnapshot> requests = new ArrayList<>();
    private final List<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView listView;
    private TextView messageView;
    private String technicianEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        technicianEmail = user.getEmail().trim();
        db = FirebaseFirestore.getInstance();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("My Assigned Jobs");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(25, 135, 84));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 8, 0, 8);
        root.addView(title);

        TextView account = new TextView(this);
        account.setText("Technician: " + technicianEmail);
        account.setTextSize(13);
        account.setTextColor(Color.DKGRAY);
        account.setPadding(0, 0, 0, 16);
        root.addView(account);

        messageView = new TextView(this);
        messageView.setText("Loading assigned jobs...");
        messageView.setTextSize(16);
        messageView.setTextColor(Color.DKGRAY);
        messageView.setGravity(Gravity.CENTER);
        messageView.setPadding(12, 24, 12, 24);
        root.addView(messageView);

        listView = new ListView(this);
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                rows
        );
        listView.setAdapter(adapter);

        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        setContentView(root);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < requests.size()) {
                showStatusOptions(requests.get(position));
            }
        });

        loadAssignedJobs();
    }

    private void loadAssignedJobs() {
        if (registration != null) {
            registration.remove();
        }

        messageView.setVisibility(View.VISIBLE);
        messageView.setText("Loading assigned jobs...");

        registration = db.collection("serviceRequests")
                .whereEqualTo("technicianEmail", technicianEmail)
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        requests.clear();
                        rows.clear();
                        adapter.notifyDataSetChanged();
                        listView.setVisibility(View.GONE);
                        messageView.setVisibility(View.VISIBLE);
                        messageView.setText("Unable to load assigned jobs.\n\n"
                                + error.getMessage());
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    requests.clear();
                    rows.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        requests.add(document);

                        String service = value(document, "serviceType", "Not specified");
                        String vehicle = value(document, "vehicleType", "Not specified");
                        String issue = value(document, "issue", "Not specified");
                        String location = value(document, "location", "Not specified");
                        String status = value(document, "status", "Pending");
                        String customerEmail = value(document, "userEmail", "Not available");
                        String technicianName = value(document, "technicianName", "Not available");
                        String technicianPhone = value(document, "technicianPhone", "Not available");
                        String technicianVehicle = value(document, "technicianVehicle", "Not available");

                        String row = "SERVICE: " + service
                                + "\n\nVehicle: " + vehicle
                                + "\nIssue: " + issue
                                + "\nCustomer location: " + location
                                + "\nCustomer email: " + customerEmail
                                + "\n\nAssigned technician: " + technicianName
                                + "\nTechnician phone: " + technicianPhone
                                + "\nTechnician vehicle: " + technicianVehicle
                                + "\n\nSTATUS: " + status
                                + "\n\nTap here to update status";

                        rows.add(row);
                    }

                    adapter.notifyDataSetChanged();

                    if (requests.isEmpty()) {
                        listView.setVisibility(View.GONE);
                        messageView.setVisibility(View.VISIBLE);
                        messageView.setText(
                                "No jobs assigned yet.\n\n"
                                        + "Ask the admin to assign a request to:\n"
                                        + technicianEmail
                        );
                    } else {
                        messageView.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showStatusOptions(DocumentSnapshot request) {
        String currentStatus = value(request, "status", "Pending");
        String[] actions;
        String[] nextStatuses;

        switch (currentStatus.toLowerCase(Locale.ROOT)) {
            case "assigned":
                actions = new String[]{"Accept Job", "Reject Job"};
                nextStatuses = new String[]{"Accepted", "Rejected"};
                break;

            case "accepted":
                actions = new String[]{"Start Work"};
                nextStatuses = new String[]{"In Progress"};
                break;

            case "in progress":
                actions = new String[]{"Mark Completed"};
                nextStatuses = new String[]{"Completed"};
                break;

            default:
                Toast.makeText(
                        this,
                        "No status actions are available for this job",
                        Toast.LENGTH_SHORT
                ).show();
                return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Update Job Status")
                .setItems(actions, (dialog, which) -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("status", nextStatuses[which]);
                    update.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("serviceRequests")
                            .document(request.getId())
                            .update(update)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(
                                            this,
                                            "Job status updated to " + nextStatuses[which],
                                            Toast.LENGTH_SHORT
                                    ).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Status update failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String value(DocumentSnapshot document, String key, String fallback) {
        String result = document.getString(key);
        return result == null || result.trim().isEmpty() ? fallback : result;
    }

    @Override
    protected void onDestroy() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        super.onDestroy();
    }
}
