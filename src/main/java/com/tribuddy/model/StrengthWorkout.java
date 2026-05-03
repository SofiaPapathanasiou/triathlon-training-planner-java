package com.tribuddy.model;

public class StrengthWorkout extends Workout{
    public StrengthWorkout(String name, int durationMinutes, String notes) {
        super(name, durationMinutes, null, notes);
    }

    @Override
    public String getType(){return "Strength";}
}
