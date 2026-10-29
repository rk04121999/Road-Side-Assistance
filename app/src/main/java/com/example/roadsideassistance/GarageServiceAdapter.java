
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

import java.util.List;

public class GarageServiceAdapter extends RecyclerView.Adapter<GarageServiceAdapter.GarageViewHolder> {

    public static class GarageService {
        public String name;
        public String address;
        public String phone;
        public String mapsQuery;

        public GarageService(String name, String address, String phone, String mapsQuery) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.mapsQuery = mapsQuery;
        }
    }

    private final Context context;
    private final List<GarageService> garageList;

    public GarageServiceAdapter(Context context, List<GarageService> garageList) {
        this.context = context;
        this.garageList = garageList;
    }

    @NonNull
    @Override
    public GarageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_garage_service, parent, false);
        return new GarageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GarageViewHolder holder, int position) {
        GarageService service = garageList.get(position);

        holder.tvName.setText(service.name);
        holder.tvAddress.setText(service.address);
        holder.tvPhone.setText(service.phone);

        holder.btnCall.setOnClickListener(v -> {
            String phone = service.phone.replaceAll("[^+0-9]", "");

            if (phone.isEmpty()) {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            context.startActivity(intent);
        });

        holder.btnNavigate.setOnClickListener(v -> {
            String query = service.mapsQuery != null && !service.mapsQuery.trim().isEmpty()
                    ? service.mapsQuery
                    : service.address;

            Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                    + Uri.encode(query));

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return garageList.size();
    }

    static class GarageViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        TextView tvPhone;
        Button btnCall;
        Button btnNavigate;

        GarageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvGarageName);
            tvAddress = itemView.findViewById(R.id.tvGarageAddress);
            tvPhone = itemView.findViewById(R.id.tvGaragePhone);
            btnCall = itemView.findViewById(R.id.btnGarageCall);
            btnNavigate = itemView.findViewById(R.id.btnGarageNavigate);
        }
    }
}
