package com.example.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    RecyclerView rvGarageServices;
    RecyclerView rvAvailableMechanics;

    TextView tvHomeMechanicsMessage;

    private FirebaseFirestore db;
    private ListenerRegistration mechanicListener;
    private ListenerRegistration favoriteListener;

    private MechanicAdapter mechanicAdapter;

    private final List<Mechanic> mechanicList = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        Button btnAvailableMechanics = findViewById(R.id.btnAvailableMechanics);

        btnAvailableMechanics.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, Mechanics_Activity.class);
            startActivity(intent);
        });

        rvGarageServices = findViewById(R.id.rvGarageServices);

        List<GarageServiceAdapter.GarageService> garageList = new ArrayList<>();

        garageList.add(new GarageServiceAdapter.GarageService(
                "Rk Tow Service",
                "Mirzapur, Ahmedabad.",
                "7687877889",
                "Replace with your real towing service address"
        ));

        garageList.add(new GarageServiceAdapter.GarageService(
                "Towing Expert",
                "C.G Road, Ahmedabad.",
                "7687987989",
                "Replace with your real towing service address"
        ));

        rvGarageServices.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        rvGarageServices.setAdapter(new GarageServiceAdapter(this, garageList));


        rvAvailableMechanics = findViewById(R.id.rvAvailableMechanics);
        tvHomeMechanicsMessage = findViewById(R.id.tvHomeMechanicsMessage);

        rvGarageServices.setNestedScrollingEnabled(false);
        rvAvailableMechanics.setNestedScrollingEnabled(false);

        rvAvailableMechanics.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        mechanicAdapter = new MechanicAdapter(
                this,
                mechanicList,
                favoriteIds,
                db,
                userId
        );

        rvAvailableMechanics.setAdapter(mechanicAdapter);

        loadHomeMechanics();
        loadHomeFavoriteIds();

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_services) {
                startActivity(new Intent(HomeActivity.this, ServicesActivity.class));
                return true;
            } else if (id == R.id.nav_requests) {
                Intent intent = new Intent(HomeActivity.this, My_Requests_Activity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, Profile_Activity.class));
                return true;
            }

            return false;
        });
    }

    private void loadHomeMechanics() {

        if (userId == null) {
            mechanicList.clear();
            mechanicAdapter.notifyDataSetChanged();
            tvHomeMechanicsMessage.setText("Please log in to view available mechanics.");
            return;
        }

        tvHomeMechanicsMessage.setText("Loading available mechanics...");

        mechanicListener = db.collection("mechanics")
                .whereEqualTo("available", true)
                .addSnapshotListener((snapshot, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        mechanicList.clear();
                        mechanicAdapter.notifyDataSetChanged();
                        tvHomeMechanicsMessage.setText("Unable to load mechanics. Please try again.");
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    mechanicList.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        String name = document.getString("name");
                        String phone = document.getString("phone");
                        String specialization = document.getString("specialization");
                        String experience = document.getString("experience");
                        String vehicle = document.getString("vehicle");

                        Object ratingValue = document.get("rating");

                        double rating = ratingValue instanceof Number
                                ? ((Number) ratingValue).doubleValue()
                                : 0.0;

                        Boolean available = document.getBoolean("available");

                        if (name == null || name.trim().isEmpty()) {
                            continue;
                        }

                        mechanicList.add(new Mechanic(
                                document.getId(),
                                name,
                                phone == null ? "" : phone,
                                specialization == null ? "General Mechanic" : specialization,
                                experience == null ? "Not specified" : experience,
                                vehicle == null ? "Not specified" : vehicle,
                                rating,
                                available != null && available
                        ));
                    }

                    mechanicAdapter.notifyDataSetChanged();

                    if (mechanicList.isEmpty()) {
                        tvHomeMechanicsMessage.setText("No available mechanics found.");
                    } else {
                        tvHomeMechanicsMessage.setText(
                                mechanicList.size() + " available mechanics"
                        );
                    }
                });
    }

    private void loadHomeFavoriteIds() {

        if (userId == null) {
            return;
        }

        favoriteListener = db.collection("users")
                .document(userId)
                .collection("favoriteMechanics")
                .addSnapshotListener((snapshot, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null || snapshot == null) {
                        return;
                    }

                    favoriteIds.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        favoriteIds.add(document.getId());
                    }

                    mechanicAdapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {

        if (mechanicListener != null) {
            mechanicListener.remove();
        }

        if (favoriteListener != null) {
            favoriteListener.remove();
        }

        super.onDestroy();
    }
}
