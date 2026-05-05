# TriBuddy: Smart Training Planner for Triathletes

#### CS-UY 3913 Applied Java Programming, Spring 2026.
#### Applied Semester Project
#### Author: Sofia Papathanasiou

A Java console application that helps triathletes structure their weekly training.



## What It Does

TriBuddy lets an athlete build and manage a weekly training plan across swim, bike, run, strength, and recovery sessions. It schedules workouts intelligently, tracks training load and fatigue, enforces recovery constraints, persists data to a local SQLite database, and uses the Groq AI API to provide coaching feedback and recommend next week's training focus.



## How to Run

### Prerequisites
- Java 17 (Temurin-17 recommended)
- Apache Maven 3.8+

### Setup
```bash
# Clone or unzip the project
# project code available at git repo: https://github.com/SofiaPapathanasiou/triathlon-training-planner-java.git
cd TriathlonTrain

# Create the data directory for the SQLite database
mkdir data

# Set your Groq API key (get one free at console.groq.com)
export GROQ_API_KEY=your_key_here
```

### Build and Run
```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.tribuddy.ui.ConsoleUI"
```

Or run `ConsoleUI.main()` directly from IntelliJ with `GROQ_API_KEY` set as an environment variable in Run Configurations.

### Run Tests
```bash
mvn test
```

---

## Project Structure

```
src/
├── main/java/com/tribuddy/
│   ├── model/
│   │   ├── Workout.java              ← Abstract base class
│   │   ├── SwimWorkout.java          ← Workout subclasses (5 total)
│   │   ├── BikeWorkout.java
│   │   ├── RunWorkout.java
│   │   ├── StrengthWorkout.java      ← Fixed load multiplier, no zone
│   │   ├── RecoveryWorkout.java      ← Always Z1_RECOVERY
│   │   ├── Athlete.java              ← Profile, availability, fatigue
│   │   ├── TrainingPlan.java         ← Map<DayOfWeek, List<Workout>>
│   │   ├── IntensityZone.java        ← Enum: Z1–Z5 with load multipliers
│   │   └── WorkoutStatus.java        ← Enum: PLANNED, COMPLETED, SKIPPED
│   ├── service/
│   │   ├── Scheduler.java            ← Distributes workouts across days
│   │   ├── FatigueCalculator.java    ← Computes weekly load + fatigue status
│   │   ├── FatigueReport.java        ← Immutable result object
│   │   └── ConstraintValidator.java  ← Checks recovery rules
│   ├── db/
│   │   ├── DbConfig.java             ← SQLite connection string
│   │   ├── DatabaseSetup.java        ← Creates tables on startup
│   │   ├── AthleteDao.java           ← CRUD interface for athletes
│   │   ├── JdbcAthleteDao.java       ← JDBC implementation
│   │   ├── WorkoutDao.java           ← CRUD interface for workouts
│   │   └── JdbcWorkoutDao.java       ← JDBC implementation
│   ├── ai/
│   │   └── AICoach.java              ← Groq API integration
│   └── ui/
│       └── ConsoleUI.java            ← Menu-driven console interface
├── test/java/com/tribuddy/
│   ├── WorkoutTest.java              ← 9 tests
│   ├── TrainingPlanTest.java         ← 8 tests
│   └── FatigueCalculatorTest.java    ← 6 tests (total: 23 tests)
└── pom.xml
```

---

## Key Design Decisions

### Why `abstract Workout` with 5 subclasses?

Making `Workout` abstract enforces the rule that you can never schedule a "generic" workout, it must be a specific type (Swim, Bike, Run, etc.). This enables polymorphism throughout the codebase: `FatigueCalculator`, `ConstraintValidator`, and the database layer all operate on `Workout` without knowing the specific subclass. `getType()` and `getLoadScore()` behave differently per subclass, which is polymorphism in action.

`StrengthWorkout` and `RecoveryWorkout` pass `null` for zone because their intensity is fixed by definition (recovery is always easy (Z1), and strength follows a non-aerobic load model). This is a deliberate design choice that reflects real triathlon coaching principles.

### Why `IntensityZone` enum instead of a 1–10 scale?

Training zones (Z1–Z5) are the standard framework used by endurance athletes. Each zone maps to a physiological state (aerobic base, lactate threshold, VO2max, etc.) and carries a load multiplier that feeds directly into `getLoadScore()`. Using an enum instead of raw integers prevents invalid values, makes the code self-documenting, and enables `ConstraintValidator` to identify "hard" sessions (Z4/Z5) without arbitrary threshold comparisons.

### Why `Map<DayOfWeek, List<Workout>>` in `TrainingPlan`?

An `EnumMap` gives O(1) lookup by day and naturally models the weekly structure. Pre-initialising all 7 days with empty lists means callers never receive null (so iterating the full week always works without null checks). The map returns an unmodifiable view via `getDayWorkouts()` to protect the internal state.

### Why separate planned vs completed load scores?

`getTotalLoadScore()` only sums workouts with `status == COMPLETED`, which is what actually counts toward fatigue. `getPlannedLoadScore()` sums all workouts. Showing both gives the user meaningful feedback. They can see how much they planned vs how much they actually did, and `FatigueCalculator` correctly ignores skipped sessions.

### Why the DAO pattern for the database layer?

Following the same pattern from Lab 4, `AthleteDao` and `WorkoutDao` are interfaces that define the contract for persistence. `JdbcAthleteDao` and `JdbcWorkoutDao` implement those interfaces with raw JDBC. This means the rest of the application (schedulers, UI) depends only on the interface. If the database technology changed, only the implementation classes would need updating. All SQL uses `PreparedStatement` with `?` placeholders to prevent SQL injection, and every connection is opened inside a `try-with-resources` block to guarantee closure.

### Why is `AICoach` isolated in its own package?

AI output is non-deterministic. Isolating `AICoach` behind a single class ensures the rest of the application never depends on AI output for correctness. Every service class (Scheduler, FatigueCalculator, ConstraintValidator) works identically whether or not `AICoach` is called. All exceptions in `AICoach` are caught internally and return a fallback string — the app never crashes due to a network failure or missing API key.

### Why `CompletableFuture` for AI calls?

The app makes two independent AI calls when the user requests feedback: `getWeeklyFeedback()` and `recommendNextWeekFocus()`. Using `CompletableFuture.supplyAsync()` fires both calls simultaneously on separate threads rather than sequentially. `completeOnTimeout()` ensures the app never hangs if the API is slow, and `thenCombine()` merges the results when both complete. This directly applies the concurrency patterns taught in the course.

---

## External Libraries

| Library | Version | Purpose |
|---|---|---|
| JUnit Jupiter | 5.10.0 | Unit testing |
| SQLite JDBC | 3.42.0.0 | SQLite database driver |
| SLF4J Simple | 2.0.9 | Logging for database errors |
| Jackson Databind | 2.15.2 | JSON serialization for Groq API |

AI provider: Groq API (model: `llama-3.1-8b-instant`). Free tier, no credit card required.

---

## Academic Integrity

All design, implementation, and testing is my own work, completed individually in accordance with NYU's academic integrity policy. External libraries are listed above. The Groq API is used solely as a service called through my own `AICoach` class. All application logic is implemented in Java.

*CS-UY 3913 Applied Java Programming, Spring 2026, NYU Tandon*