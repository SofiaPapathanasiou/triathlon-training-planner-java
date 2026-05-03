package com.tribuddy.service;
import com.tribuddy.model.Athlete;
import com.tribuddy.model.TrainingPlan;

public class FatigueCalculator {
    private static final double ELEVATED_THRESHOLD = 0.1;
    private static final double CRITICAL_THRESHOLD = 0.25;

    public FatigueReport calculate(TrainingPlan plan, Athlete athlete){
        int currLoad = plan.getTotalLoadScore();
        int prevLoad = athlete.getPrevWeekLoad();

        double changePercent = 0.0;
        if (prevLoad > 0){
            changePercent = ((double)(currLoad-prevLoad)/prevLoad)*100;
        }
        FatigueReport.Status status;
        boolean deLoad;

        if (changePercent > CRITICAL_THRESHOLD*100){
            status = FatigueReport.Status.CRITICAL;
            deLoad = true;
        } else if (changePercent > ELEVATED_THRESHOLD*100){
            status = FatigueReport.Status.ELEVATED;
            deLoad = false;
        } else{
            status = FatigueReport.Status.NORMAL;
            deLoad = false;
        }

        if (athlete.getCurrFatigue() > 300) {
            deLoad = true;
        }
        return new FatigueReport(currLoad, prevLoad, changePercent, status, deLoad);
    }
}
