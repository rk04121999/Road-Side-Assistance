package com.example.roadsideassistance;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class My_Requests_Activity extends AppCompatActivity {

    private RecyclerView recyclerMyRequests;
    private TextView tvRequestsMessage;
    private MyRequestAdapter adapter;
    private final List<com.google.firebase.firestore.DocumentSnapshot> requests =
            new ArrayList<>();

    private FirebaseFirestore firestore;
    private ListenerRegistration requestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        recyclerMyRequests = findViewById(R.id.recyclerMyRequests);
        tvRequestsMessage = findViewById(R.id.tvRequestsMessage);

        recyclerMyRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRequestAdapter(requests);
        recyclerMyRequests.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        loadRequests();
    }

    private void loadRequests() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvRequestsMessage.setText("Please log in to view your requests.");
            return;
        }

        tvRequestsMessage.setText("Loading your requests...");

        requestListener = firestore.collection("serviceRequests")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        tvRequestsMessage.setText("Unable to load requests.");
                        Toast.makeText(
                                this,
                                "Error: " + error.getLocalizedMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    requests.clear();

                    if (snapshot != null) {
                        requests.addAll(snapshot.getDocuments());
                    }

                    adapter.notifyDataSetChanged();

                    if (requests.isEmpty()) {
                        tvRequestsMessage.setText("You have not submitted any requests yet.");
                    } else {
                        tvRequestsMessage.setText(
                                "Your requests: " + requests.size()
                        );
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (requestListener != null) {
            requestListener.remove();
        }
    }
}
