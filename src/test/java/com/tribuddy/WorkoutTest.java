package com.tribuddy;

import com.tribuddy.exception.InvalidWorkoutException;
import com.tribuddy.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkoutTest {

    @Test
    void loadScore_isCorrectForAerobicWorkout() {
        Workout w = new RunWorkout("Easy run", 60, IntensityZone.Z2_AEROBIC, "");
        assertEquals(240, w.getLoadScore()); // 60 * 4
    }

    @Test
    void loadScore_isCorrectForStrengthWorkout() {
        Workout w = new StrengthWorkout("Core", 30, "");
        assertEquals(150, w.getLoadScore()); // 30 * 5
    }

    @Test
    void loadScore_isCorrectForRecoveryWorkout() {
        Workout w = new RecoveryWorkout("Yoga", 60, "");
        assertEquals(120, w.getLoadScore()); // 60 * 2
    }

    @Test
    void constructor_throwsOnZeroDuration() {
        assertThrows(InvalidWorkoutException.class,
                () -> new RunWorkout("Bad run", 0, IntensityZone.Z2_AEROBIC, ""));
    }

    @Test
    void constructor_throwsOnNegativeDuration() {
        assertThrows(InvalidWorkoutException.class,
                () -> new SwimWorkout("Bad swim", -10, IntensityZone.Z2_AEROBIC, ""));
    }

    @Test
    void constructor_throwsOnBlankName() {
        assertThrows(InvalidWorkoutException.class,
                () -> new RunWorkout("  ", 30, IntensityZone.Z2_AEROBIC, ""));
    }

    @Test
    void status_defaultsToPlanned() {
        Workout w = new BikeWorkout("Ride", 45, IntensityZone.Z3_TEMPO, "");
        assertEquals(WorkoutStatus.PLANNED, w.getStatus());
    }

    @Test
    void status_canBeChangedToCompleted() {
        Workout w = new RunWorkout("Run", 30, IntensityZone.Z2_AEROBIC, "");
        w.setStatus(WorkoutStatus.COMPLETED);
        assertEquals(WorkoutStatus.COMPLETED, w.getStatus());
    }

    @Test
    void recoveryWorkout_alwaysZ1() {
        Workout w = new RecoveryWorkout("Yoga", 30, "");
        assertEquals(IntensityZone.Z1_RECOVERY, w.getZone());
    }
}