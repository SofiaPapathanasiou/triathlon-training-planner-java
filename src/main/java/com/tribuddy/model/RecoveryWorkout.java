package com.tribuddy.model;

public class RecoveryWorkout extends Workout{
    public RecoveryWorkout(String name, int durationMinutes, IntensityZone zone, String notes){
        super(name, durationMinutes, zone, notes);
    }

    @Override
    public String getType(){return "Recovery";}
}
