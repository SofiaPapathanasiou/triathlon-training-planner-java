package com.tribuddy.model;

public enum IntensityZone {
    Z1_RECOVERY(2),
    Z2_AEROBIC(4),
    Z3_TEMPO(6),
    Z4_THRESHOLD(8),
    Z5_VO2MAX(10);

    private final int loadMultiplier;
    IntensityZone(int loadMultiplier){
        this.loadMultiplier = loadMultiplier;
    }
    public int getLoadMultiplier(){
        return loadMultiplier;
    }
}
