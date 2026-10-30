package com.example.roadsideassistance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Profile_Activity extends AppCompatActivity {

    EditText name, email, contact, carname, createpassword, confirmpassword;
    Button btnEditProfile, profileUpdate, btnDeleteProfile, btnLogoutProfile, btnChangePassword;
    TextView txtProfileName;
    FirebaseAuth auth;
    FirebaseFirestore db;
    SharedPreferences sp;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        name = findViewById(R.id.name_profile);
        email = findViewById(R.id.email_profile);
        contact = findViewById(R.id.contact_profile);
        carname = findViewById(R.id.carname_profile);
        createpassword = findViewById(R.id.createpass_profile);
        confirmpassword = findViewById(R.id.confirmpass_profile);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        profileUpdate = findViewById(R.id.profile_update);
        btnDeleteProfile = findViewById(R.id.btnDeleteProfile);
        btnLogoutProfile = findViewById(R.id.btnlogoutProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        txtProfileName = findViewById(R.id.txtProfileName);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sp = getSharedPreferences(ConstantSp.PREF, MODE_PRIVATE);

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        uid = currentUser.getUid();

        createpassword.setVisibility(View.GONE);
        confirmpassword.setVisibility(View.GONE);

        txtProfileName.setText("Welcome");

        setEditMode(false);
        loadProfile();

        btnEditProfile.setOnClickListener(v -> setEditMode(true));
        profileUpdate.setOnClickListener(v -> updateProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogoutProfile.setOnClickListener(v -> confirmLogout());
        btnDeleteProfile.setOnClickListener(v -> confirmDelete());
    }

    private void loadProfile() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Profile not found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userName = document.getString("name");
                    String userEmail = document.getString("email");
                    String userContact = document.getString("contact");
                    String userCar = document.getString("carname");

                    name.setText(userName == null ? "" : userName);
                    email.setText(userEmail == null ? "" : userEmail);
                    contact.setText(userContact == null ? "" : userContact);
                    carname.setText(userCar == null ? "" : userCar);

                    String displayName = userName == null ? "" : userName.trim();

                    if (displayName.isEmpty()) {
                        txtProfileName.setText("Welcome");
                    } else {
                        txtProfileName.setText("Welcome " + displayName);
                    }

                    sp.edit()
                            .putString(ConstantSp.USERID, uid)
                            .putString(ConstantSp.NAME, userName)
                            .putString(ConstantSp.EMAIL, userEmail)
                            .putString(ConstantSp.CONTACT, userContact)
                            .putString(ConstantSp.CARNAME, userCar)
                            .apply();

                    setEditMode(false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void setEditMode(boolean enabled) {
        name.setEnabled(enabled);
        contact.setEnabled(enabled);
        carname.setEnabled(enabled);
        email.setEnabled(false);

        createpassword.setVisibility(View.GONE);
        confirmpassword.setVisibility(enabled ? View.VISIBLE : View.GONE);

        profileUpdate.setText("Update Profile");
        profileUpdate.setVisibility(enabled ? View.VISIBLE : View.GONE);
        btnEditProfile.setVisibility(enabled ? View.GONE : View.VISIBLE);
        btnChangePassword.setVisibility(enabled ? View.GONE : View.VISIBLE);

        confirmpassword.setText("");
        confirmpassword.setError(null);

        if (enabled) {
            confirmpassword.setHint("Enter Current Password");
        } else {
            confirmpassword.setHint("Confirm Password");
        }
    }

    private void updateProfile() {
        String userName = name.getText().toString().trim();
        String userContact = contact.getText().toString().trim();
        String userCar = carname.getText().toString().trim();
        String password = confirmpassword.getText().toString();

        if (TextUtils.isEmpty(userName)
                || TextUtils.isEmpty(userContact)
                || TextUtils.isEmpty(userCar)) {
            Toast.makeText(this, "Please fill all editable fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!userContact.matches("[0-9]{10}")) {
            contact.setError("Enter a valid 10-digit contact number");
            contact.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            confirmpassword.setError("Enter your current password");
            confirmpassword.requestFocus();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) {
            goToLogin();
            return;
        }

        profileUpdate.setEnabled(false);

        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(), password
        );

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        saveProfile(userName, userContact, userCar))
                .addOnFailureListener(e -> {
                    profileUpdate.setEnabled(true);
                    confirmpassword.setError("Incorrect password");
                    confirmpassword.requestFocus();

                    Toast.makeText(this,
                            "Password verification failed. Check your current password.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfile(String userName, String userContact, String userCar) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", userName);
        updates.put("contact", userContact);
        updates.put("carname", userCar);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    sp.edit()
                            .putString(ConstantSp.NAME, userName)
                            .putString(ConstantSp.CONTACT, userContact)
                            .putString(ConstantSp.CARNAME, userCar)
                            .apply();

                    txtProfileName.setText("Welcome " + userName.trim());

                    profileUpdate.setEnabled(true);
                    setEditMode(false);

                    Toast.makeText(this, "Profile updated successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    profileUpdate.setEnabled(true);
                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText currentPassword = new EditText(this);
        currentPassword.setHint("Current Password");
        currentPassword.setInputType(129);
        layout.addView(currentPassword);

        EditText newPassword = new EditText(this);
        newPassword.setHint("New Password");
        newPassword.setInputType(129);
        layout.addView(newPassword);

        EditText confirmPassword = new EditText(this);
        confirmPassword.setHint("Confirm New Password");
        confirmPassword.setInputType(129);
        layout.addView(confirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            String oldPass = currentPassword.getText().toString();
                            String newPass = newPassword.getText().toString();
                            String confirmPass = confirmPassword.getText().toString();

                            if (TextUtils.isEmpty(oldPass)) {
                                currentPassword.setError("Enter current password");
                                return;
                            }

                            if (newPass.length() < 6) {
                                newPassword.setError("Use at least 6 characters");
                                return;
                            }

                            if (!newPass.equals(confirmPass)) {
                                confirmPassword.setError("Passwords do not match");
                                return;
                            }

                            if (oldPass.equals(newPass)) {
                                newPassword.setError("Choose a different password");
                                return;
                            }

                            changePassword(oldPass, newPass, dialog);
                        }));

        dialog.show();
    }

    private void changePassword(String oldPass, String newPass, AlertDialog dialog) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) {
            goToLogin();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(), oldPass
        );

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        currentUser.updatePassword(newPass)
                                .addOnSuccessListener(result -> {
                                    dialog.dismiss();
                                    Toast.makeText(this,
                                            "Password changed successfully",
                                            Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Password update failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Current password is incorrect",
                                Toast.LENGTH_LONG).show());
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    auth.signOut();
                    sp.edit().clear().apply();
                    goToLogin();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void confirmDelete() {
        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter current password");
        passwordInput.setInputType(129);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        passwordInput.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Enter your current password to permanently delete your account.")
                .setView(passwordInput)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String password = passwordInput.getText().toString();

                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(this, "Password is required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    deleteAccount(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount(String password) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) {
            goToLogin();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(), password
        );

        btnDeleteProfile.setEnabled(false);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        db.collection("users").document(uid).delete()
                                .addOnSuccessListener(deleted ->
                                        currentUser.delete()
                                                .addOnSuccessListener(result -> {
                                                    sp.edit().clear().apply();

                                                    Toast.makeText(this,
                                                            "Account deleted successfully",
                                                            Toast.LENGTH_SHORT).show();

                                                    goToLogin();
                                                })
                                                .addOnFailureListener(e -> {
                                                    btnDeleteProfile.setEnabled(true);

                                                    Toast.makeText(this,
                                                            "Profile deleted, but account deletion failed. Sign in again and retry.",
                                                            Toast.LENGTH_LONG).show();
                                                }))
                                .addOnFailureListener(e -> {
                                    btnDeleteProfile.setEnabled(true);

                                    Toast.makeText(this,
                                            "Could not delete profile: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }))
                .addOnFailureListener(e -> {
                    btnDeleteProfile.setEnabled(true);

                    Toast.makeText(this,
                            "Password verification failed. Account was not deleted.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(Profile_Activity.this, MainActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }
}