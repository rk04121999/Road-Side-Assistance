
package com.example.roadsideassistance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Profile_Activity extends AppCompatActivity {

    EditText name, email, contact, carname, createpassword, confirmpassword;
    Button btnEditProfile, profileUpdate, btnDeleteProfile, btnLogoutProfile, btnChangePassword;
    Button btnAddVehicle;
    TextView txtProfileName, tvNoVehicles;
    ImageView imgProfile;
    LinearLayout vehicleListContainer;

    FirebaseAuth auth;
    FirebaseFirestore db;
    SharedPreferences sp;
    String uid;

    private ActivityResultLauncher<String> imagePicker;
    private File profileImageFile;

    private final String[] vehicleTypes = {
            "Car", "Bike", "Scooter", "Truck", "Other"
    };

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
        btnAddVehicle = findViewById(R.id.btnAddVehicle);

        txtProfileName = findViewById(R.id.txtProfileName);
        imgProfile = findViewById(R.id.imgProfile);
        tvNoVehicles = findViewById(R.id.tvNoVehicles);
        vehicleListContainer = findViewById(R.id.vehicleListContainer);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sp = getSharedPreferences(ConstantSp.PREF, MODE_PRIVATE);

        profileImageFile = new File(getFilesDir(), "profile_picture.jpg");

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveProfilePicture(uri);
                    }
                }
        );

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
        loadProfilePicture();
        loadVehicles();

        imgProfile.setOnClickListener(v -> showProfilePictureOptions());

        btnEditProfile.setOnClickListener(v -> setEditMode(true));
        profileUpdate.setOnClickListener(v -> updateProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogoutProfile.setOnClickListener(v -> confirmLogout());
        btnDeleteProfile.setOnClickListener(v -> confirmDelete());
        btnAddVehicle.setOnClickListener(v -> showVehicleDialog(null));
    }

    private void showProfilePictureOptions() {
        String[] options;

        if (profileImageFile.exists()) {
            options = new String[]{"Change Photo", "Delete Photo", "Cancel"};
        } else {
            options = new String[]{"Upload Photo", "Cancel"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (profileImageFile.exists()) {
                        if (which == 0) {
                            imagePicker.launch("image/*");
                        } else if (which == 1) {
                            deleteProfilePicture();
                        }
                    } else if (which == 0) {
                        imagePicker.launch("image/*");
                    }
                })
                .show();
    }

    private void saveProfilePicture(Uri imageUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
             FileOutputStream outputStream = new FileOutputStream(profileImageFile)) {

            if (inputStream == null) {
                Toast.makeText(this, "Unable to open selected image",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[8192];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            loadProfilePicture();

            Toast.makeText(this, "Profile picture updated",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Could not save picture: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadProfilePicture() {
        imgProfile.setImageTintList(null);

        if (!profileImageFile.exists()) {
            imgProfile.setImageResource(R.drawable.ic_profile);
            imgProfile.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imgProfile.setPadding(dp(18), dp(18), dp(18), dp(18));
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(profileImageFile.getAbsolutePath());

        if (bitmap == null) {
            imgProfile.setImageResource(R.drawable.ic_profile);
            return;
        }

        Bitmap circularBitmap = createCircularBitmap(bitmap);
        imgProfile.setImageBitmap(circularBitmap);
        imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgProfile.setPadding(0, 0, 0, 0);
    }

    private Bitmap createCircularBitmap(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        float left = (source.getWidth() - size) / 2f;
        float top = (source.getHeight() - size) / 2f;

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        BitmapShader shader = new BitmapShader(
                source,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
        );

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(shader);

        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setTranslate(-left, -top);
        shader.setLocalMatrix(matrix);

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        if (source != output && !source.isRecycled()) {
            source.recycle();
        }

        return output;
    }

    private void deleteProfilePicture() {
        if (profileImageFile.exists() && profileImageFile.delete()) {
            imgProfile.setImageResource(R.drawable.ic_profile);
            imgProfile.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imgProfile.setPadding(dp(18), dp(18), dp(18), dp(18));

            Toast.makeText(this, "Profile picture deleted",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not delete profile picture",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
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

                    txtProfileName.setText(
                            displayName.isEmpty()
                                    ? "Welcome"
                                    : "Welcome " + displayName
                    );

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

        confirmpassword.setHint(
                enabled ? "Enter Current Password" : "Confirm Password"
        );
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

    private void loadVehicles() {
        db.collection("users")
                .document(uid)
                .collection("vehicles")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    vehicleListContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        tvNoVehicles.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvNoVehicles.setVisibility(View.GONE);

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        addVehicleCard(document);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load vehicles: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    private void showVehicleDialog(DocumentSnapshot vehicle) {
        boolean isEditing = vehicle != null;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = dp(20);
        layout.setPadding(padding, padding, padding, dp(8));

        EditText modelInput = new EditText(this);
        modelInput.setHint("Vehicle Model (e.g. Honda Activa)");
        modelInput.setSingleLine(true);
        modelInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(modelInput);

        EditText numberInput = new EditText(this);
        numberInput.setHint("Registration Number (e.g. GJ01AB1234)");
        numberInput.setSingleLine(true);
        numberInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        layout.addView(numberInput);

        Spinner typeSpinner = new Spinner(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                vehicleTypes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        typeSpinner.setAdapter(adapter);

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );

        spinnerParams.topMargin = dp(8);
        layout.addView(typeSpinner, spinnerParams);

        if (isEditing) {
            modelInput.setText(vehicle.getString("model"));
            numberInput.setText(vehicle.getString("number"));

            String savedType = vehicle.getString("type");

            for (int i = 0; i < vehicleTypes.length; i++) {
                if (vehicleTypes[i].equals(savedType)) {
                    typeSpinner.setSelection(i);
                    break;
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEditing ? "Edit Vehicle" : "Add Vehicle")
                .setView(layout)
                .setPositiveButton(isEditing ? "Update" : "Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

//            Toast.makeText(this, "Save button clicked", Toast.LENGTH_SHORT).show();

            String model = modelInput.getText().toString().trim();
            String number = numberInput.getText().toString()
                    .trim().toUpperCase(Locale.ROOT);
            String type = typeSpinner.getSelectedItem().toString();

            if (model.isEmpty()) {
                modelInput.setError("Enter vehicle model");
                modelInput.requestFocus();
                return;
            }

            if (number.isEmpty()) {
                numberInput.setError("Enter registration number");
                numberInput.requestFocus();
                return;
            }

            saveVehicle(
                    isEditing ? vehicle.getId() : null,
                    model,
                    number,
                    type,
                    dialog
            );
        });
    }



    private void saveVehicle(
            String vehicleId,
            String model,
            String number,
            String type,
            AlertDialog dialog
    ) {
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> vehicleData = new HashMap<>();
        vehicleData.put("model", model);
        vehicleData.put("number", number);
        vehicleData.put("type", type);

        if (vehicleId == null) {
            db.collection("users")
                    .document(uid)
                    .collection("vehicles")
                    .add(vehicleData)
                    .addOnSuccessListener(documentReference -> {
                        dialog.dismiss();

                        Toast.makeText(this,
                                "Vehicle added successfully",
                                Toast.LENGTH_SHORT).show();

                        loadVehicles();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Could not add vehicle: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            db.collection("users")
                    .document(uid)
                    .collection("vehicles")
                    .document(vehicleId)
                    .update(vehicleData)
                    .addOnSuccessListener(unused -> {
                        dialog.dismiss();

                        Toast.makeText(this,
                                "Vehicle updated successfully",
                                Toast.LENGTH_SHORT).show();

                        loadVehicles();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Could not update vehicle: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }


    private void addVehicleCard(DocumentSnapshot document) {
        String model = document.getString("model");
        String number = document.getString("number");
        String type = document.getString("type");

        CardView card = new CardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(resolveThemeColor(
                com.google.android.material.R.attr.colorSurface
        ));

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView modelText = new TextView(this);
        modelText.setText(model == null ? "Vehicle" : model);
        modelText.setTextSize(17);
        modelText.setTypeface(null, android.graphics.Typeface.BOLD);
        modelText.setTextColor(resolveThemeColor(
                com.google.android.material.R.attr.colorOnSurface
        ));
        cardContent.addView(modelText);

        TextView numberText = new TextView(this);
        numberText.setText("Registration: " + (number == null ? "" : number));
        numberText.setTextSize(14);
        numberText.setTextColor(resolveThemeColor(
                com.google.android.material.R.attr.colorOnSurface
        ));

        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        numberParams.topMargin = dp(6);
        cardContent.addView(numberText, numberParams);

        TextView typeText = new TextView(this);
        typeText.setText("Type: " + (type == null ? "" : type));
        typeText.setTextSize(14);
        typeText.setTextColor(resolveThemeColor(
                com.google.android.material.R.attr.colorOnSurface
        ));

        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        typeParams.topMargin = dp(3);
        cardContent.addView(typeText, typeParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);

        Button editButton = new Button(this);
        editButton.setText("EDIT");
        editButton.setTextSize(12);
        editButton.setOnClickListener(v -> showVehicleDialog(document));

        Button deleteButton = new Button(this);
        deleteButton.setText("DELETE");
        deleteButton.setTextSize(12);
        deleteButton.setOnClickListener(v -> confirmDeleteVehicle(document));

        buttons.addView(editButton);
        buttons.addView(deleteButton);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dp(5);
        cardContent.addView(buttons, buttonParams);

        card.addView(cardContent);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(8);

        vehicleListContainer.addView(card, cardParams);
    }

    private void confirmDeleteVehicle(DocumentSnapshot vehicle) {
        String model = vehicle.getString("model");

        new AlertDialog.Builder(this)
                .setTitle("Delete Vehicle")
                .setMessage("Are you sure you want to delete "
                        + (model == null ? "this vehicle" : model) + "?")
                .setPositiveButton("Delete", (dialog, which) ->
                        db.collection("users")
                                .document(uid)
                                .collection("vehicles")
                                .document(vehicle.getId())
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    loadVehicles();

                                    Toast.makeText(this,
                                            "Vehicle deleted successfully",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Could not delete vehicle: "
                                                        + e.getMessage(),
                                                Toast.LENGTH_LONG).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int resolveThemeColor(int attribute) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attribute, typedValue, true);
        return typedValue.data;
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = dp(20);
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

    private void changePassword(
            String oldPass,
            String newPass,
            AlertDialog dialog
    ) {
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
                                                "Password update failed: "
                                                        + e.getMessage(),
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

        int padding = dp(20);
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
                .addOnSuccessListener(unused -> deleteUserVehicles(currentUser))
                .addOnFailureListener(e -> {
                    btnDeleteProfile.setEnabled(true);

                    Toast.makeText(this,
                            "Password verification failed. Account was not deleted.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void deleteUserVehicles(FirebaseUser currentUser) {
        db.collection("users")
                .document(uid)
                .collection("vehicles")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        batch.delete(document.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused ->
                                    deleteUserDocument(currentUser))
                            .addOnFailureListener(e -> {
                                btnDeleteProfile.setEnabled(true);

                                Toast.makeText(this,
                                        "Could not remove saved vehicles: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnDeleteProfile.setEnabled(true);

                    Toast.makeText(this,
                            "Could not load saved vehicles: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void deleteUserDocument(FirebaseUser currentUser) {
        db.collection("users").document(uid).delete()
                .addOnSuccessListener(unused ->
                        currentUser.delete()
                                .addOnSuccessListener(result -> {
                                    if (profileImageFile.exists()) {
                                        profileImageFile.delete();
                                    }

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
