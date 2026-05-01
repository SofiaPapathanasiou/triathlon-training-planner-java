package com.tribuddy.model;
import com.tribuddy.exception.InvalidWorkoutException;

public abstract class Workout {
    private final String name;
    private final int durationMinutes;
    private final IntensityZone zone;
    private final String notes;

    protected Workout(String name, int durationMinutes, IntensityZone zone, String notes){
        if (name == null || name.isBlank()){
            throw new InvalidWorkoutException("Workout name cannot be empty.");
        }
        if (durationMinutes <= 0){
            throw new InvalidWorkoutException("Duration must be greater than 0 (got: " + durationMinutes + ").");
        }
        if (zone == null){
            throw new InvalidWorkoutException("Intensity zone cannot be null.");
        }
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.zone = zone;
        this.notes = notes;
    }
    public int getLoadScore(){
        return durationMinutes*zone.getLoadMultiplier();
    }
    public abstract String getType();
    public String getName(){return name;}
    public int getDurationMinutes(){return durationMinutes;}
    public IntensityZone getZone(){return zone;}
    public String getNotes(){return notes;}

    @Override
    public String toString(){
        return String.format("[%s] %s — %d min, %s (load %d)", getType(), name, durationMinutes, zone, getLoadScore());
    }
}
