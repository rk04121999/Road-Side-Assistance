
package com.example.roadsideassistance;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Feedback_Activity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser user;
    private String requestId;
    private String technicianEmail;
    private String technicianName;
    private TextView tvFeedbackTechnician;
    private RatingBar ratingTechnician;
    private RatingBar ratingService;
    private EditText etTechnicianFeedback;
    private EditText etServiceFeedback;
    private Button btnSubmitFeedback;
    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        requestId = getIntent().getStringExtra("requestId");

        tvFeedbackTechnician = findViewById(R.id.tvFeedbackTechnician);
        ratingTechnician = findViewById(R.id.ratingTechnician);
        ratingService = findViewById(R.id.ratingService);
        etTechnicianFeedback = findViewById(R.id.etTechnicianFeedback);
        etServiceFeedback = findViewById(R.id.etServiceFeedback);
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);

        ratingTechnician.setStepSize(1);
        ratingService.setStepSize(1);

        if (user == null || requestId == null || requestId.trim().isEmpty()) {
            Toast.makeText(this, "Unable to open feedback form", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnSubmitFeedback.setEnabled(false);
        loadRequest();

        btnSubmitFeedback.setOnClickListener(v -> submitFeedback());
    }

    private void loadRequest() {
        db.collection("serviceRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()
                            || !user.getUid().equals(document.getString("userId"))
                            || !"Completed".equalsIgnoreCase(document.getString("status"))) {
                        Toast.makeText(this, "Feedback is available after your service is completed",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    technicianEmail = document.getString("technicianEmail");
                    technicianName = document.getString("technicianName");

                    if (technicianEmail == null || technicianEmail.trim().isEmpty()) {
                        Toast.makeText(this, "Technician details are unavailable",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    tvFeedbackTechnician.setText(
                            "Technician: " + (technicianName == null ? "Assigned Technician" : technicianName)
                    );

                    checkExistingFeedback();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Unable to load request: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void checkExistingFeedback() {
        db.collection("feedback")
                .document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Toast.makeText(this, "You have already submitted feedback for this request",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        btnSubmitFeedback.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Unable to check feedback: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void submitFeedback() {
        if (isSubmitting || user == null) {
            return;
        }

        int technicianRatingValue = Math.round(ratingTechnician.getRating());
        int serviceRatingValue = Math.round(ratingService.getRating());

        String technicianComment = etTechnicianFeedback.getText().toString().trim();
        String serviceComment = etServiceFeedback.getText().toString().trim();

        if (technicianRatingValue < 1 || technicianRatingValue > 5) {
            Toast.makeText(this, "Please rate the technician", Toast.LENGTH_SHORT).show();
            return;
        }

        if (technicianComment.isEmpty()) {
            etTechnicianFeedback.setError("Please write feedback about the technician");
            etTechnicianFeedback.requestFocus();
            return;
        }

        if (serviceRatingValue < 1 || serviceRatingValue > 5) {
            Toast.makeText(this, "Please rate our service", Toast.LENGTH_SHORT).show();
            return;
        }

        isSubmitting = true;
        btnSubmitFeedback.setEnabled(false);

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("requestId", requestId);
        feedback.put("customerId", user.getUid());
        feedback.put("customerEmail", user.getEmail() == null ? "" : user.getEmail());
        feedback.put("technicianEmail", technicianEmail);
        feedback.put("technicianName", technicianName == null ? "" : technicianName);
        feedback.put("technicianRating", technicianRatingValue);
        feedback.put("technicianComment", technicianComment);
        feedback.put("serviceRating", serviceRatingValue);
        feedback.put("serviceComment", serviceComment);
        feedback.put("createdAt", FieldValue.serverTimestamp());

        db.collection("feedback")
                .document(requestId)
                .set(feedback)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Thank you for your feedback!",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    isSubmitting = false;
                    btnSubmitFeedback.setEnabled(true);
                    Toast.makeText(this, "Unable to submit feedback: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
