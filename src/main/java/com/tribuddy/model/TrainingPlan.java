package com.tribuddy.model;
import java.time.DayOfWeek;
import java.util.*;

public class TrainingPlan {
    private final Map<DayOfWeek, List<Workout>> schedule;
    public TrainingPlan(){
        this.schedule = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()){
            schedule.put(day, new ArrayList<>());
        }
    }
    public void addWorkout(DayOfWeek day, Workout workout){
        if (day == null || workout == null){
            throw new IllegalArgumentException("Day and workout cannot be null.");
        }
        schedule.get(day).add(workout);
    }

    public void removeWorkout(DayOfWeek day, int index){
        List<Workout> workouts = schedule.get(day);
        if (index <0 || index >= workouts.size()){
            throw new IllegalArgumentException("No workout at index " + index + " on "+ day+ ".");
        }
        workouts.remove(index);
    }

    public List<Workout> getDayWorkouts(DayOfWeek day){ return Collections.unmodifiableList(schedule.get(day));}
    public List<Workout> getAllWorkouts(){
        List<Workout> all = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()){
            all.addAll(schedule.get(day));
        }
        return all;
    }
    public int getTotalLoadScore() {
        return getAllWorkouts().stream()
                .filter(w->w.getStatus()== WorkoutStatus.COMPLETED)
                .mapToInt(Workout::getLoadScore).sum();
    }
    public int getPlannedLoadScore() {
        return getAllWorkouts().stream().mapToInt(Workout::getLoadScore).sum();
    }
    public int getCompletedCount(){
        return (int) getAllWorkouts().stream()
                .filter(w->w.getStatus() == WorkoutStatus.COMPLETED).count();
    }

    public int getSkippedCount(){
        return (int) getAllWorkouts().stream()
                .filter(w->w.getStatus() == WorkoutStatus.SKIPPED).count();
    }
    public boolean isEmpty() {
        return getAllWorkouts().isEmpty();
    }

    public void clearAll() {
        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.get(day).clear();
        }
    }

    public Map<DayOfWeek, List<Workout>> getSchedule() {
        Map<DayOfWeek, List<Workout>> copy = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            copy.put(day, new ArrayList<>(schedule.get(day)));
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Training Plan\n");
        for (DayOfWeek day : DayOfWeek.values()) {
            List<Workout> workouts = schedule.get(day);
            sb.append(day).append(":\n");
            if (workouts.isEmpty()) {
                sb.append("  (rest)\n");
            } else {
                for (Workout w : workouts) {
                    sb.append("  ").append(w).append("\n");
                }
            }
        }
        sb.append("Planned load: ").append(getPlannedLoadScore());
        sb.append(" | Completed load: ").append(getTotalLoadScore());
        return sb.toString();
    }
}
