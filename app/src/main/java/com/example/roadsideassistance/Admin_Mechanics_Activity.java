
package com.example.roadsideassistance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Admin_Mechanics_Activity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistrationHolder listenerHolder;
    private RecyclerView recyclerAdminMechanics;
    private TextView tvAdminMechanicsMessage;
    private final List<DocumentSnapshot> mechanics = new ArrayList<>();
    private MechanicAdapter adapter;

    private static class ListenerRegistrationHolder {
        com.google.firebase.firestore.ListenerRegistration registration;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_mechanics);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || user.getEmail() == null
                || !user.getEmail().equalsIgnoreCase("admin@rsa.com")) {
            Toast.makeText(this, "Authorized admin account required",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        listenerHolder = new ListenerRegistrationHolder();

        recyclerAdminMechanics = findViewById(R.id.recyclerAdminMechanics);
        tvAdminMechanicsMessage = findViewById(R.id.tvAdminMechanicsMessage);

        recyclerAdminMechanics.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MechanicAdapter();
        recyclerAdminMechanics.setAdapter(adapter);

        findViewById(R.id.btnAddMechanic)
                .setOnClickListener(v -> showMechanicDialog(null));

        tvAdminMechanicsMessage.setText("Loading mechanics...");

        user.getIdToken(true)
                .addOnSuccessListener(token -> loadMechanics())
                .addOnFailureListener(e ->
                        showMessage("Unable to verify admin account: " + e.getMessage()));
    }

    private void loadMechanics() {
        listenerHolder.registration = db.collection("mechanics")
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        tvAdminMechanicsMessage.setText("Unable to load mechanics.");
                        showMessage(error.getMessage());
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    mechanics.clear();
                    mechanics.addAll(snapshot.getDocuments());
                    adapter.notifyDataSetChanged();

                    tvAdminMechanicsMessage.setText(mechanics.isEmpty()
                            ? "No mechanics added yet."
                            : mechanics.size() + " mechanics registered");
                });
    }

    private void showMechanicDialog(DocumentSnapshot document) {
        boolean editing = document != null;

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(40, 8, 40, 0);

        EditText name = createField(form, "Mechanic full name",
                InputType.TYPE_CLASS_TEXT);

        EditText email = createField(form, "Mechanic login email",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        EditText password = null;

        if (!editing) {
            password = createField(form, "Login password (minimum 6 characters)",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            email.setEnabled(false);
        }

        EditText phone = createField(form, "Phone number", InputType.TYPE_CLASS_PHONE);
        EditText specialization = createField(form, "Specialization",
                InputType.TYPE_CLASS_TEXT);
        EditText experience = createField(form, "Experience (example: 5 Years)",
                InputType.TYPE_CLASS_TEXT);
        EditText vehicle = createField(form, "Vehicle type (Car, Bike, Both)",
                InputType.TYPE_CLASS_TEXT);
        EditText rating = createField(form, "Rating (0 to 5)",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        CheckBox available = new CheckBox(this);
        available.setText("Mechanic is available");
        available.setTextSize(15);
        available.setChecked(true);
        form.addView(available);

        if (editing) {
            name.setText(getValue(document, "name"));
            email.setText(getValue(document, "email"));
            phone.setText(getValue(document, "phone"));
            specialization.setText(getValue(document, "specialization"));
            experience.setText(getValue(document, "experience"));
            vehicle.setText(getValue(document, "vehicle"));

            Object ratingValue = document.get("rating");
            if (ratingValue instanceof Number) {
                rating.setText(String.valueOf(((Number) ratingValue).doubleValue()));
            }

            Boolean isAvailable = document.getBoolean("available");
            available.setChecked(isAvailable != null && isAvailable);
        }

        EditText finalPassword = password;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Edit Mechanic" : "Register New Mechanic")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(editing ? "Update" : "Create Account", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                    String mechanicName = name.getText().toString().trim();
                    String mechanicEmail = email.getText().toString()
                            .trim().toLowerCase(Locale.ROOT);
                    String mechanicPhone = phone.getText().toString().trim();
                    String mechanicSpecialization = specialization.getText().toString().trim();
                    String mechanicExperience = experience.getText().toString().trim();
                    String mechanicVehicle = vehicle.getText().toString().trim();
                    String ratingText = rating.getText().toString().trim();

                    String mechanicPassword = editing || finalPassword == null
                            ? "" : finalPassword.getText().toString();

                    if (mechanicName.isEmpty() || mechanicEmail.isEmpty()
                            || mechanicPhone.isEmpty() || mechanicSpecialization.isEmpty()
                            || mechanicExperience.isEmpty() || mechanicVehicle.isEmpty()
                            || ratingText.isEmpty()) {
                        showMessage("Please fill in all mechanic details.");
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mechanicEmail).matches()) {
                        showMessage("Enter a valid email address.");
                        return;
                    }

                    if (!editing && mechanicPassword.length() < 6) {
                        showMessage("Password must contain at least 6 characters.");
                        return;
                    }

                    if (!mechanicPhone.matches("[0-9]{10}")) {
                        showMessage("Enter a valid 10-digit phone number.");
                        return;
                    }

                    double mechanicRating;

                    try {
                        mechanicRating = Double.parseDouble(ratingText);
                    } catch (NumberFormatException e) {
                        showMessage("Enter a valid rating.");
                        return;
                    }

                    if (mechanicRating < 0 || mechanicRating > 5) {
                        showMessage("Rating must be between 0 and 5.");
                        return;
                    }

                    Map<String, Object> mechanicData = new HashMap<>();
                    mechanicData.put("name", mechanicName);
                    mechanicData.put("email", mechanicEmail);
                    mechanicData.put("phone", mechanicPhone);
                    mechanicData.put("specialization", mechanicSpecialization);
                    mechanicData.put("experience", mechanicExperience);
                    mechanicData.put("vehicle", mechanicVehicle);
                    mechanicData.put("rating", mechanicRating);
                    mechanicData.put("available", available.isChecked());

                    if (editing) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                        db.collection("mechanics")
                                .document(document.getId())
                                .update(mechanicData)
                                .addOnSuccessListener(result -> {
                                    showMessage("Mechanic updated successfully.");
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                            .setEnabled(true);
                                    showMessage("Update failed: " + e.getMessage());
                                });
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                        createMechanicAccount(mechanicEmail, mechanicPassword,
                                mechanicData, dialog);
                    }
                }));

        dialog.show();
    }

    private void createMechanicAccount(String mechanicEmail, String mechanicPassword,
                                       Map<String, Object> mechanicData,
                                       AlertDialog dialog) {

        FirebaseApp secondaryApp = null;

        try {
            for (FirebaseApp app : FirebaseApp.getApps(this)) {
                if (app.getName().equals("MechanicRegistration")) {
                    secondaryApp = app;
                    break;
                }
            }

            if (secondaryApp == null) {
                secondaryApp = FirebaseApp.initializeApp(
                        getApplicationContext(),
                        FirebaseApp.getInstance().getOptions(),
                        "MechanicRegistration"
                );
            }

            if (secondaryApp == null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                showMessage("Unable to initialize mechanic registration.");
                return;
            }

            FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
            secondaryAuth.signOut();

            secondaryAuth.createUserWithEmailAndPassword(
                    mechanicEmail, mechanicPassword
            ).addOnCompleteListener(task -> {
                if (isFinishing() || isDestroyed()) {
                    secondaryAuth.signOut();
                    return;
                }

                if (!task.isSuccessful() || secondaryAuth.getCurrentUser() == null) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);

                    String error = task.getException() == null
                            ? "Account creation failed."
                            : task.getException().getMessage();

                    showMessage(error);
                    secondaryAuth.signOut();
                    return;
                }

                FirebaseUser mechanicUser = secondaryAuth.getCurrentUser();

                if (mechanicUser == null) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    secondaryAuth.signOut();
                    showMessage("Unable to retrieve the new mechanic account.");
                    return;
                }

                mechanicData.put("uid", mechanicUser.getUid());

                db.collection("mechanics")
                        .add(mechanicData)
                        .addOnSuccessListener(reference -> {
                            secondaryAuth.signOut();
                            showMessage("Mechanic account created successfully.");
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            secondaryAuth.signOut();
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            showMessage("Account created, but mechanic details could not be saved. "
                                    + "Contact support before trying to register this email again. "
                                    + e.getMessage());
                        });
            });

        } catch (Exception e) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            showMessage("Account registration error: " + e.getMessage());
        }
    }

    private EditText createField(LinearLayout parent, String hint, int inputType) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setInputType(inputType);
        parent.addView(editText);
        return editText;
    }

    private String getValue(DocumentSnapshot document, String key) {
        String value = document.getString(key);
        return value == null ? "" : value;
    }

    private void deleteMechanic(DocumentSnapshot document) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Mechanic")
                .setMessage("Are you sure you want to delete "
                        + getValue(document, "name") + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) ->
                        db.collection("mechanics")
                                .document(document.getId())
                                .delete()
                                .addOnSuccessListener(result ->
                                        showMessage("Mechanic record deleted. "
                                                + "The Authentication account still exists."))
                                .addOnFailureListener(e ->
                                        showMessage("Delete failed: " + e.getMessage())))
                .show();
    }

    private void showMessage(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private class MechanicAdapter extends RecyclerView.Adapter<MechanicAdapter.MechanicViewHolder> {

        @NonNull
        @Override
        public MechanicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_mechanic, parent, false);
            return new MechanicViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MechanicViewHolder holder, int position) {
            DocumentSnapshot document = mechanics.get(position);

            holder.tvName.setText(getValue(document, "name"));
            holder.tvSpecialization.setText(
                    "Specialization: " + getValue(document, "specialization"));
            holder.tvPhone.setText("Phone: " + getValue(document, "phone"));
            holder.tvDetails.setText(
                    "Experience: " + getValue(document, "experience")
                            + "\nVehicle: " + getValue(document, "vehicle"));

            Object ratingValue = document.get("rating");
            double mechanicRating = ratingValue instanceof Number
                    ? ((Number) ratingValue).doubleValue() : 0.0;

            holder.tvRating.setText("Rating: " + mechanicRating + " / 5");

            Boolean isAvailable = document.getBoolean("available");
            boolean mechanicAvailable = isAvailable != null && isAvailable;

            holder.tvAvailability.setText(
                    mechanicAvailable ? "Available" : "Unavailable");
            holder.tvAvailability.setTextColor(
                    mechanicAvailable ? 0xFF198754 : 0xFFD32F2F);

            holder.btnEdit.setOnClickListener(v -> showMechanicDialog(document));
            holder.btnDelete.setOnClickListener(v -> deleteMechanic(document));
        }

        @Override
        public int getItemCount() {
            return mechanics.size();
        }

        class MechanicViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSpecialization, tvPhone, tvDetails, tvRating, tvAvailability;
            Button btnEdit, btnDelete;

            MechanicViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvAdminMechanicName);
                tvSpecialization = itemView.findViewById(R.id.tvAdminMechanicSpecialization);
                tvPhone = itemView.findViewById(R.id.tvAdminMechanicPhone);
                tvDetails = itemView.findViewById(R.id.tvAdminMechanicDetails);
                tvRating = itemView.findViewById(R.id.tvAdminMechanicRating);
                tvAvailability = itemView.findViewById(R.id.tvAdminMechanicAvailability);
                btnEdit = itemView.findViewById(R.id.btnEditMechanic);
                btnDelete = itemView.findViewById(R.id.btnDeleteMechanic);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (listenerHolder != null && listenerHolder.registration != null) {
            listenerHolder.registration.remove();
        }
        super.onDestroy();
    }
}
