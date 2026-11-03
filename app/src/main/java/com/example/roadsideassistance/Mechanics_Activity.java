package com.example.roadsideassistance;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class Mechanics_Activity extends AppCompatActivity {

    private RecyclerView recyclerMechanics;
    private TextView tvMechanicsMessage;

    private FirebaseFirestore db;
    private ListenerRegistration mechanicListener;
    private MechanicAdapter adapter;

    private final List<Mechanic> mechanicList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanics);

        recyclerMechanics = findViewById(R.id.recyclerMechanics);
        tvMechanicsMessage = findViewById(R.id.tvMechanicsMessage);

        db = FirebaseFirestore.getInstance();

        recyclerMechanics.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new MechanicAdapter(this, mechanicList);
        recyclerMechanics.setAdapter(adapter);

        loadMechanics();
    }

    private void loadMechanics() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showStaticMechanics("Please log in to view mechanics.");
            return;
        }

        tvMechanicsMessage.setText("Loading available mechanics...");
        tvMechanicsMessage.setVisibility(View.VISIBLE);

        mechanicListener = db.collection("mechanics")
                .whereEqualTo("available", true)
                .addSnapshotListener((snapshot, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        showStaticMechanics(
                                "Firebase unavailable. Showing demo mechanics."
                        );
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

                    adapter.notifyDataSetChanged();

                    if (mechanicList.isEmpty()) {
                        showStaticMechanics(
                                "No mechanics found in Firebase. Showing demo mechanics."
                        );
                    } else {
                        tvMechanicsMessage.setText(
                                mechanicList.size() + " available mechanics found"
                        );
                        tvMechanicsMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showStaticMechanics(String message) {

        mechanicList.clear();

        mechanicList.add(new Mechanic(
                "demo1",
                "Demo Mechanic 1",
                "",
                "Car Repair",
                "5 years",
                "Mobile Service Van",
                4.8,
                true
        ));

        mechanicList.add(new Mechanic(
                "demo2",
                "Demo Mechanic 2",
                "",
                "Battery & Electrical",
                "4 years",
                "Service Bike",
                4.5,
                true
        ));

        mechanicList.add(new Mechanic(
                "demo3",
                "Demo Mechanic 3",
                "",
                "Puncture & Tyres",
                "3 years",
                "Mobile Service Van",
                4.6,
                true
        ));

        mechanicList.add(new Mechanic(
                "demo4",
                "Demo Mechanic 4",
                "",
                "Towing Service",
                "6 years",
                "Tow Truck",
                4.9,
                true
        ));

        adapter.notifyDataSetChanged();

        tvMechanicsMessage.setText(message);
        tvMechanicsMessage.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {

        if (mechanicListener != null) {
            mechanicListener.remove();
            mechanicListener = null;
        }

        super.onDestroy();
    }
}