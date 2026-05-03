package com.tribuddy.db;

import com.tribuddy.exception.DatabaseOperationException;
import com.tribuddy.model.Athlete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;


public class JdbcAthleteDao implements AthleteDao{
    private static final Logger log = LoggerFactory.getLogger(JdbcAthleteDao.class);
    @Override
    public void insertAthlete(Athlete athlete){
        String sql = "INSERT INTO athletes (name, curr_fatigue, prev_week_load) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, athlete.getName());
            stmt.setInt(2, athlete.getCurrFatigue());
            stmt.setInt(3, athlete.getPrevWeekLoad());
            stmt.executeUpdate();}
        catch(SQLException e){
            log.error("Failed to insert athlete: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to insert athlete: " + e.getMessage());
        }
    }
    @Override
    public Athlete getAthlete(long id) {
        String sql = "SELECT * FROM athletes WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAthlete(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get athlete: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to get athlete: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void updateFatigue(long id, int currFatigue, int prevWeekLoad) {
        String sql = "UPDATE athletes SET curr_fatigue = ?, prev_week_load = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currFatigue);
            stmt.setInt(2, prevWeekLoad);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update fatigue: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to update fatigue: " + e.getMessage());
        }
    }
    @Override
    public void deleteAthlete(long id) {
        String sql = "DELETE FROM athletes WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete athlete: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to delete athlete: " + e.getMessage());
        }
    }

    private Athlete mapRowToAthlete(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        // Default available days since we don't store them in DB
        Set<DayOfWeek> defaultDays = EnumSet.of(
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        );
        Athlete athlete = new Athlete(name, defaultDays);
        athlete.setCurrFatigue(rs.getInt("curr_fatigue"));
        athlete.setPrevWeekLoad(rs.getInt("prev_week_load"));
        return athlete;
    }

}
