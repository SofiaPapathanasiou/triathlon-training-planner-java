package com.tribuddy.service;

import com.tribuddy.exception.SchedulingConflictException;
import com.tribuddy.model.*;

import java.time.DayOfWeek;
import java.util.*;

public class Scheduler {

    private final ConstraintValidator validator;

    public Scheduler(ConstraintValidator validator) {
        this.validator = validator;
    }

    public TrainingPlan buildPlan(Athlete athlete, List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            throw new SchedulingConflictException("Cannot build a plan with no workouts.");
        }

        List<DayOfWeek> availableDays = new ArrayList<>(athlete.getAvailableDays());
        availableDays.sort(Comparator.comparingInt(DayOfWeek::getValue));

        if (workouts.size() > availableDays.size()) {
            throw new SchedulingConflictException(
                    "Cannot schedule " + workouts.size() + " workouts across only "
                            + availableDays.size() + " available days.");
        }

        // Separate weekend and weekday available days
        List<DayOfWeek> weekendDays = availableDays.stream()
                .filter(d -> d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY)
                .toList();

        List<DayOfWeek> weekdayDays = availableDays.stream()
                .filter(d -> d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY)
                .toList();

        // Sort workouts hardest first
        List<Workout> sorted = new ArrayList<>(workouts);
        sorted.sort(Comparator.comparingInt(w -> w.getZone().getLoadMultiplier()));
        Collections.reverse(sorted);

        TrainingPlan plan = new TrainingPlan();

        int weekdayIndex = 0;
        int weekendIndex = 0;
        boolean hardPlacedOnWeekend = false;

        for (Workout w : sorted) {
            boolean isHard = w.getZone() == IntensityZone.Z4_THRESHOLD
                    || w.getZone() == IntensityZone.Z5_VO2MAX;

            if (isHard && !hardPlacedOnWeekend && weekendIndex < weekendDays.size()) {
                plan.addWorkout(weekendDays.get(weekendIndex++), w);
                hardPlacedOnWeekend = true;
            } else if (weekdayIndex < weekdayDays.size()) {
                plan.addWorkout(weekdayDays.get(weekdayIndex++), w);
            } else if (weekendIndex < weekendDays.size()) {
                plan.addWorkout(weekendDays.get(weekendIndex++), w);
            } else {
                throw new SchedulingConflictException("Not enough available days to schedule all workouts.");
            }
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

    public TrainingPlan handleMissedWorkout(TrainingPlan plan, Athlete athlete, DayOfWeek missedDay) {
        List<Workout> missed = new ArrayList<>(plan.getDayWorkouts(missedDay));
        if (missed.isEmpty()) {
            return plan;
        }

        for (int i = missed.size() - 1; i >= 0; i--) {
            plan.removeWorkout(missedDay, i);
        }

        List<DayOfWeek> availableDays = new ArrayList<>(athlete.getAvailableDays());
        availableDays.sort(Comparator.comparingInt(DayOfWeek::getValue));

        DayOfWeek rescheduleDay = null;
        for (DayOfWeek day : availableDays) {
            if (day.getValue() > missedDay.getValue()
                    && plan.getDayWorkouts(day).isEmpty()) {
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