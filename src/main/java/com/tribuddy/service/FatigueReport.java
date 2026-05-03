package com.tribuddy.service;

public class FatigueReport {
    public enum Status {
        NORMAL,
        ELEVATED,
        CRITICAL
    }

    private final int weeklyLoad;
    private final int prevWeekLoad;
    private final double changePercent;
    private final Status status;
    private final boolean deLoad;

    public FatigueReport(int weeklyLoad, int prevWeekLoad, double changePercent, Status status, boolean deLoad){
        this.weeklyLoad = weeklyLoad;
        this.prevWeekLoad = prevWeekLoad;
        this.changePercent = changePercent;
        this.status = status;
        this.deLoad = deLoad;
    }
    public int getWeeklyLoad(){return weeklyLoad;}
    public int getPrevWeekLoad(){return prevWeekLoad;}
    public double getChangePercent(){return changePercent;}
    public Status getStatus() {return status;}
    public boolean isDeLoad() {return deLoad;}

    @Override
    public String toString(){
        return String.format("Fatigue Report: load=%d (prev=%d, %+.1f%%) | Status: %s | De-load: %s",
                weeklyLoad, prevWeekLoad, changePercent,
                status, deLoad ? "YES" : "No");
    }
}
