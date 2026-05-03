package com.tribuddy.db;

import com.tribuddy.exception.DatabaseOperationException;
import com.tribuddy.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.DayOfWeek;

public class JdbcWorkoutDao implements WorkoutDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcWorkoutDao.class);

    @Override
    public void insertWorkout(Workout workout, long athleteId, DayOfWeek day) {
        String sql = "INSERT INTO workouts (athlete_id, name, type, duration_minutes, zone, notes, day_of_week, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, athleteId);
            stmt.setString(2, workout.getName());
            stmt.setString(3, workout.getType());
            stmt.setInt(4, workout.getDurationMinutes());
            stmt.setString(5, workout.getZone().name());
            stmt.setString(6, workout.getNotes());
            stmt.setString(7, day.name());
            stmt.setString(8, workout.getStatus().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to insert workout: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to insert workout: " + e.getMessage());
        }
    }

    @Override
    public TrainingPlan getWorkoutsForAthlete(long athleteId) {
        String sql = "SELECT * FROM workouts WHERE athlete_id = ?";
        TrainingPlan plan = new TrainingPlan();
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, athleteId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DayOfWeek day = DayOfWeek.valueOf(rs.getString("day_of_week"));
                    Workout workout = mapRowToWorkout(rs);
                    plan.addWorkout(day, workout);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get workouts: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to get workouts: " + e.getMessage());
        }
        return plan;
    }

    @Override
    public void updateWorkoutStatus(long id, String status) {
        String sql = "UPDATE workouts SET status = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update workout status: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to update workout status: " + e.getMessage());
        }
    }

    @Override
    public void deleteWorkout(long id) {
        String sql = "DELETE FROM workouts WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete workout: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to delete workout: " + e.getMessage());
        }
    }

    @Override
    public void clearWorkoutsForAthlete(long athleteId) {
        String sql = "DELETE FROM workouts WHERE athlete_id = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, athleteId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to clear workouts: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to clear workouts: " + e.getMessage());
        }
    }

    private Workout mapRowToWorkout(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        String type = rs.getString("type");
        int duration = rs.getInt("duration_minutes");
        IntensityZone zone = IntensityZone.valueOf(rs.getString("zone"));
        String notes = rs.getString("notes");
        WorkoutStatus status = WorkoutStatus.valueOf(rs.getString("status"));

        Workout workout = switch (type) {
            case "Swim"     -> new SwimWorkout(name, duration, zone, notes);
            case "Bike"     -> new BikeWorkout(name, duration, zone, notes);
            case "Run"      -> new RunWorkout(name, duration, zone, notes);
            case "Strength" -> new StrengthWorkout(name, duration, zone, notes);
            case "Recovery" -> new RecoveryWorkout(name, duration, zone, notes);
            default -> throw new DatabaseOperationException("Unknown workout type: " + type);
        };

        workout.setStatus(status);
        return workout;
    }
}
