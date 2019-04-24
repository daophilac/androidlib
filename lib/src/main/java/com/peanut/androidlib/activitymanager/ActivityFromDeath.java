package com.peanut.androidlib.activitymanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;

public class ActivityFromDeath extends SoulSummoner {
    private Activity activity;
    private ActivityListenerFromDeathListener activityListenerFromDeathListener;
    public interface ActivityListenerFromDeathListener{
        HashMap<String, Bundle> buildBundle();
    }
    public ActivityFromDeath(){}
    public ActivityFromDeath(Activity activity){
        this.activity = activity;
        this.activityListenerFromDeathListener = (ActivityListenerFromDeathListener) activity;
        SoulSummoner.setActivityClass(activity.getClass());
    }
    public void sendOnStopSignal(){
        HashMap<String, Bundle> mapBundleActivity = this.activityListenerFromDeathListener.buildBundle();
        SoulSummoner.setMapBundleActivity(mapBundleActivity);
        Intent intent = new Intent(activity, ActivityFromDeath.class);
        intent.setAction(ActivitySignal.ON_STOP);
        activity.sendOrderedBroadcast(intent, null);
    }
    public void sendOnDestroySignal(){
        Intent intent = new Intent(activity, ActivityFromDeath.class);
        intent.setAction(ActivitySignal.ON_DESTROY);
        activity.sendOrderedBroadcast(intent, null);
    }
    public void sendOnResumeSignal(){
        Intent intent = new Intent(activity, ActivityFromDeath.class);
        intent.setAction(ActivitySignal.ON_RESUME);
        activity.sendOrderedBroadcast(intent, null);
    }
    public void sendOnCreateSignal(){
        Intent intent = new Intent(activity, ActivityFromDeath.class);
        intent.setAction(ActivitySignal.ON_CREATE);
        activity.sendOrderedBroadcast(intent, null);
    }
}