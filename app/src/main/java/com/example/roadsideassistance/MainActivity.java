
package com.example.roadsideassistance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    EditText email, password;
    Button btnLogin;
    TextView signup, forgotPassword;
    FirebaseAuth auth;
    FirebaseFirestore db;
    SharedPreferences sp;

    private static final String ADMIN_EMAIL = "admin@rsa.com";
    private static final String TECHNICIAN_EMAIL = "technician@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        email = findViewById(R.id.email_login);
        password = findViewById(R.id.password_login);
        btnLogin = findViewById(R.id.btnLogin);
        signup = findViewById(R.id.signup_login);
        forgotPassword = findViewById(R.id.forgotPassword);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sp = getSharedPreferences(ConstantSp.PREF, MODE_PRIVATE);

        btnLogin.setOnClickListener(v -> loginUser());

        signup.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SignUpActivity.class)));

        forgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString();

        if (TextUtils.isEmpty(userEmail)) {
            email.setError("Enter your email");
            email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            email.setError("Enter a valid email");
            email.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(userPassword)) {
            password.setError("Enter your password");
            password.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || auth.getCurrentUser() == null) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(this, "Invalid email or password",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(this, "Unable to get login account",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String loginEmail = user.getEmail();

                    if (loginEmail == null) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(this, "Account email not found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (loginEmail.equalsIgnoreCase(ADMIN_EMAIL)) {
                        openRoleActivity(Admin_Requests_Activity.class);
                        return;
                    }

                    if (loginEmail.equalsIgnoreCase(TECHNICIAN_EMAIL)) {
                        openRoleActivity(Technician_Requests_Activity.class);
                        return;
                    }

                    loadCustomerProfile(user);
                });
    }

    private void loadCustomerProfile(FirebaseUser user) {
        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!document.exists()) {
                        btnLogin.setEnabled(true);
                        auth.signOut();
                        Toast.makeText(this,
                                "User profile not found in Firestore",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    sp.edit()
                            .putString(ConstantSp.USERID, uid)
                            .putString(ConstantSp.NAME, document.getString("name"))
                            .putString(ConstantSp.EMAIL, document.getString("email"))
                            .putString(ConstantSp.CONTACT, document.getString("contact"))
                            .putString(ConstantSp.CARNAME, document.getString("carname"))
                            .apply();

                    openRoleActivity(HomeActivity.class);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnLogin.setEnabled(true);

                    Toast.makeText(this,
                            "Could not load profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void openRoleActivity(Class<?> activityClass) {
        Intent intent = new Intent(MainActivity.this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetPassword() {
        String userEmail = email.getText().toString().trim();

        if (TextUtils.isEmpty(userEmail)) {
            email.setError("Enter your registered email first");
            email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            email.setError("Enter a valid email");
            email.requestFocus();
            return;
        }

        forgotPassword.setEnabled(false);

        auth.sendPasswordResetEmail(userEmail)
                .addOnCompleteListener(task -> {
                    forgotPassword.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "If this email is registered, a password reset email will be sent.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Could not send reset email. Check Firebase settings and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
