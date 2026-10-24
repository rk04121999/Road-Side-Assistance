package com.example.roadsideassistance;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Service_Req_Activity extends AppCompatActivity {

    private TextView SelectedService;
    private AutoCompleteTextView etVehicleType, etServiceIssue;
    private EditText etServiceLocation;
    private Button btnSubmitServiceRequest;
    private FirebaseFirestore firestore;
    private String serviceName;
    private String requestId;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_req);

        SelectedService = findViewById(R.id.tvSelectedService);
        etVehicleType = findViewById(R.id.etVehicleType);
        etServiceIssue = findViewById(R.id.etServiceIssue);
        etServiceLocation = findViewById(R.id.etServiceLocation);
        btnSubmitServiceRequest = findViewById(R.id.btnSubmitServiceRequest);

        firestore = FirebaseFirestore.getInstance();

        serviceName = getIntent().getStringExtra("serviceName");
        requestId = getIntent().getStringExtra("requestId");
        isEditMode = requestId != null && !requestId.trim().isEmpty();

        if (serviceName == null || serviceName.trim().isEmpty()) {
            serviceName = "General Assistance";
        }

        SelectedService.setText(serviceName);

        setupDropdown(etVehicleType, new String[]{
                "Bike", "Scooter", "Car", "SUV", "Van", "Truck", "Other"
        });

        setupDropdown(etServiceIssue, getIssues(serviceName));

        if (isEditMode) {
            btnSubmitServiceRequest.setText("Update Request");
            loadRequestForEditing();
        } else {
            btnSubmitServiceRequest.setText("Submit Request");
        }

        btnSubmitServiceRequest.setOnClickListener(v -> submitRequest());
    }

    private void setupDropdown(AutoCompleteTextView field, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                options
        );

        field.setAdapter(adapter);
        field.setThreshold(1);
        field.setOnClickListener(v -> field.showDropDown());
    }

    private String[] getIssues(String service) {
        switch (service) {
            case "Tow Truck":
                return new String[]{
                        "Vehicle Breakdown", "Accident Recovery",
                        "Vehicle Won't Start", "Other"
                };

            case "Flat Tire Change":
                return new String[]{
                        "Flat Tire", "Puncture", "Damaged Tire",
                        "Spare Tire Needed", "Other"
                };

            case "Battery Jumpstart":
            case "Battery Maintenance":
                return new String[]{
                        "Dead Battery", "Vehicle Not Starting",
                        "Jump Start Required", "Battery Check",
                        "Battery Maintenance", "Other"
                };

            case "Roadside Repair":
                return new String[]{
                        "Engine Trouble", "Overheating", "Brake Issue",
                        "Mechanical Failure", "Other"
                };

            case "Fuel Delivery":
                return new String[]{
                        "Out of Fuel", "Petrol Needed", "Diesel Needed", "Other"
                };

            case "Lockout Service":
                return new String[]{
                        "Keys Locked Inside", "Lost Keys", "Broken Key", "Other"
                };

            case "Oil Delivery":
                return new String[]{
                        "Engine Oil Needed", "Oil Level Low", "Other"
                };

            default:
                return new String[]{
                        "General Breakdown", "Accident Assistance",
                        "Vehicle Problem", "Other"
                };
        }
    }

    private void loadRequestForEditing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnSubmitServiceRequest.setEnabled(false);

        firestore.collection("serviceRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!document.exists()
                            || !user.getUid().equals(document.getString("userId"))
                            || !"Pending".equalsIgnoreCase(document.getString("status"))) {
                        Toast.makeText(
                                this,
                                "This request can no longer be edited",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                        return;
                    }

                    serviceName = document.getString("serviceType");

                    if (serviceName == null || serviceName.trim().isEmpty()) {
                        serviceName = "General Assistance";
                    }

                    SelectedService.setText(serviceName);

                    setupDropdown(etServiceIssue, getIssues(serviceName));

                    String vehicle = document.getString("vehicleType");
                    String issue = document.getString("issue");
                    String location = document.getString("location");

                    etVehicleType.setText(vehicle == null ? "" : vehicle, false);
                    etServiceIssue.setText(issue == null ? "" : issue, false);
                    etServiceLocation.setText(location == null ? "" : location);

                    btnSubmitServiceRequest.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            this,
                            "Unable to load request: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void submitRequest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in before submitting a request",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String vehicle = etVehicleType.getText().toString().trim();
        String issue = etServiceIssue.getText().toString().trim();
        String location = etServiceLocation.getText().toString().trim();

        if (vehicle.isEmpty()) {
            etVehicleType.setError("Enter or select vehicle type");
            etVehicleType.requestFocus();
            return;
        }

        if (issue.isEmpty()) {
            etServiceIssue.setError("Enter or select your issue");
            etServiceIssue.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            etServiceLocation.setError("Enter your location");
            etServiceLocation.requestFocus();
            return;
        }

        btnSubmitServiceRequest.setEnabled(false);
        btnSubmitServiceRequest.setText(isEditMode ? "Updating..." : "Submitting...");

        Map<String, Object> request = new HashMap<>();
        request.put("serviceType", serviceName);
        request.put("vehicleType", vehicle);
        request.put("issue", issue);
        request.put("location", location);
        request.put("updatedAt", FieldValue.serverTimestamp());

        if (isEditMode) {
            firestore.collection("serviceRequests")
                    .document(requestId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (!document.exists()
                                || !user.getUid().equals(document.getString("userId"))
                                || !"Pending".equalsIgnoreCase(document.getString("status"))) {
                            btnSubmitServiceRequest.setEnabled(true);
                            btnSubmitServiceRequest.setText("Update Request");
                            Toast.makeText(
                                    this,
                                    "This request is no longer pending and cannot be edited",
                                    Toast.LENGTH_LONG
                            ).show();
                            finish();
                            return;
                        }

                        firestore.collection("serviceRequests")
                                .document(requestId)
                                .update(request)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(
                                            this,
                                            "Request updated successfully",
                                            Toast.LENGTH_LONG
                                    ).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> showSaveError(e, true));
                    })
                    .addOnFailureListener(e -> showSaveError(e, true));

        } else {
            request.put("userId", user.getUid());
            request.put("userEmail", user.getEmail() == null ? "" : user.getEmail());
            request.put("status", "Pending");
            request.put("createdAt", FieldValue.serverTimestamp());

            firestore.collection("serviceRequests")
                    .add(request)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(
                                this,
                                "Request submitted successfully",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    })
                    .addOnFailureListener(e -> showSaveError(e, false));
        }
    }

    private void showSaveError(Exception e, boolean editing) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        btnSubmitServiceRequest.setEnabled(true);
        btnSubmitServiceRequest.setText(editing ? "Update Request" : "Submit Request");

        Toast.makeText(
                this,
                (editing ? "Update failed: " : "Submission failed: ")
                        + e.getMessage(),
                Toast.LENGTH_LONG
        ).show();
    }
}