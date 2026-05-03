package com.tribuddy.db;

import com.tribuddy.exception.DatabaseOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseSetup {
    private static final Logger log = LoggerFactory.getLogger(DatabaseSetup.class);

    public static void initialize() {
        createAthletesTable();
        createWorkoutsTable();
        log.info("Database initialized successfully.");
    }

    private static void createAthletesTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS athletes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    curr_fatigue INTEGER DEFAULT 0,
                    prev_week_load INTEGER DEFAULT 0
                );
                """;
        executeStatement(sql);
    }

    private static void createWorkoutsTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS workouts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    athlete_id INTEGER,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    zone TEXT NOT NULL,
                    notes TEXT,
                    day_of_week TEXT,
                    status TEXT DEFAULT 'PLANNED',
                    FOREIGN KEY (athlete_id) REFERENCES athletes(id)
                );
                """;
        executeStatement(sql);
    }

    private static void executeStatement(String sql) {
        try (Connection conn = DriverManager.getConnection(DbConfig.URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            log.error("Failed to create table: {}", e.getMessage());
            throw new DatabaseOperationException("Database setup failed: " + e.getMessage());
        }
    }
}

