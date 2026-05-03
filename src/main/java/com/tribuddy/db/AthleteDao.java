package com.tribuddy.db;

import com.tribuddy.model.Athlete;

public interface AthleteDao {
    void insertAthlete(Athlete athlete);
    Athlete getAthlete(long id);
    void updateFatigue(long id, int currFatigue, int prevWeekLoad);
    void deleteAthlete(long id);
}
