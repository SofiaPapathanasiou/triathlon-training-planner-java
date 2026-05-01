package com.tribuddy.model;
import com.tribuddy.exception.InvalidAthleteException;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

public class Athlete {
    private final String name;
    private Set<DayOfWeek> availableDays;
    private int currFatigue;
    private int prevWeekLoad;

    public Athlete(String name, Set<DayOfWeek> availableDays){
        if (name == null || name.isBlank()){
            throw new InvalidAthleteException("Athlete name cannot be empty.");
        }
        if (availableDays == null || availableDays.isEmpty()){
            throw new InvalidAthleteException("Athlete must have at least one available training day.");
        }
        this.name = name;
        this.availableDays = EnumSet.copyOf(availableDays);
        this.currFatigue = 0;
        this.prevWeekLoad = 0;
    }

    public String getName() {return name;}
    public Set<DayOfWeek> getAvailableDays(){return EnumSet.copyOf(availableDays);}
    public int getCurrFatigue(){return currFatigue;}
    public int getPrevWeekLoad(){return prevWeekLoad;}

    public void setCurrFatigue(int score)    { this.currFatigue = score; }
    public void setPrevWeekLoad(int score)  { this.prevWeekLoad = score; }
    public void setAvailableDays(Set<DayOfWeek> availableDays) {
        if (availableDays == null || availableDays.isEmpty()) {
            throw new InvalidAthleteException("Athlete must have at least one available training day.");
        }
        this.availableDays = EnumSet.copyOf(availableDays);
    }
    @Override
    public String toString() {
        return String.format("Athlete: %s | Available: %s | Fatigue: %d",
                name, availableDays, currFatigue);
    }
}
