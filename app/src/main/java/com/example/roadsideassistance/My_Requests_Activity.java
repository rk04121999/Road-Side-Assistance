
package com.example.roadsideassistance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class My_Requests_Activity extends AppCompatActivity {

    private RecyclerView recyclerMyRequests;
    private TextView tvRequestsMessage;
    private Spinner spinnerRequestStatus;
    private Button btnFromDate;
    private Button btnToDate;
    private Button btnClearDateFilter;

    private FirebaseFirestore firestore;
    private MyRequestAdapter adapter;
    private ListenerRegistration requestListener;

    private final List<DocumentSnapshot> allRequests = new ArrayList<>();
    private final List<DocumentSnapshot> requestList = new ArrayList<>();
    private final Map<String, String> lastStatuses = new HashMap<>();

    private String selectedStatus = "All";
    private Calendar fromDate;
    private Calendar toDate;

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

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        recyclerMyRequests = findViewById(R.id.recyclerMyRequests);
        tvRequestsMessage = findViewById(R.id.tvRequestsMessage);
        spinnerRequestStatus = findViewById(R.id.spinnerRequestStatus);
        btnFromDate = findViewById(R.id.btnFromDate);
        btnToDate = findViewById(R.id.btnToDate);
        btnClearDateFilter = findViewById(R.id.btnClearDateFilter);

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
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        selectedStatus = statusOptions[position];
                        applyFilters();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v -> showDatePicker(false));

        btnClearDateFilter.setOnClickListener(v -> {
            fromDate = null;
            toDate = null;
            btnFromDate.setText("From Date");
            btnToDate.setText("To Date");
            applyFilters();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadRequests();
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar initialDate = Calendar.getInstance();

        if (isFromDate && fromDate != null) {
            initialDate.setTimeInMillis(fromDate.getTimeInMillis());
        } else if (!isFromDate && toDate != null) {
            initialDate.setTimeInMillis(toDate.getTimeInMillis());
        } else if (!isFromDate && fromDate != null) {
            initialDate.setTimeInMillis(fromDate.getTimeInMillis());
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, year, month, dayOfMonth) -> {
                    Calendar chosenDate = Calendar.getInstance();
                    chosenDate.set(Calendar.YEAR, year);
                    chosenDate.set(Calendar.MONTH, month);
                    chosenDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    chosenDate.set(Calendar.HOUR_OF_DAY, 0);
                    chosenDate.set(Calendar.MINUTE, 0);
                    chosenDate.set(Calendar.SECOND, 0);
                    chosenDate.set(Calendar.MILLISECOND, 0);

                    if (isFromDate) {
                        fromDate = chosenDate;
                        btnFromDate.setText(
                                "From: " + dateFormat.format(chosenDate.getTime())
                        );
                    } else {
                        chosenDate.set(Calendar.HOUR_OF_DAY, 23);
                        chosenDate.set(Calendar.MINUTE, 59);
                        chosenDate.set(Calendar.SECOND, 59);
                        chosenDate.set(Calendar.MILLISECOND, 999);

                        toDate = chosenDate;
                        btnToDate.setText(
                                "To: " + dateFormat.format(chosenDate.getTime())
                        );
                    }

                    applyFilters();
                },
                initialDate.get(Calendar.YEAR),
                initialDate.get(Calendar.MONTH),
                initialDate.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void loadRequests() {
        if (requestListener != null) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvRequestsMessage.setText(
                    "Please log in to view your requests."
            );
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
                        requestListener = null;

                        tvRequestsMessage.setText(
                                "Unable to load requests. Check your connection or Firebase permissions."
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

                    applyFilters();
                });
    }

    private void applyFilters() {
        requestList.clear();

        if (fromDate != null && toDate != null
                && fromDate.getTimeInMillis() > toDate.getTimeInMillis()) {
            adapter.notifyDataSetChanged();
            tvRequestsMessage.setText(
                    "From Date cannot be later than To Date."
            );
            tvRequestsMessage.setVisibility(View.VISIBLE);
            recyclerMyRequests.setVisibility(View.GONE);
            return;
        }

        List<DocumentSnapshot> filteredRequests = new ArrayList<>();

        for (DocumentSnapshot document : allRequests) {
            String status = document.getString("status");

            boolean matchesStatus = selectedStatus.equalsIgnoreCase("All")
                    || (status != null
                    && status.trim().equalsIgnoreCase(selectedStatus));

            if (!matchesStatus) {
                continue;
            }

            Object createdAtValue = document.get("createdAt");
            long createdAtMillis = -1L;

            if (createdAtValue instanceof Timestamp) {
                createdAtMillis = ((Timestamp) createdAtValue)
                        .toDate().getTime();
            } else if (createdAtValue instanceof Date) {
                createdAtMillis = ((Date) createdAtValue).getTime();
            }

            if (fromDate != null || toDate != null) {
                if (createdAtMillis < 0) {
                    continue;
                }

                if (fromDate != null
                        && createdAtMillis < fromDate.getTimeInMillis()) {
                    continue;
                }

                if (toDate != null
                        && createdAtMillis > toDate.getTimeInMillis()) {
                    continue;
                }
            }

            filteredRequests.add(document);
        }

        Collections.sort(filteredRequests, new Comparator<DocumentSnapshot>() {
            @Override
            public int compare(DocumentSnapshot first, DocumentSnapshot second) {
                long firstTime = getCreatedAtMillis(first);
                long secondTime = getCreatedAtMillis(second);

                return Long.compare(secondTime, firstTime);
            }
        });

        requestList.addAll(filteredRequests);
        adapter.notifyDataSetChanged();

        if (allRequests.isEmpty()) {
            tvRequestsMessage.setText(
                    "You have not submitted any service requests yet."
            );
            tvRequestsMessage.setVisibility(View.VISIBLE);
            recyclerMyRequests.setVisibility(View.GONE);
        } else if (requestList.isEmpty()) {
            tvRequestsMessage.setText(
                    "No requests found for the selected status and date range."
            );
            tvRequestsMessage.setVisibility(View.VISIBLE);
            recyclerMyRequests.setVisibility(View.GONE);
        } else {
            tvRequestsMessage.setText(
                    requestList.size()
                            + (requestList.size() == 1
                            ? " request found"
                            : " requests found")
            );
            tvRequestsMessage.setVisibility(View.VISIBLE);
            recyclerMyRequests.setVisibility(View.VISIBLE);
        }
    }

    private long getCreatedAtMillis(DocumentSnapshot document) {
        Object createdAtValue = document.get("createdAt");

        if (createdAtValue instanceof Timestamp) {
            return ((Timestamp) createdAtValue).toDate().getTime();
        }

        if (createdAtValue instanceof Date) {
            return ((Date) createdAtValue).getTime();
        }

        return Long.MIN_VALUE;
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
