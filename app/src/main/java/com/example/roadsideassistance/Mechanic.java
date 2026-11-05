package com.example.roadsideassistance;

public class Mechanic {

    private String id;
    private String name;
    private String phone;
    private String specialization;
    private String experience;
    private String vehicle;
    private double rating;
    private boolean available;

    public Mechanic(String id, String name, String phone,
                    String specialization, String experience,
                    String vehicle, double rating, boolean available) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.specialization = specialization;
        this.experience = experience;
        this.vehicle = vehicle;
        this.rating = rating;
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getExperience() {
        return experience;
    }

    public String getVehicle() {
        return vehicle;
    }

    public double getRating() {
        return rating;
    }

    public boolean isAvailable() {
        return available;
    }
}