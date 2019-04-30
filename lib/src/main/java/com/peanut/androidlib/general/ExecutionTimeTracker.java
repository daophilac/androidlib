package com.peanut.androidlib.general;

import java.util.Calendar;

public class ExecutionTimeTracker {
    private static final String ALREADY_STARTED = "Tracker has already started.";
    private static final String ALREADY_STOPPED = "Tracker has already stopped.";
    private static final String HAVE_NOT_STARTED_YET = "Tracker has not been started yet.";
    private static final String HAVE_NOT_STOPPED_YET = "Tracker has not been stopped yet.";
    private long beforeMillisecond;
    private long afterMillisecond;
    private boolean started;
    private boolean stopped;
    private boolean running;
    public ExecutionTimeTracker(){
        this.started = false;
        this.stopped = true;
        this.running = false;
    }
    public void startTracking(){
        if(this.started){
            throw new IllegalStateException(ALREADY_STARTED);
        }
        if(this.running && !this.stopped){
            throw new IllegalStateException(HAVE_NOT_STOPPED_YET);
        }
        this.beforeMillisecond = Calendar.getInstance().getTimeInMillis();
        this.started = true;
        this.stopped = false;
        this.running = true;
    }
    public void stopTracking(){
        if(!this.started){
            throw new IllegalStateException(HAVE_NOT_STARTED_YET);
        }
        if(this.stopped){
            throw new IllegalStateException(ALREADY_STOPPED);
        }
        this.afterMillisecond = Calendar.getInstance().getTimeInMillis();
        this.started = false;
        this.stopped = true;
        this.running = false;
    }

    public long getBeforeMillisecond() {
        if(!this.started){
            throw new IllegalStateException(HAVE_NOT_STARTED_YET);
        }
        return this.beforeMillisecond;
    }

    public long getAfterMillisecond() {
        if(!this.stopped){
            throw new IllegalStateException(HAVE_NOT_STOPPED_YET);
        }
        return this.afterMillisecond;
    }

    public long getExecutionTime(){
        if(!this.stopped){
            throw new IllegalStateException(HAVE_NOT_STOPPED_YET);
        }
        return this.afterMillisecond - this.beforeMillisecond;
    }
}
