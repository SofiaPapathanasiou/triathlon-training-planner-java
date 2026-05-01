package com.tribuddy.model;

public class StrengthWorkout extends Workout{
    public StrengthWorkout(String name, int durationMinutes, IntensityZone zone, String notes){
        super(name, durationMinutes, zone, notes);
    }

    @Override
    public String getType(){return "Strength";}
}
