package com.example.roadsideassistance;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Favorites_Activity extends AppCompatActivity {

    private RecyclerView recyclerFavorites;
    private TextView tvFavoritesMessage;

    private FirebaseFirestore db;
    private ListenerRegistration favoriteListener;
    private MechanicAdapter adapter;

    private final List<Mechanic> favoriteList = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerFavorites = findViewById(R.id.recyclerFavorites);
        tvFavoritesMessage = findViewById(R.id.tvFavoritesMessage);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvFavoritesMessage.setText("Please log in to view favorites.");
            return;
        }

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MechanicAdapter(
                this,
                favoriteList,
                favoriteIds,
                db,
                userId
        );

        recyclerFavorites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {

        tvFavoritesMessage.setText("Loading favorites...");
        tvFavoritesMessage.setVisibility(View.VISIBLE);

        favoriteListener = db.collection("users")
                .document(userId)
                .collection("favoriteMechanics")
                .addSnapshotListener((snapshot, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        tvFavoritesMessage.setText("Unable to load favorite mechanics.");
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    favoriteList.clear();
                    favoriteIds.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        String mechanicId = document.getString("mechanicId");
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

                        favoriteIds.add(document.getId());

                        favoriteList.add(new Mechanic(
                                mechanicId == null ? document.getId() : mechanicId,
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

                    if (favoriteList.isEmpty()) {
                        tvFavoritesMessage.setText("You have not added any favorite mechanics yet.");
                    } else {
                        tvFavoritesMessage.setText(
                                favoriteList.size() + " favorite mechanics"
                        );
                    }

                    tvFavoritesMessage.setVisibility(View.VISIBLE);
                });
    }

    @Override
    protected void onDestroy() {

        if (favoriteListener != null) {
            favoriteListener.remove();
        }

        super.onDestroy();
    }
}