
package com.example.roadsideassistance;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MechanicAdapter extends RecyclerView.Adapter<MechanicAdapter.MechanicViewHolder> {

    private final Context context;
    private final List<Mechanic> mechanicList;
    private final Set<String> favoriteIds;
    private final FirebaseFirestore db;
    private final String userId;

    public MechanicAdapter(Context context, List<Mechanic> mechanicList,
                           Set<String> favoriteIds, FirebaseFirestore db, String userId) {
        this.context = context;
        this.mechanicList = mechanicList;
        this.favoriteIds = favoriteIds;
        this.db = db;
        this.userId = userId;
    }

    @NonNull
    @Override
    public MechanicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_mechanic, parent, false);

        return new MechanicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MechanicViewHolder holder, int position) {

        Mechanic mechanic = mechanicList.get(position);

        holder.tvMechanicName.setText(mechanic.getName());
        holder.tvSpecialization.setText(mechanic.getSpecialization());
        holder.tvExperience.setText("Experience: " + mechanic.getExperience());
        holder.tvVehicle.setText("Vehicle: " + mechanic.getVehicle());

        holder.tvRating.setText(
                String.format(Locale.getDefault(), "★ %.1f", mechanic.getRating())
        );

        holder.tvAvailability.setText(
                mechanic.isAvailable() ? "Available" : "Unavailable"
        );

        holder.tvAvailability.setTextColor(
                mechanic.isAvailable() ? 0xFF198754 : 0xFFD32F2F
        );

        boolean isFavorite = favoriteIds.contains(mechanic.getId());

        holder.tvFavorite.setText(isFavorite ? "♥" : "♡");
        holder.tvFavorite.setTextColor(isFavorite ? 0xFFD32F2F : 0xFF777777);

        holder.tvFavorite.setOnClickListener(v -> toggleFavorite(mechanic));

        String phone = mechanic.getPhone();

        if (phone != null && !phone.trim().isEmpty()) {
            holder.btnContact.setEnabled(true);
            holder.btnContact.setText("Call");

            holder.btnContact.setOnClickListener(v -> {
                Intent intent = new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + phone)
                );
                context.startActivity(intent);
            });
        } else {
            holder.btnContact.setEnabled(false);
            holder.btnContact.setText("Call Unavailable");
        }

        holder.btnNavigate.setOnClickListener(v -> {
            String searchQuery = mechanic.getName() + " "
                    + mechanic.getSpecialization() + " mechanic";

            Uri mapUri = Uri.parse(
                    "geo:0,0?q=" + Uri.encode(searchQuery)
            );

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                Intent browserIntent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query="
                                + Uri.encode(searchQuery))
                );
                context.startActivity(browserIntent);
            }
        });
    }

    private void toggleFavorite(Mechanic mechanic) {

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String mechanicId = mechanic.getId();

        if (mechanicId == null || mechanicId.trim().isEmpty()) {
            Toast.makeText(context, "Invalid mechanic", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mechanicId.startsWith("demo")) {
            Toast.makeText(context, "Demo mechanics cannot be saved", Toast.LENGTH_SHORT).show();
            return;
        }

        if (favoriteIds.contains(mechanicId)) {

            favoriteIds.remove(mechanicId);
            notifyDataSetChanged();

            db.collection("users")
                    .document(userId)
                    .collection("favoriteMechanics")
                    .document(mechanicId)
                    .delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(context, "Removed from favorites",
                                    Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        favoriteIds.add(mechanicId);
                        notifyDataSetChanged();

                        Toast.makeText(context, "Could not remove favorite",
                                Toast.LENGTH_SHORT).show();
                    });

        } else {

            favoriteIds.add(mechanicId);
            notifyDataSetChanged();

            Map<String, Object> favorite = new HashMap<>();
            favorite.put("mechanicId", mechanicId);
            favorite.put("name", mechanic.getName());
            favorite.put("phone", mechanic.getPhone());
            favorite.put("specialization", mechanic.getSpecialization());
            favorite.put("experience", mechanic.getExperience());
            favorite.put("vehicle", mechanic.getVehicle());
            favorite.put("rating", mechanic.getRating());
            favorite.put("available", mechanic.isAvailable());
            favorite.put("savedAt", FieldValue.serverTimestamp());

            db.collection("users")
                    .document(userId)
                    .collection("favoriteMechanics")
                    .document(mechanicId)
                    .set(favorite)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(context, "Added to favorites",
                                    Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        favoriteIds.remove(mechanicId);
                        notifyDataSetChanged();

                        Toast.makeText(context, "Could not save favorite",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public int getItemCount() {
        return mechanicList.size();
    }

    static class MechanicViewHolder extends RecyclerView.ViewHolder {

        TextView tvMechanicName;
        TextView tvSpecialization;
        TextView tvExperience;
        TextView tvVehicle;
        TextView tvRating;
        TextView tvAvailability;
        TextView tvFavorite;

        Button btnContact;
        Button btnNavigate;

        public MechanicViewHolder(@NonNull View itemView) {
            super(itemView);

            tvMechanicName = itemView.findViewById(R.id.tvMechanicName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvVehicle = itemView.findViewById(R.id.tvMechanicVehicle);
            tvRating = itemView.findViewById(R.id.tvMechanicRating);
            tvAvailability = itemView.findViewById(R.id.tvMechanicAvailability);
            tvFavorite = itemView.findViewById(R.id.tvFavorite);

            btnContact = itemView.findViewById(R.id.btnContactMechanic);
            btnNavigate = itemView.findViewById(R.id.btnNavigateMechanic);
        }
    }
}
