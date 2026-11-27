
package com.example.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicesActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private GridLayout servicesGrid;
    private final List<ServiceItem> serviceItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        firestore = FirebaseFirestore.getInstance();

        servicesGrid = findGridLayout(
                (ViewGroup) findViewById(android.R.id.content)
        );

        if (servicesGrid == null) {
            Toast.makeText(
                    this,
                    "Unable to load services",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        addService(R.id.cardTowTruck, "Tow Truck");
        addService(R.id.cardFlatTire, "Flat Tire Change");
        addService(R.id.cardBattery, "Battery Jumpstart");
        addService(R.id.cardRepair, "Roadside Repair");
        addService(R.id.cardFuel, "Fuel Delivery");
        addService(R.id.cardLockout, "Lockout Service");
        addService(R.id.cardOil, "Oil Delivery");
        addService(R.id.cardGeneral, "General Assistance");

        addService(R.id.cardGeneralService, "General Service");
        addService(R.id.cardAnnualService, "Annual Service");
        addService(R.id.cardInspection, "Inspection");

        loadPreferredServices();
    }

    private void addService(int cardId, String serviceName) {
        CardView card = findViewById(cardId);

        if (card == null) {
            return;
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ServicesActivity.this,
                    Service_Req_Activity.class
            );

            intent.putExtra("serviceName", serviceName);
            startActivity(intent);
        });

        serviceItems.add(new ServiceItem(card, serviceName));
    }

    private void loadPreferredServices() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()
                            || !document.exists()) {
                        return;
                    }

                    List<String> preferredServices =
                            (List<String>) document.get("preferredServiceTypes");

                    if (preferredServices == null
                            || preferredServices.isEmpty()) {
                        return;
                    }

                    arrangeServices(preferredServices);
                });
    }

    private void arrangeServices(List<String> preferredServices) {
        Map<String, Integer> preferredOrder = new HashMap<>();

        for (int i = 0; i < preferredServices.size(); i++) {
            String service = preferredServices.get(i);

            if (service != null) {
                preferredOrder.put(
                        service.trim().toLowerCase(),
                        i
                );
            }
        }

        Collections.sort(serviceItems, (first, second) -> {
            int firstPosition = preferredOrder.getOrDefault(
                    first.serviceName.toLowerCase(),
                    Integer.MAX_VALUE
            );

            int secondPosition = preferredOrder.getOrDefault(
                    second.serviceName.toLowerCase(),
                    Integer.MAX_VALUE
            );

            return Integer.compare(firstPosition, secondPosition);
        });

        servicesGrid.removeAllViews();

        for (ServiceItem item : serviceItems) {
            ViewGroup.LayoutParams params = item.card.getLayoutParams();
            servicesGrid.addView(item.card, params);
        }
    }

    private GridLayout findGridLayout(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child instanceof GridLayout) {
                return (GridLayout) child;
            }

            if (child instanceof ViewGroup) {
                GridLayout result = findGridLayout((ViewGroup) child);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private static class ServiceItem {

        private final CardView card;
        private final String serviceName;

        private ServiceItem(CardView card, String serviceName) {
            this.card = card;
            this.serviceName = serviceName;
        }
    }
}
