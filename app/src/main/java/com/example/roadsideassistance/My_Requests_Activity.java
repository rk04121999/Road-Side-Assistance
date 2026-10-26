
package com.example.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import java.util.Map;

public class My_Requests_Activity extends AppCompatActivity {

    private RecyclerView recyclerMyRequests;
    private TextView tvRequestsMessage;
    private MyRequestAdapter adapter;
    private final List<DocumentSnapshot> requestList = new ArrayList<>();
    private final Map<String, String> lastStatuses = new HashMap<>();
    private FirebaseFirestore firestore;
    private ListenerRegistration requestsListener;
    private boolean initialSnapshotLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        recyclerMyRequests = findViewById(R.id.recyclerMyRequests);
        tvRequestsMessage = findViewById(R.id.tvRequestsMessage);

        firestore = FirebaseFirestore.getInstance();

        recyclerMyRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRequestAdapter(this, requestList);
        recyclerMyRequests.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadMyRequests();
    }

    private void loadMyRequests() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvRequestsMessage.setVisibility(View.VISIBLE);
            tvRequestsMessage.setText("Please log in to view your requests");
            recyclerMyRequests.setVisibility(View.GONE);
            return;
        }

        if (requestsListener != null) {
            requestsListener.remove();
        }

        initialSnapshotLoaded = false;
        lastStatuses.clear();

        tvRequestsMessage.setVisibility(View.VISIBLE);
        tvRequestsMessage.setText("Loading your requests...");

        requestsListener = firestore.collection("serviceRequests")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        tvRequestsMessage.setVisibility(View.VISIBLE);
                        tvRequestsMessage.setText("Unable to load requests");
                        recyclerMyRequests.setVisibility(View.GONE);

                        Toast.makeText(
                                this,
                                "Error loading requests: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    String requestToReview = null;

                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        DocumentSnapshot document = change.getDocument();
                        String requestId = document.getId();
                        String status = document.getString("status");

                        if (status == null) {
                            status = "Pending";
                        }

                        String previousStatus = lastStatuses.put(requestId, status);

                        if (initialSnapshotLoaded
                                && change.getType() == DocumentChange.Type.MODIFIED
                                && previousStatus != null
                                && !previousStatus.equalsIgnoreCase(status)) {

                            Toast.makeText(
                                    this,
                                    "Request status updated: " + status,
                                    Toast.LENGTH_LONG
                            ).show();

                            if ("Completed".equalsIgnoreCase(status)
                                    && !"Completed".equalsIgnoreCase(previousStatus)) {
                                requestToReview = requestId;
                            }
                        }
                    }

                    initialSnapshotLoaded = true;

                    requestList.clear();
                    requestList.addAll(snapshot.getDocuments());
                    adapter.notifyDataSetChanged();

                    if (requestList.isEmpty()) {
                        tvRequestsMessage.setVisibility(View.VISIBLE);
                        tvRequestsMessage.setText("You have no service requests yet");
                        recyclerMyRequests.setVisibility(View.GONE);
                    } else {
                        tvRequestsMessage.setVisibility(View.GONE);
                        recyclerMyRequests.setVisibility(View.VISIBLE);
                    }

                    if (requestToReview != null) {
                        Intent intent = new Intent(this, Feedback_Activity.class);
                        intent.putExtra("requestId", requestToReview);
                        startActivity(intent);
                    }
                });
    }

    @Override
    protected void onStop() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        super.onStop();
    }
}
