package com.tribuddy.ui;
import com.tribuddy.ai.AICoach;
import com.tribuddy.db.*;
import com.tribuddy.model.*;
import com.tribuddy.service.*;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConsoleUI {
    private final Scanner scanner = new Scanner(System.in);
    private final ConstraintValidator validator = new ConstraintValidator();
    private final Scheduler scheduler = new Scheduler(validator);
    private final FatigueCalculator fatigueCalculator = new FatigueCalculator();
    private final AICoach aiCoach;
    private final AthleteDao athleteDao = new JdbcAthleteDao();
    private final WorkoutDao workoutDao = new JdbcWorkoutDao();

    private Athlete athlete;
    private long athleteId = -1;
    private TrainingPlan currentPlan = new TrainingPlan();
    private final List<Workout> workoutPool = new ArrayList<>();

    public ConsoleUI(String apiKey) {
        this.aiCoach = new AICoach(apiKey);
    }

    public void run() {
        DatabaseSetup.initialize();
        System.out.println("   Welcome to TriBuddy!!!");
        setupAthlete();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = readChoice("Choice: ", List.of("1","2","3","4","5","6","7","8","9"));
            switch (choice) {
                case "1" -> addWorkout();
                case "2" -> generatePlan();
                case "3" -> viewPlan();
                case "4" -> markWorkout();
                case "5" -> viewFatigueReport();
                case "6" -> getAIFeedback();
                case "7" -> running = false;
                case "8" -> moveWorkout();
                case "9" -> manageWorkoutPool();
            }
        }
        System.out.println("\nGood luck with your training!");
    }

    // Setup
    private void setupAthlete() {
        System.out.println("Setting up your profile.");
        String name = readNonBlank("Your name: ");
        Set<DayOfWeek> days = selectDays("Select your available training days");
        athlete = new Athlete(name, days);
        athleteDao.insertAthlete(athlete);
        System.out.println("\nProfile created: " + athlete + "\n");
    }

    // Menu actions
    private void addWorkout() {
        System.out.println("\n--- Add Workout ---");
        System.out.println("Type: 1=Swim  2=Bike  3=Run  4=Strength  5=Recovery");
        String typeChoice = readChoice("Choice: ", List.of("1","2","3","4","5"));

        String name = readNonBlank("Workout name: ");
        int duration = readPositiveInt("Duration (minutes): ");

        IntensityZone zone = null;
        if (!typeChoice.equals("4") && !typeChoice.equals("5")) {
            System.out.println("Zone: 1=Z1_RECOVERY  2=Z2_AEROBIC  3=Z3_TEMPO  4=Z4_THRESHOLD  5=Z5_VO2MAX");
            String zoneChoice = readChoice("Choice: ", List.of("1","2","3","4","5"));
            zone = switch (zoneChoice) {
                case "1" -> IntensityZone.Z1_RECOVERY;
                case "2" -> IntensityZone.Z2_AEROBIC;
                case "3" -> IntensityZone.Z3_TEMPO;
                case "4" -> IntensityZone.Z4_THRESHOLD;
                case "5" -> IntensityZone.Z5_VO2MAX;
                default  -> IntensityZone.Z2_AEROBIC;
            };
        }

        System.out.print("Notes (optional, press Enter to skip): ");
        String notes = scanner.nextLine().trim();

        try {
            Workout workout = switch (typeChoice) {
                case "1" -> new SwimWorkout(name, duration, zone, notes);
                case "2" -> new BikeWorkout(name, duration, zone, notes);
                case "3" -> new RunWorkout(name, duration, zone, notes);
                case "4" -> new StrengthWorkout(name, duration, notes);
                case "5" -> new RecoveryWorkout(name, duration, notes);
                default  -> new RunWorkout(name, duration, zone, notes);
            };
            workoutPool.add(workout);
            System.out.println("Added: " + workout);
        } catch (Exception e) {
            System.out.println("Invalid workout: " + e.getMessage());
        }
    }

    private void generatePlan() {
        if (workoutPool.isEmpty()) {
            System.out.println("No workouts added yet. Add some workouts first.");
            return;
        }
        try {
            currentPlan = scheduler.buildPlan(athlete, workoutPool);
            workoutDao.clearWorkoutsForAthlete(athleteId);
            for (Map.Entry<DayOfWeek, List<Workout>> entry : currentPlan.getSchedule().entrySet()) {
                for (Workout w : entry.getValue()) {
                    workoutDao.insertWorkout(w, athleteId, entry.getKey());
                }
            }
            System.out.println("\nPlan generated and saved!");
            System.out.println(currentPlan);
        } catch (Exception e) {
            System.out.println("Could not generate plan: " + e.getMessage());
        }
    }

    private void viewPlan() {
        if (currentPlan.isEmpty()) {
            System.out.println("No plan yet. Use option 2 first.");
        } else {
            System.out.println("\n" + currentPlan);
        }
    }

    private void markWorkout() {
        if (currentPlan.isEmpty()) {
            System.out.println("No plan yet. Use option 2 first.");
            return;
        }
        System.out.println("\nWhich day?");
        Set<DayOfWeek> days = selectDays("Enter day");
        DayOfWeek day = days.iterator().next();

        List<Workout> workouts = currentPlan.getDayWorkouts(day);
        if (workouts.isEmpty()) {
            System.out.println("No workouts on " + day);
            return;
        }

        for (int i = 0; i < workouts.size(); i++) {
            System.out.println(i + ". " + workouts.get(i));
        }

        int index = readBoundedInt("Select workout number: ", 0, workouts.size() - 1);
        String statusChoice = readChoice("1=Completed  2=Skipped\nChoice: ", List.of("1","2"));
        WorkoutStatus status = statusChoice.equals("1")
                ? WorkoutStatus.COMPLETED
                : WorkoutStatus.SKIPPED;

        workouts.get(index).setStatus(status);
        System.out.println("Marked as " + status);
    }

    private void viewFatigueReport() {
        if (currentPlan.isEmpty()) {
            System.out.println("Generate a plan first.");
            return;
        }
        FatigueReport report = fatigueCalculator.calculate(currentPlan, athlete);
        System.out.println("\n" + report);

        List<ConstraintValidator.ValidationResult> results = validator.validate(currentPlan);
        System.out.println("\nConstraint Check:");
        for (ConstraintValidator.ValidationResult r : results) {
            System.out.println("  " + (r.passed() ? "✓" : "✗") + " " + r.message());
        }
    }

    private void getAIFeedback() {
        if (currentPlan.isEmpty()) {
            System.out.println("Generate a plan first.");
            return;
        }
        FatigueReport report = fatigueCalculator.calculate(currentPlan, athlete);
        System.out.println("\nFetching AI feedback (this may take a few seconds)...");

        CompletableFuture<String> feedbackFuture = CompletableFuture
                .supplyAsync(() -> aiCoach.getWeeklyFeedback(currentPlan, report))
                .completeOnTimeout("[Feedback timed out]", 10, TimeUnit.SECONDS);

        CompletableFuture<String> focusFuture = CompletableFuture
                .supplyAsync(() -> aiCoach.recommendNextWeekFocus(report, 1))
                .completeOnTimeout("[Recommendation timed out]", 10, TimeUnit.SECONDS);

        feedbackFuture.thenCombine(focusFuture, (feedback, focus) -> {
            System.out.println("\nAI Coaching Feedback");
            System.out.println(feedback);
            System.out.println("\nNext Week Focus");
            System.out.println(focus);
            return null;
        }).join();
    }

    private void moveWorkout() {
        if (currentPlan.isEmpty()) {
            System.out.println("Generate a plan first.");
            return;
        }
        System.out.println("\n" + currentPlan);

        Set<DayOfWeek> fromSet = selectDays("Move FROM which day?");
        DayOfWeek fromDay = fromSet.iterator().next();

        List<Workout> workouts = currentPlan.getDayWorkouts(fromDay);
        if (workouts.isEmpty()) {
            System.out.println("No workouts on " + fromDay);
            return;
        }

        for (int i = 0; i < workouts.size(); i++) {
            System.out.println(i + ". " + workouts.get(i));
        }

        int index = readBoundedInt("Select workout number: ", 0, workouts.size() - 1);

        Set<DayOfWeek> toSet = selectDays("Move TO which day?");
        DayOfWeek toDay = toSet.iterator().next();

        try {
            currentPlan = scheduler.moveWorkout(currentPlan, fromDay, index, toDay);
            System.out.println("Workout moved successfully!");
            System.out.println(currentPlan);
        } catch (Exception e) {
            System.out.println("Could not move workout: " + e.getMessage());
        }
    }

    private void manageWorkoutPool() {
        if (workoutPool.isEmpty()) {
            System.out.println("No workouts added yet.");
            return;
        }

        System.out.println("\n--- Your Workout Pool ---");
        for (int i = 0; i < workoutPool.size(); i++) {
            System.out.println(i + ". " + workoutPool.get(i));
        }

        System.out.println("\nWhat would you like to do?");
        System.out.println("1. Remove a workout");
        System.out.println("2. Generate plan with selected workouts only");
        System.out.println("3. Back to menu");
        String choice = readChoice("Choice: ", List.of("1","2","3"));

        switch (choice) {
            case "1" -> removeFromPool();
            case "2" -> generatePlanFromSelection();
            case "3" -> {}
        }
    }

    private void removeFromPool() {
        System.out.println("Enter the number of the workout to remove (0 to "
                + (workoutPool.size() - 1) + "):");
        int index = readBoundedInt("Workout number: ", 0, workoutPool.size() - 1);
        System.out.println("Removed: " + workoutPool.get(index));
        workoutPool.remove(index);
    }

    private void generatePlanFromSelection() {
        System.out.println("\nEnter workout numbers to include (comma separated, e.g. 0,1,3):");
        System.out.print("> ");
        String input = scanner.nextLine().trim();

        List<Workout> selected = new ArrayList<>();
        for (String token : input.split(",")) {
            try {
                int index = Integer.parseInt(token.trim());
                if (index >= 0 && index < workoutPool.size()) {
                    selected.add(workoutPool.get(index));
                } else {
                    System.out.println("Skipping invalid index: " + index);
                }
            } catch (NumberFormatException e) {
                System.out.println("Skipping invalid input: " + token.trim());
            }
        }

        if (selected.isEmpty()) {
            System.out.println("No valid workouts selected.");
            return;
        }

        System.out.println("\nGenerating plan with " + selected.size() + " workouts:");
        for (Workout w : selected) {
            System.out.println("  " + w);
        }

        try {
            currentPlan = scheduler.buildPlan(athlete, selected);
            workoutDao.clearWorkoutsForAthlete(athleteId);
            for (Map.Entry<DayOfWeek, List<Workout>> entry : currentPlan.getSchedule().entrySet()) {
                for (Workout w : entry.getValue()) {
                    workoutDao.insertWorkout(w, athleteId, entry.getKey());
                }
            }
            System.out.println("\nPlan generated and saved!");
            System.out.println(currentPlan);
        } catch (Exception e) {
            System.out.println("Could not generate plan: " + e.getMessage());
        }
    }

    // Helper methods
    private void printMenu() {
        System.out.println("\n--- Menu ---");
        System.out.println("1. Add workout");
        System.out.println("2. Generate weekly plan");
        System.out.println("3. View current plan");
        System.out.println("4. Mark workout complete/skipped");
        System.out.println("5. View fatigue report");
        System.out.println("6. Get AI feedback");
        System.out.println("7. Exit");
        System.out.println("8. Move a workout to a different day");
        System.out.println("9. View/manage workout pool");
    }

    private String readChoice(String prompt, List<String> validOptions) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (validOptions.contains(input)) return input;
            System.out.println("Invalid option. Please enter one of: " + validOptions);
        }
    }

    private String readNonBlank(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isBlank()) return input;
            System.out.println("This field cannot be empty.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) return value;
            System.out.println("Please enter a number greater than 0.");
        }
    }

    private int readBoundedInt(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) return value;
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    private Set<DayOfWeek> selectDays(String prompt) {
        while (true) {
            System.out.println(prompt + " (e.g. MONDAY,WEDNESDAY,FRIDAY):");
            System.out.println("Options: MONDAY TUESDAY WEDNESDAY THURSDAY FRIDAY SATURDAY SUNDAY");
            System.out.print("> ");
            Set<DayOfWeek> days = new LinkedHashSet<>();
            for (String token : scanner.nextLine().toUpperCase().split(",")) {
                try {
                    days.add(DayOfWeek.valueOf(token.trim()));
                } catch (IllegalArgumentException e) {
                    System.out.println("Unrecognized day: " + token.trim());
                }
            }
            if (!days.isEmpty()) return days;
            System.out.println("No valid days entered. Please try again.");
        }
    }

    // Entry point
    public static void main(String[] args) {
        String apiKey = System.getenv("GROQ_API_KEY");
        new ConsoleUI(apiKey).run();
    }
}