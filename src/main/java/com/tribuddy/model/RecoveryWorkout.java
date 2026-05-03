package com.tribuddy.model;

public class RecoveryWorkout extends Workout{
    public RecoveryWorkout(String name, int durationMinutes, String notes) {
        super(name, durationMinutes, IntensityZone.Z1_RECOVERY, notes);
    }

    @Override
    public String getType(){return "Recovery";}
}
