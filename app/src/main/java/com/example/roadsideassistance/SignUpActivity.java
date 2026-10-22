package com.example.roadsideassistance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    EditText name, email, contact, carname, createpassword, confirmpassword;
    Button btnSignup;
    TextView loginsignup;
    FirebaseAuth auth;
    FirebaseFirestore db;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        name = findViewById(R.id.name_signup);
        email = findViewById(R.id.email_signup);
        contact = findViewById(R.id.contact_signup);
        carname = findViewById(R.id.carname_signup);
        createpassword = findViewById(R.id.createpass_signup);
        confirmpassword = findViewById(R.id.confirmpass_signup);
        btnSignup = findViewById(R.id.btnSignup);
        loginsignup = findViewById(R.id.loginsignup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sp = getSharedPreferences(ConstantSp.PREF, MODE_PRIVATE);

        btnSignup.setOnClickListener(v -> registerUser());

        loginsignup.setOnClickListener(v ->
                startActivity(new Intent(SignUpActivity.this, MainActivity.class)));
    }

    private void registerUser() {
        String userName = name.getText().toString().trim();
        String userEmail = email.getText().toString().trim();
        String userContact = contact.getText().toString().trim();
        String userCar = carname.getText().toString().trim();
        String password = createpassword.getText().toString();
        String confirmPassword = confirmpassword.getText().toString();

        if (TextUtils.isEmpty(userName) ||
                TextUtils.isEmpty(userEmail) ||
                TextUtils.isEmpty(userContact) ||
                TextUtils.isEmpty(userCar) ||
                TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            email.setError("Enter a valid email");
            return;
        }

        if (password.length() < 6) {
            createpassword.setError("Password must contain at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmpassword.setError("Passwords do not match");
            return;
        }

        btnSignup.setEnabled(false);

        auth.createUserWithEmailAndPassword(userEmail, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        btnSignup.setEnabled(true);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (auth.getCurrentUser() == null) {
                        btnSignup.setEnabled(true);
                        Toast.makeText(this, "Account created. Please log in.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = auth.getCurrentUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", userName);
                    user.put("email", userEmail);
                    user.put("contact", userContact);
                    user.put("carname", userCar);

                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(unused -> {
                                sp.edit()
                                        .putString(ConstantSp.USERID, uid)
                                        .putString(ConstantSp.NAME, userName)
                                        .putString(ConstantSp.EMAIL, userEmail)
                                        .putString(ConstantSp.CONTACT, userContact)
                                        .putString(ConstantSp.CARNAME, userCar)
                                        .apply();

                                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSignup.setEnabled(true);
                                Toast.makeText(this, "Account created, but profile save failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                });
    }
}
