package com.tribuddy.model;

public class BikeWorkout extends Workout{
    public BikeWorkout(String name, int durationMinutes, IntensityZone zone, String notes){
        super(name, durationMinutes, zone, notes);
    }

    @Override
    public String getType(){return "Bike";}
}
