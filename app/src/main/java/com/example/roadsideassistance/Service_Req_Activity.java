
package com.example.roadsideassistance;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Service_Req_Activity extends AppCompatActivity {

    private TextView SelectedService, tvEstimatedPrice;
    private AutoCompleteTextView etVehicleType, etServiceIssue;
    private EditText etServiceLocation;
    private Button btnSubmitServiceRequest;
    private Button btnPreferredDate, btnPreferredTime;
    private LinearLayout layoutPreferredSchedule;
    private FirebaseFirestore firestore;
    private String serviceName;
    private String requestId;
    private String preferredDate = "";
    private String preferredTime = "";
    private Spinner spinnerFuelType, spinnerFuelQuantity;
    private boolean isEditMode = false;
    private boolean isRequestAgain = false;
    private double estimatedPrice = 0;

    private static final double FUEL_DELIVERY_FEE = 150;
    private static final double PETROL_PRICE_PER_LITRE = 105;
    private static final double DIESEL_PRICE_PER_LITRE = 92;

    private final SimpleDateFormat storedDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private final SimpleDateFormat storedTimeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final SimpleDateFormat displayTimeFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_req);

        SelectedService = findViewById(R.id.tvSelectedService);
        tvEstimatedPrice = findViewById(R.id.tvEstimatedPrice);
        etVehicleType = findViewById(R.id.etVehicleType);
        etServiceIssue = findViewById(R.id.etServiceIssue);
        etServiceLocation = findViewById(R.id.etServiceLocation);
        btnSubmitServiceRequest = findViewById(R.id.btnSubmitServiceRequest);

        LinearLayout layoutFuelOptions = findViewById(R.id.layoutFuelOptions);
        layoutPreferredSchedule = findViewById(R.id.layoutPreferredSchedule);
        btnPreferredDate = findViewById(R.id.btnPreferredDate);
        btnPreferredTime = findViewById(R.id.btnPreferredTime);

        spinnerFuelType = findViewById(R.id.spinnerFuelType);
        spinnerFuelQuantity = findViewById(R.id.spinnerFuelQuantity);

        firestore = FirebaseFirestore.getInstance();

        serviceName = getIntent().getStringExtra("serviceName");
        requestId = getIntent().getStringExtra("requestId");
        isRequestAgain = getIntent().getBooleanExtra("requestAgain", false);
        isEditMode = requestId != null && !requestId.trim().isEmpty();

        if (serviceName == null || serviceName.trim().isEmpty()) {
            serviceName = "General Assistance";
        }

        layoutFuelOptions.setVisibility(
                serviceName.equalsIgnoreCase("Fuel Delivery")
                        ? View.VISIBLE : View.GONE
        );

        updatePreferredScheduleVisibility();
        SelectedService.setText(serviceName);

        setupDropdown(etVehicleType, new String[]{
                "Bike", "Scooter", "Car", "SUV", "Van", "Truck", "Other"
        });

        setupDropdown(etServiceIssue, getIssues(serviceName));

        btnPreferredDate.setOnClickListener(v -> showPreferredDatePicker());
        btnPreferredTime.setOnClickListener(v -> showPreferredTimePicker());

        etVehicleType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateEstimatedPrice();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        AdapterView.OnItemSelectedListener fuelSelectionListener =
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        updateEstimatedPrice();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        updateEstimatedPrice();
                    }
                };

        spinnerFuelType.setOnItemSelectedListener(fuelSelectionListener);
        spinnerFuelQuantity.setOnItemSelectedListener(fuelSelectionListener);

        if (isRequestAgain && !isEditMode) {
            prefillRequestAgain();
        }

        if (!isEditMode && !isRequestAgain) {
            loadDefaultVehicle();
        }

        updateEstimatedPrice();

        if (isEditMode) {
            btnSubmitServiceRequest.setText("Update Request");
            loadRequestForEditing();
        } else {
            btnSubmitServiceRequest.setText("Confirm Booking");
        }

        btnSubmitServiceRequest.setOnClickListener(v -> submitRequest());
    }

    private boolean isPreferredScheduleService() {
        return serviceName != null
                && (serviceName.equalsIgnoreCase("General Service")
                || serviceName.equalsIgnoreCase("Annual Service")
                || serviceName.equalsIgnoreCase("Inspection"));
    }

    private void updatePreferredScheduleVisibility() {
        if (layoutPreferredSchedule != null) {
            layoutPreferredSchedule.setVisibility(
                    isPreferredScheduleService() ? View.VISIBLE : View.GONE
            );
        }

        if (!isPreferredScheduleService()) {
            preferredDate = "";
            preferredTime = "";
            if (btnPreferredDate != null) {
                btnPreferredDate.setText("Select Preferred Date");
            }
            if (btnPreferredTime != null) {
                btnPreferredTime.setText("Select Preferred Time");
            }
        }
    }

    private void showPreferredDatePicker() {
        Calendar calendar = Calendar.getInstance();

        if (!preferredDate.isEmpty()) {
            try {
                Date selectedDate = storedDateFormat.parse(preferredDate);
                if (selectedDate != null) {
                    calendar.setTime(selectedDate);
                }
            } catch (ParseException ignored) {
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);

                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    if (selected.before(today)) {
                        Toast.makeText(this,
                                "Please select today or a future date",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    preferredDate = storedDateFormat.format(selected.getTime());
                    btnPreferredDate.setText(
                            "Date: " + displayDateFormat.format(selected.getTime())
                    );
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void showPreferredTimePicker() {
        Calendar calendar = Calendar.getInstance();

        if (!preferredTime.isEmpty()) {
            try {
                Date selectedTime = storedTimeFormat.parse(preferredTime);
                if (selectedTime != null) {
                    Calendar parsed = Calendar.getInstance();
                    parsed.setTime(selectedTime);
                    calendar.set(Calendar.HOUR_OF_DAY,
                            parsed.get(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE,
                            parsed.get(Calendar.MINUTE));
                }
            } catch (ParseException ignored) {
            }
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 10);
            calendar.set(Calendar.MINUTE, 0);
        }

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (picker, hourOfDay, minute) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute);

                    preferredTime = storedTimeFormat.format(selected.getTime());
                    btnPreferredTime.setText(
                            "Time: " + displayTimeFormat.format(selected.getTime())
                    );
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );

        dialog.show();
    }

    private boolean isPreferredScheduleValid() {
        if (!isPreferredScheduleService()) {
            return true;
        }

        if (preferredDate.trim().isEmpty()) {
            Toast.makeText(this,
                    "Please select your preferred booking date",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (preferredTime.trim().isEmpty()) {
            Toast.makeText(this,
                    "Please select your preferred booking time",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            Date date = storedDateFormat.parse(preferredDate);
            Date time = storedTimeFormat.parse(preferredTime);

            if (date == null || time == null) {
                return false;
            }

            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.setTime(date);

            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(time);

            Calendar booking = Calendar.getInstance();
            booking.set(
                    dateCalendar.get(Calendar.YEAR),
                    dateCalendar.get(Calendar.MONTH),
                    dateCalendar.get(Calendar.DAY_OF_MONTH),
                    timeCalendar.get(Calendar.HOUR_OF_DAY),
                    timeCalendar.get(Calendar.MINUTE),
                    0
            );
            booking.set(Calendar.MILLISECOND, 0);

            if (booking.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(this,
                        "Please select a future booking date and time",
                        Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (ParseException e) {
            Toast.makeText(this,
                    "Please select a valid booking date and time",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void setPreferredSchedule(String date, String time) {
        preferredDate = date == null ? "" : date;
        preferredTime = time == null ? "" : time;

        if (!preferredDate.isEmpty()) {
            try {
                Date parsedDate = storedDateFormat.parse(preferredDate);
                if (parsedDate != null) {
                    btnPreferredDate.setText(
                            "Date: " + displayDateFormat.format(parsedDate)
                    );
                }
            } catch (ParseException ignored) {
                preferredDate = "";
            }
        }

        if (!preferredTime.isEmpty()) {
            try {
                Date parsedTime = storedTimeFormat.parse(preferredTime);
                if (parsedTime != null) {
                    btnPreferredTime.setText(
                            "Time: " + displayTimeFormat.format(parsedTime)
                    );
                }
            } catch (ParseException ignored) {
                preferredTime = "";
            }
        }

        if (preferredDate.isEmpty()) {
            btnPreferredDate.setText("Select Preferred Date");
        }

        if (preferredTime.isEmpty()) {
            btnPreferredTime.setText("Select Preferred Time");
        }
    }

    private void loadDefaultVehicle() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (isFinishing() || isDestroyed()
                            || !userDocument.exists()) {
                        return;
                    }

                    String defaultVehicleId =
                            userDocument.getString("defaultVehicleId");

                    if (defaultVehicleId == null
                            || defaultVehicleId.trim().isEmpty()) {
                        return;
                    }

                    firestore.collection("users")
                            .document(user.getUid())
                            .collection("vehicles")
                            .document(defaultVehicleId)
                            .get()
                            .addOnSuccessListener(vehicleDocument -> {
                                if (isFinishing() || isDestroyed()
                                        || !vehicleDocument.exists()) {
                                    return;
                                }

                                String vehicleType =
                                        vehicleDocument.getString("type");

                                if (vehicleType != null
                                        && !vehicleType.trim().isEmpty()
                                        && etVehicleType.getText().toString()
                                        .trim().isEmpty()) {

                                    etVehicleType.setText(vehicleType, false);
                                    updateEstimatedPrice();

                                    Toast.makeText(this,
                                            "Default vehicle selected",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void prefillRequestAgain() {
        String vehicle = getIntent().getStringExtra("againVehicleType");
        String issue = getIntent().getStringExtra("againIssue");
        String location = getIntent().getStringExtra("againLocation");
        String fuelType = getIntent().getStringExtra("againFuelType");
        String fuelQuantity = getIntent().getStringExtra("againFuelQuantity");

        etVehicleType.setText(vehicle == null ? "" : vehicle, false);
        etServiceIssue.setText(issue == null ? "" : issue, false);
        etServiceLocation.setText(location == null ? "" : location);

        if ("Fuel Delivery".equalsIgnoreCase(serviceName)) {
            selectSpinnerItem(spinnerFuelType, fuelType);
            selectSpinnerItem(spinnerFuelQuantity, fuelQuantity);
        }
    }

    private void setupDropdown(AutoCompleteTextView field, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                options
        );

        field.setAdapter(adapter);
        field.setThreshold(1);
        field.setOnClickListener(v -> field.showDropDown());
    }

    private String[] getIssues(String service) {
        switch (service) {
            case "Tow Truck":
                return new String[]{
                        "Vehicle Breakdown", "Accident Recovery",
                        "Vehicle Won't Start", "Other"
                };

            case "Flat Tire Change":
                return new String[]{
                        "Flat Tire", "Puncture", "Damaged Tire",
                        "Spare Tire Needed", "Other"
                };

            case "Battery Jumpstart":
            case "Battery Maintenance":
                return new String[]{
                        "Dead Battery", "Vehicle Not Starting",
                        "Jump Start Required", "Battery Check",
                        "Battery Maintenance", "Other"
                };

            case "Roadside Repair":
                return new String[]{
                        "Engine Trouble", "Overheating", "Brake Issue",
                        "Mechanical Failure", "Other"
                };

            case "Fuel Delivery":
                return new String[]{
                        "Out of Fuel", "Petrol Needed", "Diesel Needed", "Other"
                };

            case "Lockout Service":
                return new String[]{
                        "Keys Locked Inside", "Lost Keys", "Broken Key", "Other"
                };

            case "Oil Delivery":
                return new String[]{
                        "Engine Oil Needed", "Oil Level Low", "Other"
                };

            case "General Service":
                return new String[]{
                        "Regular Vehicle Servicing", "Engine Oil Change",
                        "Filter Replacement", "Brake Inspection",
                        "Vehicle Maintenance", "Other"
                };

            case "Annual Service":
                return new String[]{
                        "Annual Vehicle Maintenance",
                        "Complete Vehicle Servicing",
                        "Engine Oil and Filter Change",
                        "Brake and Tyre Check",
                        "Full Vehicle Checkup", "Other"
                };

            case "Inspection":
                return new String[]{
                        "General Vehicle Inspection",
                        "Pre-Purchase Inspection", "Engine Inspection",
                        "Brake Inspection", "Tyre Inspection",
                        "Vehicle Safety Check", "Other"
                };

            default:
                return new String[]{
                        "General Breakdown", "Accident Assistance",
                        "Vehicle Problem", "Other"
                };
        }
    }

    private double getVehicleMultiplier() {
        String vehicle = etVehicleType.getText().toString().trim();

        if (vehicle.equalsIgnoreCase("Bike")
                || vehicle.equalsIgnoreCase("Scooter")) {
            return 0.7;
        }

        if (vehicle.equalsIgnoreCase("SUV")
                || vehicle.equalsIgnoreCase("Van")) {
            return 1.3;
        }

        if (vehicle.equalsIgnoreCase("Truck")) {
            return 1.8;
        }

        return 1.0;
    }

    private double getServiceBasePrice() {
        if (serviceName == null) {
            return 500;
        }

        switch (serviceName.toLowerCase()) {
            case "tow truck":
                return 1200;
            case "flat tire change":
                return 400;
            case "battery jumpstart":
                return 500;
            case "battery maintenance":
                return 600;
            case "roadside repair":
                return 800;
            case "lockout service":
                return 450;
            case "oil delivery":
                return 250;
            case "general service":
            case "annual service":
            case "inspection":
            default:
                return 500;
        }
    }

    private void updateEstimatedPrice() {
        if (tvEstimatedPrice == null || serviceName == null) {
            return;
        }

        if (serviceName.equalsIgnoreCase("Fuel Delivery")) {
            if (spinnerFuelType == null || spinnerFuelQuantity == null
                    || spinnerFuelType.getSelectedItemPosition() == 0
                    || spinnerFuelQuantity.getSelectedItemPosition() == 0) {

                estimatedPrice = 0;
                tvEstimatedPrice.setText(
                        "Estimated Price: Select fuel type and quantity"
                );
                return;
            }

            String fuelType = spinnerFuelType.getSelectedItem().toString();
            String quantityText = spinnerFuelQuantity.getSelectedItem().toString();
            int litres = extractLitres(quantityText);

            if (litres <= 0) {
                estimatedPrice = 0;
                tvEstimatedPrice.setText(
                        "Estimated Price: Select fuel quantity"
                );
                return;
            }

            double fuelRate = fuelType.toLowerCase().contains("diesel")
                    ? DIESEL_PRICE_PER_LITRE : PETROL_PRICE_PER_LITRE;

            estimatedPrice =
                    (FUEL_DELIVERY_FEE * getVehicleMultiplier())
                            + (litres * fuelRate);

        } else {
            estimatedPrice = getServiceBasePrice() * getVehicleMultiplier();
        }

        tvEstimatedPrice.setText(
                "Estimated Price: ₹" + Math.round(estimatedPrice)
        );
    }

    private int extractLitres(String value) {
        Matcher matcher = Pattern.compile("\\d+").matcher(value);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        return 0;
    }

    private void selectSpinnerItem(Spinner spinner, String value) {
        if (value == null || value.trim().isEmpty()
                || spinner.getAdapter() == null) {
            return;
        }

        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            String item = spinner.getAdapter().getItem(i).toString();

            if (item.equalsIgnoreCase(value)
                    || item.toLowerCase().contains(value.toLowerCase())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void loadRequestForEditing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this,
                    "Please log in again", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnSubmitServiceRequest.setEnabled(false);

        firestore.collection("serviceRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!document.exists()
                            || !user.getUid().equals(document.getString("userId"))
                            || !"Pending".equalsIgnoreCase(
                            document.getString("status"))) {

                        Toast.makeText(this,
                                "This request can no longer be edited",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    serviceName = document.getString("serviceType");

                    if (serviceName == null || serviceName.trim().isEmpty()) {
                        serviceName = "General Assistance";
                    }

                    LinearLayout layoutFuelOptions =
                            findViewById(R.id.layoutFuelOptions);

                    layoutFuelOptions.setVisibility(
                            serviceName.equalsIgnoreCase("Fuel Delivery")
                                    ? View.VISIBLE : View.GONE
                    );

                    updatePreferredScheduleVisibility();
                    SelectedService.setText(serviceName);
                    setupDropdown(etServiceIssue, getIssues(serviceName));

                    String vehicle = document.getString("vehicleType");
                    String issue = document.getString("issue");
                    String location = document.getString("location");

                    etVehicleType.setText(vehicle == null ? "" : vehicle, false);
                    etServiceIssue.setText(issue == null ? "" : issue, false);
                    etServiceLocation.setText(location == null ? "" : location);

                    selectSpinnerItem(spinnerFuelType,
                            document.getString("fuelType"));
                    selectSpinnerItem(spinnerFuelQuantity,
                            document.getString("fuelQuantity"));

                    if (isPreferredScheduleService()) {
                        setPreferredSchedule(
                                document.getString("preferredDate"),
                                document.getString("preferredTime")
                        );
                    }

                    updateEstimatedPrice();
                    btnSubmitServiceRequest.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(this,
                            "Unable to load request: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void submitRequest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this,
                    "Please log in before submitting a request",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String vehicle = etVehicleType.getText().toString().trim();
        String issue = etServiceIssue.getText().toString().trim();
        String location = etServiceLocation.getText().toString().trim();

        if (vehicle.isEmpty()) {
            etVehicleType.setError("Enter or select vehicle type");
            etVehicleType.requestFocus();
            return;
        }

        if (issue.isEmpty()) {
            etServiceIssue.setError("Enter or select your issue");
            etServiceIssue.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            etServiceLocation.setError("Enter your location");
            etServiceLocation.requestFocus();
            return;
        }

        if (!isPreferredScheduleValid()) {
            return;
        }

        if ("Fuel Delivery".equalsIgnoreCase(serviceName)) {
            if (spinnerFuelType.getSelectedItemPosition() == 0) {
                Toast.makeText(this,
                        "Please select fuel type", Toast.LENGTH_SHORT).show();
                return;
            }

            if (spinnerFuelQuantity.getSelectedItemPosition() == 0) {
                Toast.makeText(this,
                        "Please select fuel quantity", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        updateEstimatedPrice();

        if (estimatedPrice <= 0) {
            Toast.makeText(this,
                    "Please select valid service options to calculate the price",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode) {
            saveRequest(user, vehicle, issue, location);
        } else {
            showBookingConfirmation(user, vehicle, issue, location);
        }
    }

    private void showBookingConfirmation(
            FirebaseUser user,
            String vehicle,
            String issue,
            String location
    ) {
        StringBuilder details = new StringBuilder();

        details.append("Service: ").append(serviceName)
                .append("\nVehicle: ").append(vehicle)
                .append("\nIssue: ").append(issue)
                .append("\nLocation: ").append(location)
                .append("\nEstimated Price: ₹").append(Math.round(estimatedPrice));

        if (isPreferredScheduleService()) {
            details.append("\nPreferred Date: ")
                    .append(btnPreferredDate.getText().toString())
                    .append("\nPreferred Time: ")
                    .append(btnPreferredTime.getText().toString());
        }

        if ("Fuel Delivery".equalsIgnoreCase(serviceName)) {
            details.append("\nFuel Type: ")
                    .append(spinnerFuelType.getSelectedItem().toString())
                    .append("\nFuel Quantity: ")
                    .append(spinnerFuelQuantity.getSelectedItem().toString());
        }

        details.append("\n\nBooking Status: Pending");

        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage(details.toString())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Confirm & Save", (dialog, which) -> {
                    saveRequest(user, vehicle, issue, location);
                })
                .show();
    }

    private void saveRequest(
            FirebaseUser user,
            String vehicle,
            String issue,
            String location
    ) {
        btnSubmitServiceRequest.setEnabled(false);
        btnSubmitServiceRequest.setText(
                isEditMode ? "Updating..." : "Saving..."
        );

        Map<String, Object> request = new HashMap<>();
        request.put("serviceType", serviceName);
        request.put("vehicleType", vehicle);
        request.put("issue", issue);
        request.put("location", location);
        request.put("estimatedPrice", Math.round(estimatedPrice));
        request.put("updatedAt", FieldValue.serverTimestamp());

        if (isPreferredScheduleService()) {
            request.put("preferredDate", preferredDate);
            request.put("preferredTime", preferredTime);
        } else if (isEditMode) {
            request.put("preferredDate", FieldValue.delete());
            request.put("preferredTime", FieldValue.delete());
        }

        if ("Fuel Delivery".equalsIgnoreCase(serviceName)) {
            request.put("fuelType", spinnerFuelType.getSelectedItem().toString());
            request.put("fuelQuantity",
                    spinnerFuelQuantity.getSelectedItem().toString());
        } else if (isEditMode) {
            request.put("fuelType", FieldValue.delete());
            request.put("fuelQuantity", FieldValue.delete());
        }

        if (isEditMode) {
            firestore.collection("serviceRequests")
                    .document(requestId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (!document.exists()
                                || !user.getUid().equals(document.getString("userId"))
                                || !"Pending".equalsIgnoreCase(
                                document.getString("status"))) {

                            btnSubmitServiceRequest.setEnabled(true);
                            btnSubmitServiceRequest.setText("Update Request");

                            Toast.makeText(this,
                                    "This request is no longer pending and cannot be edited",
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        firestore.collection("serviceRequests")
                                .document(requestId)
                                .update(request)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this,
                                            "Request updated successfully",
                                            Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> showSaveError(e, true));
                    })
                    .addOnFailureListener(e -> showSaveError(e, true));

        } else {
            request.put("userId", user.getUid());
            request.put("userEmail",
                    user.getEmail() == null ? "" : user.getEmail());
            request.put("status", "Pending");
            request.put("createdAt", FieldValue.serverTimestamp());

            firestore.collection("serviceRequests")
                    .add(request)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this,
                                "Booking saved successfully! Status: Pending",
                                Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> showSaveError(e, false));
        }
    }

    private void showSaveError(Exception e, boolean editing) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        btnSubmitServiceRequest.setEnabled(true);
        btnSubmitServiceRequest.setText(
                editing ? "Update Request" : "Confirm Booking"
        );

        Toast.makeText(this,
                (editing ? "Update failed: " : "Booking save failed: ")
                        + e.getMessage(),
                Toast.LENGTH_LONG).show();
    }
}
