package com.tribuddy.db;

import com.tribuddy.model.TrainingPlan;
import com.tribuddy.model.Workout;

import java.time.DayOfWeek;

public interface WorkoutDao {
    void insertWorkout(Workout workout, long athleteId, DayOfWeek day);
    TrainingPlan getWorkoutsForAthlete(long athleteId);
    void updateWorkoutStatus(long id, String status);
    void deleteWorkout(long id);
    void clearWorkoutsForAthlete(long athleteId);
}