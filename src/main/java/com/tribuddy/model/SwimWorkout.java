package com.tribuddy.model;

public class SwimWorkout extends Workout{
    public SwimWorkout(String name, int durationMinutes, IntensityZone zone, String notes){
        super(name, durationMinutes, zone, notes);
    }

    @Override
    public String getType(){return "Swim";}
}
