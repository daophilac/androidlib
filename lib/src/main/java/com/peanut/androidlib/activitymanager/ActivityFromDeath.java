package com.peanut.androidlib.activitymanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import java.util.HashMap;

public class ActivityFromDeath extends SoulSummoner {
    private Activity activity;
    private ActivityFromDeathListener activityFromDeathListener;
    private boolean started;
    private boolean stopped;
    public ActivityFromDeath(){}
    public ActivityFromDeath(Activity activity){
        this.activity = activity;
        this.activityFromDeathListener = (ActivityFromDeathListener) activity;
        this.started = false;
        this.stopped = false;
        this.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        SoulSummoner.setActivityClass(activity.getClass());
    }
    public void sendOnStopSignal(){
        if(!this.started || this.stopped){
            return;
        }
        this.activity.finish();
        HashMap<String, Bundle> mapBundleActivity = this.activityFromDeathListener.buildBundle();
        setMapBundleActivity(mapBundleActivity);
        Intent intent = new Intent(this.activity, ActivityFromDeath.class);
        intent.setAction(SoulSummoner.ActivitySignal.ON_STOP);
        this.activity.sendOrderedBroadcast(intent, null);
    }
    public void sendOnResumeSignal(){
        if(!this.started || this.stopped){
            return;
        }
        this.activityFromDeathListener.getBundle();
        Intent intent = new Intent(activity, ActivityFromDeath.class);
        intent.setAction(SoulSummoner.ActivitySignal.ON_RESUME);
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
    public interface ActivityFromDeathListener {
        HashMap<String, Bundle> buildBundle();
        void getBundle();
    }
}