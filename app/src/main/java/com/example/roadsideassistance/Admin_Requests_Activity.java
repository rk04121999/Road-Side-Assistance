
package com.example.roadsideassistance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

public class Admin_Requests_Activity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration registration;
    private final List<DocumentSnapshot> requests = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final List<String> rows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

//        if (user == null || user.getEmail() == null
//                || !user.getEmail().equalsIgnoreCase("admin@rsa.com")
//                || !user.isEmailVerified()) {
//            Toast.makeText(this, "Authorized admin account required", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }

        db = FirebaseFirestore.getInstance();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("Manage Service Requests");
        title.setTextSize(23);
        title.setPadding(0, 8, 0, 20);
        root.addView(title);

        ListView listView = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));

        setContentView(root);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < requests.size()) {
                showAssignmentDialog(requests.get(position));
            }
        });

        user.getIdToken(true).addOnSuccessListener(token -> loadRequests());
    }

    private void loadRequests() {
        registration = db.collection("serviceRequests")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        showMessage("Unable to load requests: " + error.getMessage());
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    requests.clear();
                    rows.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        requests.add(document);

                        String status = value(document, "status");
                        String service = value(document, "serviceType");
                        String location = value(document, "location");
                        String technician = value(document, "technicianName");

                        rows.add(
                                "Service: " + service
                                        + "\nStatus: " + status
                                        + "\nLocation: " + location
                                        + "\nTechnician: "
                                        + (technician.isEmpty() ? "Not assigned" : technician)
                                        + "\nRequest ID: " + document.getId()
                        );
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void showAssignmentDialog(DocumentSnapshot request) {
        String status = value(request, "status");

        if (!status.equals("Pending")
                && !status.equals("Assigned")
                && !status.equals("Rejected")) {
            showMessage("This request cannot be assigned in its current status");
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(40, 10, 40, 0);

        EditText name = field(form, "Technician full name", InputType.TYPE_CLASS_TEXT);
        EditText email = field(form, "Technician registered email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText phone = field(form, "Technician phone", InputType.TYPE_CLASS_PHONE);
        EditText vehicle = field(form, "Service vehicle details", InputType.TYPE_CLASS_TEXT);

        name.setText(value(request, "technicianName"));
        email.setText(value(request, "technicianEmail"));
        phone.setText(value(request, "technicianPhone"));
        vehicle.setText(value(request, "technicianVehicle"));

        new AlertDialog.Builder(this)
                .setTitle("Assign Technician")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Assign", (dialog, which) -> {
                    String technicianName = name.getText().toString().trim();
                    String technicianEmail = email.getText().toString().trim()
                            .toLowerCase(Locale.ROOT);
                    String technicianPhone = phone.getText().toString().trim();
                    String technicianVehicle = vehicle.getText().toString().trim();

                    if (technicianName.isEmpty() || technicianEmail.isEmpty()
                            || !android.util.Patterns.EMAIL_ADDRESS.matcher(technicianEmail).matches()
                            || technicianPhone.isEmpty() || technicianVehicle.isEmpty()) {
                        showMessage("Enter valid technician name, email, phone and vehicle");
                        return;
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("technicianName", technicianName);
                    update.put("technicianEmail", technicianEmail);
                    update.put("technicianPhone", technicianPhone);
                    update.put("technicianVehicle", technicianVehicle);
                    update.put("status", "Assigned");
                    update.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("serviceRequests")
                            .document(request.getId())
                            .update(update)
                            .addOnSuccessListener(unused ->
                                    showMessage("Technician assigned successfully"))
                            .addOnFailureListener(e ->
                                    showMessage("Assignment failed: " + e.getMessage()));
                })
                .show();
    }

    private EditText field(LinearLayout parent, String hint, int inputType) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setInputType(inputType);
        parent.addView(editText);
        return editText;
    }

    private String value(DocumentSnapshot document, String key) {
        String value = document.getString(key);
        return value == null ? "" : value;
    }

    private void showMessage(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
