package com.example.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;


    RecyclerView rvGarageServices;

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

        rvGarageServices = findViewById(R.id.rvGarageServices);

        List<GarageServiceAdapter.GarageService> garageList = new ArrayList<>();

        garageList.add(new GarageServiceAdapter.GarageService(
                "Auto Garage Towing",
                "Gandhinager Sec-24",
                "9999999999",
                "Replace with your real garage address"
        ));

        garageList.add(new GarageServiceAdapter.GarageService(
                "Rk Tow Service",
                "Mirzapur Ahmedabad",
                "7687877889",
                "Replace with your real towing service address"
        ));


        garageList.add(new GarageServiceAdapter.GarageService(
                "Towing Expert",
                "C.G Road Ahmedabad",
                "7687987989",
                "Replace with your real towing service address"
        ));




        rvGarageServices.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        rvGarageServices.setAdapter(new GarageServiceAdapter(this, garageList));

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
}
