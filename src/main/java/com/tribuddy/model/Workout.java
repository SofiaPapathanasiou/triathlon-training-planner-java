package com.tribuddy.model;
import com.tribuddy.exception.InvalidWorkoutException;

public abstract class Workout {
    private final String name;
    private final int durationMinutes;
    private final IntensityZone zone;
    private String notes;
    private WorkoutStatus status;

    protected Workout(String name, int durationMinutes, IntensityZone zone, String notes){
        if (name == null || name.isBlank()){
            throw new InvalidWorkoutException("Workout name cannot be empty.");
        }
        if (durationMinutes <= 0){
            throw new InvalidWorkoutException("Duration must be greater than 0 (got: " + durationMinutes + ").");
        }
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.zone = zone;
        this.notes = notes;
        this.status = WorkoutStatus.PLANNED;
    }
    public int getLoadScore() {
        if (zone != null) {
            return durationMinutes * zone.getLoadMultiplier();
        }
        return durationMinutes * 5;
    }
    public abstract String getType();
    public String getName(){return name;}
    public int getDurationMinutes(){return durationMinutes;}
    public IntensityZone getZone(){return zone;}
    public String getNotes(){return notes;}
    public void setNotes(String notes) { this.notes = (notes == null) ? "" : notes; }
    public WorkoutStatus getStatus(){return status;}
    public void setStatus(WorkoutStatus status){this.status = status;}

    @Override
    public String toString(){
        return String.format("[%s] %s — %d min, %s (load %d)", getType(), name, durationMinutes, zone, getLoadScore());
    }
}
