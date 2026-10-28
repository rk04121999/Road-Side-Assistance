
package com.example.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class My_Requests_Activity extends AppCompatActivity {

    private RecyclerView recyclerMyRequests;
    private TextView tvRequestsMessage;
    private Spinner spinnerRequestStatus;
    private FirebaseFirestore firestore;
    private MyRequestAdapter adapter;
    private final List<DocumentSnapshot> allRequests = new ArrayList<>();
    private final List<DocumentSnapshot> requestList = new ArrayList<>();
    private final Map<String, String> lastStatuses = new HashMap<>();
    private ListenerRegistration requestListener;
    private String selectedStatus = "All";

    private final String[] statusOptions = {
            "All",
            "Pending",
            "Assigned",
            "Accepted",
            "In Progress",
            "Completed",
            "Cancelled",
            "Rejected"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        recyclerMyRequests = findViewById(R.id.recyclerMyRequests);
        tvRequestsMessage = findViewById(R.id.tvRequestsMessage);
        spinnerRequestStatus = findViewById(R.id.spinnerRequestStatus);

        firestore = FirebaseFirestore.getInstance();

        recyclerMyRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRequestAdapter(this, requestList);
        recyclerMyRequests.setAdapter(adapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statusOptions
        );
        statusAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerRequestStatus.setAdapter(statusAdapter);

        spinnerRequestStatus.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        selectedStatus = statusOptions[position];
                        applyStatusFilter();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        loadRequests();
    }

    private void loadRequests() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvRequestsMessage.setText("Please log in to view your requests.");
            tvRequestsMessage.setVisibility(View.VISIBLE);
            recyclerMyRequests.setVisibility(View.GONE);
            return;
        }

        tvRequestsMessage.setText("Loading your requests...");
        tvRequestsMessage.setVisibility(View.VISIBLE);

        requestListener = firestore.collection("serviceRequests")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        tvRequestsMessage.setText(
                                "Unable to load requests. Please check your connection or Firebase permissions."
                        );
                        tvRequestsMessage.setVisibility(View.VISIBLE);
                        recyclerMyRequests.setVisibility(View.GONE);
                        Toast.makeText(
                                My_Requests_Activity.this,
                                "Failed to load requests: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        DocumentSnapshot document = change.getDocument();
                        String requestId = document.getId();
                        String newStatus = document.getString("status");

                        if (change.getType() == DocumentChange.Type.MODIFIED) {
                            String oldStatus = lastStatuses.get(requestId);

                            if (newStatus != null
                                    && oldStatus != null
                                    && !oldStatus.equalsIgnoreCase(newStatus)) {

                                Toast.makeText(
                                        My_Requests_Activity.this,
                                        "Request status updated: " + newStatus,
                                        Toast.LENGTH_LONG
                                ).show();

                                if (newStatus.equalsIgnoreCase("Completed")) {
                                    Intent intent = new Intent(
                                            My_Requests_Activity.this,
                                            Feedback_Activity.class
                                    );
                                    intent.putExtra("requestId", requestId);
                                    startActivity(intent);
                                }
                            }
                        }

                        if (newStatus != null) {
                            lastStatuses.put(requestId, newStatus);
                        }
                    }

                    allRequests.clear();
                    allRequests.addAll(snapshot.getDocuments());

                    applyStatusFilter();
                });
    }

    private void applyStatusFilter() {
        requestList.clear();

        for (DocumentSnapshot document : allRequests) {
            String status = document.getString("status");

            if (selectedStatus.equalsIgnoreCase("All")
                    || (status != null
                    && status.trim().equalsIgnoreCase(selectedStatus))) {
                requestList.add(document);
            }
        }

        adapter.notifyDataSetChanged();

        if (allRequests.isEmpty()) {
            tvRequestsMessage.setText("You have not submitted any service requests yet.");
            tvRequestsMessage.setVisibility(View.VISIBLE);
            recyclerMyRequests.setVisibility(View.GONE);
        } else if (requestList.isEmpty()) {
            tvRequestsMessage.setText(
                    "No requests found with status: " + selectedStatus
            );
            tvRequestsMessage.setVisibility(View.VISIBLE);
            recyclerMyRequests.setVisibility(View.GONE);
        } else {
            tvRequestsMessage.setText(
                    requestList.size() + (requestList.size() == 1
                            ? " request found"
                            : " requests found")
            );
            tvRequestsMessage.setVisibility(View.VISIBLE);
            recyclerMyRequests.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }
    }
}
