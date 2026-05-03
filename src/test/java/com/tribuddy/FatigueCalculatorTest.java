package com.tribuddy;

import com.tribuddy.model.*;
import com.tribuddy.service.FatigueCalculator;
import com.tribuddy.service.FatigueReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FatigueCalculatorTest {

    private FatigueCalculator calculator;
    private Athlete athlete;

    @BeforeEach
    void setUp() {
        calculator = new FatigueCalculator();
        athlete = new Athlete("Sofia", Set.of(
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
    }

    @Test
    void status_isNormal_whenNoPreviousLoad() {
        TrainingPlan plan = new TrainingPlan();
        Workout w = new RunWorkout("Run", 60, IntensityZone.Z2_AEROBIC, "");
        w.setStatus(WorkoutStatus.COMPLETED);
        plan.addWorkout(DayOfWeek.MONDAY, w);
        FatigueReport report = calculator.calculate(plan, athlete);
        assertEquals(FatigueReport.Status.NORMAL, report.getStatus());
    }

    @Test
    void status_isElevated_whenIncreaseBetween10And25Percent() {
        athlete.setPrevWeekLoad(1000);
        TrainingPlan plan = new TrainingPlan();
        Workout w = new BikeWorkout("Bike", 100, IntensityZone.Z4_THRESHOLD, ""); // 100 * 8 = 800
        Workout w2 = new RunWorkout("Run", 60, IntensityZone.Z3_TEMPO, "");       // 60 * 6 = 360
        // total = 1160, +16% — clearly elevated
        w.setStatus(WorkoutStatus.COMPLETED);
        w2.setStatus(WorkoutStatus.COMPLETED);
        plan.addWorkout(DayOfWeek.MONDAY, w);
        plan.addWorkout(DayOfWeek.WEDNESDAY, w2);
        FatigueReport report = calculator.calculate(plan, athlete);
        assertEquals(FatigueReport.Status.ELEVATED, report.getStatus());
    }

    @Test
    void status_isCritical_whenIncreaseOver25Percent() {
        athlete.setPrevWeekLoad(500);
        TrainingPlan plan = new TrainingPlan();
        Workout w = new RunWorkout("Run", 90, IntensityZone.Z5_VO2MAX, ""); // 90 * 10 = 900
        w.setStatus(WorkoutStatus.COMPLETED);
        plan.addWorkout(DayOfWeek.MONDAY, w);
        FatigueReport report = calculator.calculate(plan, athlete);
        assertEquals(FatigueReport.Status.CRITICAL, report.getStatus());
        assertTrue(report.isDeLoad());
    }

    @Test
    void emptyPlan_hasZeroLoad() {
        TrainingPlan plan = new TrainingPlan();
        FatigueReport report = calculator.calculate(plan, athlete);
        assertEquals(0, report.getWeeklyLoad());
    }

    @Test
    void skippedWorkouts_dontCountTowardLoad() {
        athlete.setPrevWeekLoad(0);
        TrainingPlan plan = new TrainingPlan();
        Workout w = new RunWorkout("Run", 60, IntensityZone.Z2_AEROBIC, "");
        w.setStatus(WorkoutStatus.SKIPPED);
        plan.addWorkout(DayOfWeek.MONDAY, w);
        FatigueReport report = calculator.calculate(plan, athlete);
        assertEquals(0, report.getWeeklyLoad());
    }

    @Test
    void deLoad_recommendedWhenCurrFatigueHigh() {
        athlete.setCurrFatigue(350);
        athlete.setPrevWeekLoad(1000);
        TrainingPlan plan = new TrainingPlan();
        Workout w = new RunWorkout("Easy", 30, IntensityZone.Z1_RECOVERY, "");
        w.setStatus(WorkoutStatus.COMPLETED);
        plan.addWorkout(DayOfWeek.MONDAY, w);
        FatigueReport report = calculator.calculate(plan, athlete);
        assertTrue(report.isDeLoad());
    }
}