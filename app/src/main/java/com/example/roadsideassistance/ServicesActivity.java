package com.example.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ServicesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        openRequest(R.id.cardTowTruck, "Tow Truck");
        openRequest(R.id.cardFlatTire, "Flat Tire Change");
        openRequest(R.id.cardBattery, "Battery Jumpstart");
        openRequest(R.id.cardRepair, "Roadside Repair");
        openRequest(R.id.cardFuel, "Fuel Delivery");
        openRequest(R.id.cardLockout, "Lockout Service");
        openRequest(R.id.cardOil, "Oil Delivery");
        openRequest(R.id.cardGeneral, "General Assistance");
    }

    private void openRequest(int cardId, String serviceName) {
        CardView card = findViewById(cardId);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ServicesActivity.this,
                    Service_Req_Activity.class
            );
            intent.putExtra("serviceName", serviceName);
            startActivity(intent);
        });
    }
}
