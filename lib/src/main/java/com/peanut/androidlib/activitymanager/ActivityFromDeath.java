package com.peanut.androidlib.activitymanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;

public class ActivityFromDeath extends SoulSummoner {
    private Activity activity;
    private ActivityFromDeathListener activityFromDeathListener;
    private boolean started;
    private boolean stopped;
    public interface ActivityFromDeathListener {
        HashMap<String, Bundle> buildBundle();
    }
    public ActivityFromDeath(){}
    public ActivityFromDeath(Activity activity){
        this.activity = activity;
        this.activityFromDeathListener = (ActivityFromDeathListener) activity;
        SoulSummoner.setActivityClass(activity.getClass());
    }
    public void sendOnStopSignal(){
        if(!this.started || this.stopped){
            return;
        }
        HashMap<String, Bundle> mapBundleActivity = this.activityFromDeathListener.buildBundle();
        setMapBundleActivity(mapBundleActivity);
        Intent intent = new Intent(activity, com.peanut.androidlib.activitymanager.ActivityFromDeath.class);
        intent.setAction(ActivitySignal.ON_STOP);
        activity.sendOrderedBroadcast(intent, null);
    }
    public void sendOnDestroySignal(){
        if(!this.started || this.stopped){
            return;
        }
        Intent intent = new Intent(activity, com.peanut.androidlib.activitymanager.ActivityFromDeath.class);
        intent.setAction(SoulSummoner.ActivitySignal.ON_DESTROY);
        activity.sendOrderedBroadcast(intent, null);
    }
    public void sendOnResumeSignal(){
        if(!this.started || this.stopped){
            return;
        }
        Intent intent = new Intent(activity, com.peanut.androidlib.activitymanager.ActivityFromDeath.class);
        intent.setAction(SoulSummoner.ActivitySignal.ON_RESUME);
        activity.sendOrderedBroadcast(intent, null);
    }
    public void sendOnCreateSignal(){
        if(!this.started || this.stopped){
            return;
        }
        Intent intent = new Intent(activity, com.peanut.androidlib.activitymanager.ActivityFromDeath.class);
        intent.setAction(SoulSummoner.ActivitySignal.ON_CREATE);
        activity.sendOrderedBroadcast(intent, null);
    }
    public void start(){
        this.started = true;
    }
    public void stop(){
        this.stopped = true;
        this.activity = null;
        this.activityFromDeathListener = null;
    }
}