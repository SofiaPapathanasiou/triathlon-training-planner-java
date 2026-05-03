package com.tribuddy.service;

import com.tribuddy.model.TrainingPlan;
import com.tribuddy.model.Workout;
import com.tribuddy.model.IntensityZone;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class ConstraintValidator {
    private static final int MAX_DAILY_MINUTES = 180;
    public record ValidationResult(boolean passed, String message){}

    public List<ValidationResult> validate(TrainingPlan plan){
        List<ValidationResult> results = new ArrayList<>();
        results.addAll(checkConsecHardDays(plan));
        results.addAll(checkDailyVolume(plan));
        results.addAll(checkRecoveryDay(plan));
        return results;
    }
    public boolean isValid(TrainingPlan plan){
        return validate(plan).stream()
                .allMatch(ValidationResult::passed);
    }

    private List<ValidationResult> checkConsecHardDays(TrainingPlan plan){
        List<ValidationResult> results = new ArrayList<>();
        DayOfWeek[] days = DayOfWeek.values();
        for (int i = 0; i < days.length - 1; i++) {
            boolean tdHard = isHardDay(plan, days[i]);
            boolean tmrwHard = isHardDay(plan, days[i + 1]);
            if (tdHard && tmrwHard) {
                results.add(new ValidationResult(false,
                        "Back to back hard sessions on " + days[i] + " and " + days[i + 1]));
            }
        }
        if (results.isEmpty()) {
            results.add(new ValidationResult(true, "No back to back hard sessions."));
        }
        return results;
    }
    private List<ValidationResult> checkDailyVolume(TrainingPlan plan){
        List<ValidationResult> results = new ArrayList<>();

        for (DayOfWeek day : DayOfWeek.values()) {
            int totalMinutes = plan.getDayWorkouts(day).stream()
                    .mapToInt(Workout::getDurationMinutes)
                    .sum();
            if (totalMinutes > MAX_DAILY_MINUTES) {
                results.add(new ValidationResult(false,
                        day + " exceeds max daily volume: " + totalMinutes + " min (max " + MAX_DAILY_MINUTES + ")"));
            }
        }

        if (results.isEmpty()) {
            results.add(new ValidationResult(true, "Daily volume within limits."));
        }
        return results;
    }
    private List<ValidationResult> checkRecoveryDay(TrainingPlan plan) {
        boolean hasRecovery = plan.getAllWorkouts().stream()
                .anyMatch(w -> w.getZone() == IntensityZone.Z1_RECOVERY);

        if (!hasRecovery && !plan.isEmpty()) {
            return List.of(new ValidationResult(false,
                    "No recovery workout scheduled this week. Consider adding a Z1 session."));
        }
        return List.of(new ValidationResult(true, "Recovery day present."));
    }

    private boolean isHardDay(TrainingPlan plan, DayOfWeek day) {
        return plan.getDayWorkouts(day).stream()
                .anyMatch(w -> w.getZone() == IntensityZone.Z4_THRESHOLD
                        || w.getZone() == IntensityZone.Z5_VO2MAX);
    }

}
