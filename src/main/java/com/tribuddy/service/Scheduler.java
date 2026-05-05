package com.tribuddy.service;

import com.tribuddy.exception.SchedulingConflictException;
import com.tribuddy.model.*;

import java.time.DayOfWeek;
import java.util.*;

public class Scheduler {

    private final ConstraintValidator validator;
    private static final int MAX_WORKOUTS_PER_DAY = 2;

    public Scheduler(ConstraintValidator validator) {
        this.validator = validator;
    }

    public TrainingPlan buildPlan(Athlete athlete, List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            throw new SchedulingConflictException("Cannot build a plan with no workouts.");
        }

        List<DayOfWeek> availableDays = new ArrayList<>(athlete.getAvailableDays());
        availableDays.sort(Comparator.comparingInt(DayOfWeek::getValue));

        // Check if workouts can physically fit (max 2 per day)
        int maxCapacity = availableDays.size() * MAX_WORKOUTS_PER_DAY;
        if (workouts.size() > maxCapacity) {
            throw new SchedulingConflictException(
                    "Too many workouts (" + workouts.size() + ") for "
                            + availableDays.size() + " days. Max is " + maxCapacity + ".");
        }

        List<DayOfWeek> weekendDays = availableDays.stream()
                .filter(d -> d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY)
                .toList();

        List<DayOfWeek> weekdayDays = availableDays.stream()
                .filter(d -> d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY)
                .toList();

        // Sort hardest first
        List<Workout> sorted = new ArrayList<>(workouts);
        sorted.sort(Comparator.comparingInt(w -> w.getZone() != null ? w.getZone().getLoadMultiplier() : 0));
        Collections.reverse(sorted);

        TrainingPlan plan = new TrainingPlan();
        boolean hardPlacedOnWeekend = false;

        for (Workout w : sorted) {
            boolean isHard = w.getZone() != null && (w.getZone() == IntensityZone.Z4_THRESHOLD
                    || w.getZone() == IntensityZone.Z5_VO2MAX);

            DayOfWeek targetDay = null;

            // Try to place hard workout on weekend first
            if (isHard && !hardPlacedOnWeekend) {
                targetDay = findAvailableDay(weekendDays, plan);
                if (targetDay != null) hardPlacedOnWeekend = true;
            }

            // Otherwise find any available weekday slot
            if (targetDay == null) {
                targetDay = findAvailableDay(weekdayDays, plan);
            }

            // Fall back to any available day
            if (targetDay == null) {
                targetDay = findAvailableDay(availableDays, plan);
            }

            if (targetDay == null) {
                throw new SchedulingConflictException(
                        "Could not fit all workouts within the 2-per-day limit.");
            }

            plan.addWorkout(targetDay, w);
        }

        // Validate before returning
        List<ConstraintValidator.ValidationResult> results = validator.validate(plan);
        List<String> failures = results.stream()
                .filter(r -> !r.passed())
                .map(ConstraintValidator.ValidationResult::message)
                .toList();

        if (!failures.isEmpty()) {
            throw new SchedulingConflictException(
                    "Plan violates constraints:\n" + String.join("\n", failures));
        }

        return plan;
    }

    // Finds the first day that has fewer than MAX_WORKOUTS_PER_DAY workouts
    private DayOfWeek findAvailableDay(List<DayOfWeek> days, TrainingPlan plan) {
        for (DayOfWeek day : days) {
            if (plan.getDayWorkouts(day).size() < MAX_WORKOUTS_PER_DAY) {
                return day;
            }
        }
        return null;
    }

    public TrainingPlan moveWorkout(TrainingPlan plan, DayOfWeek fromDay,
                                    int workoutIndex, DayOfWeek toDay) {
        List<Workout> fromWorkouts = plan.getDayWorkouts(fromDay);

        if (workoutIndex < 0 || workoutIndex >= fromWorkouts.size()) {
            throw new SchedulingConflictException("No workout at that index.");
        }
        if (plan.getDayWorkouts(toDay).size() >= MAX_WORKOUTS_PER_DAY) {
            throw new SchedulingConflictException(
                    toDay + " already has " + MAX_WORKOUTS_PER_DAY + " workouts.");
        }

        Workout workout = fromWorkouts.get(workoutIndex);
        plan.removeWorkout(fromDay, workoutIndex);
        plan.addWorkout(toDay, workout);
        return plan;
    }

    public TrainingPlan handleMissedWorkout(TrainingPlan plan, Athlete athlete, DayOfWeek missedDay) {
        List<Workout> missed = new ArrayList<>(plan.getDayWorkouts(missedDay));
        if (missed.isEmpty()) return plan;

        for (int i = missed.size() - 1; i >= 0; i--) {
            plan.removeWorkout(missedDay, i);
        }

        List<DayOfWeek> availableDays = new ArrayList<>(athlete.getAvailableDays());
        availableDays.sort(Comparator.comparingInt(DayOfWeek::getValue));

        DayOfWeek rescheduleDay = null;
        for (DayOfWeek day : availableDays) {
            if (day.getValue() > missedDay.getValue()
                    && plan.getDayWorkouts(day).size() < MAX_WORKOUTS_PER_DAY) {
                rescheduleDay = day;
                break;
            }
        }

        if (rescheduleDay != null) {
            for (Workout w : missed) {
                w.setStatus(WorkoutStatus.SKIPPED);
                plan.addWorkout(rescheduleDay, w);
            }
        }

        return plan;
    }
}