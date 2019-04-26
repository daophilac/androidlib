package com.peanut.androidlib.general;

import java.util.Calendar;

public class ExecutionTimeCalculator {
    private long beforeMillisecond;
    private long afterMillisecond;
    public void startTracking(){
        beforeMillisecond = Calendar.getInstance().getTimeInMillis();
    }
    public void stopTracking(){
        afterMillisecond = Calendar.getInstance().getTimeInMillis();
    }

    public long getBeforeMillisecond() {
        return beforeMillisecond;
    }

    public long getAfterMillisecond() {
        return afterMillisecond;
    }

    public long getExecutionTime(){
        return afterMillisecond - beforeMillisecond;
    }
}
