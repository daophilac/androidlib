package com.peanut.androidlib.general;

import java.util.Calendar;

public class ExecutionTimeCalculator {
    private static final String HAVE_NOT_STARTED_YET = "Tracker has not been started yet.";
    private long beforeMillisecond;
    private long afterMillisecond;
    private boolean started;
    public void startTracking(){
        this.beforeMillisecond = Calendar.getInstance().getTimeInMillis();
        this.started = true;
    }
    public void stopTracking(){
        if(!this.started){
            throw new IllegalStateException(HAVE_NOT_STARTED_YET);
        }
        this.started = false;
        this.afterMillisecond = Calendar.getInstance().getTimeInMillis();
    }

    public long getBeforeMillisecond() {
        return this.beforeMillisecond;
    }

    public long getAfterMillisecond() {
        return this.afterMillisecond;
    }

    public long getExecutionTime(){
        return this.afterMillisecond - this.beforeMillisecond;
    }
}
