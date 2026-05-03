package com.tribuddy;

import com.tribuddy.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.*;

class TrainingPlanTest {

    private TrainingPlan plan;

    @BeforeEach
    void setUp() {
        plan = new TrainingPlan();
    }

    @Test
    void newPlan_isEmpty() {
        assertTrue(plan.isEmpty());
    }

    @Test
    void addWorkout_increasesCount() {
        plan.addWorkout(DayOfWeek.MONDAY, new RunWorkout("Run", 30, IntensityZone.Z2_AEROBIC, ""));
        assertEquals(1, plan.getDayWorkouts(DayOfWeek.MONDAY).size());
    }

    @Test
    void removeWorkout_removesCorrectly() {
        plan.addWorkout(DayOfWeek.TUESDAY, new BikeWorkout("Ride", 60, IntensityZone.Z2_AEROBIC, ""));
        plan.removeWorkout(DayOfWeek.TUESDAY, 0);
        assertTrue(plan.getDayWorkouts(DayOfWeek.TUESDAY).isEmpty());
    }

    @Test
    void removeWorkout_throwsOnInvalidIndex() {
        assertThrows(IllegalArgumentException.class,
                () -> plan.removeWorkout(DayOfWeek.MONDAY, 0));
    }

    @Test
    void plannedLoadScore_sumsAllWorkouts() {
        plan.addWorkout(DayOfWeek.MONDAY, new RunWorkout("Run", 60, IntensityZone.Z2_AEROBIC, ""));   // 240
        plan.addWorkout(DayOfWeek.WEDNESDAY, new SwimWorkout("Swim", 45, IntensityZone.Z2_AEROBIC, "")); // 180
        assertEquals(420, plan.getPlannedLoadScore());
    }

    @Test
    void completedLoadScore_onlyCountsCompleted() {
        Workout completed = new RunWorkout("Run", 60, IntensityZone.Z2_AEROBIC, "");
        Workout planned = new BikeWorkout("Bike", 60, IntensityZone.Z2_AEROBIC, "");
        completed.setStatus(WorkoutStatus.COMPLETED);
        plan.addWorkout(DayOfWeek.MONDAY, completed);
        plan.addWorkout(DayOfWeek.TUESDAY, planned);
        assertEquals(240, plan.getTotalLoadScore());
    }

    @Test
    void completedAndSkippedCount_isCorrect() {
        Workout w1 = new RunWorkout("Run", 30, IntensityZone.Z2_AEROBIC, "");
        Workout w2 = new BikeWorkout("Bike", 30, IntensityZone.Z2_AEROBIC, "");
        w1.setStatus(WorkoutStatus.COMPLETED);
        w2.setStatus(WorkoutStatus.SKIPPED);
        plan.addWorkout(DayOfWeek.MONDAY, w1);
        plan.addWorkout(DayOfWeek.TUESDAY, w2);
        assertEquals(1, plan.getCompletedCount());
        assertEquals(1, plan.getSkippedCount());
    }

    @Test
    void clearAll_emptiesThePlan() {
        plan.addWorkout(DayOfWeek.FRIDAY, new RunWorkout("Run", 30, IntensityZone.Z2_AEROBIC, ""));
        plan.clearAll();
        assertTrue(plan.isEmpty());
    }

    @Test
    void addWorkout_throwsOnNullDay() {
        assertThrows(IllegalArgumentException.class,
                () -> plan.addWorkout(null, new RunWorkout("Run", 30, IntensityZone.Z2_AEROBIC, "")));
    }
}