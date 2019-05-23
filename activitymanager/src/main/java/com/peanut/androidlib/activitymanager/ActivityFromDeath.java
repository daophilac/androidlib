package com.peanut.androidlib.activitymanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.WindowManager;

import java.util.HashMap;

public class ActivityFromDeath extends SoulSummoner {
    private static ActivityFromDeathListener activityFromDeathListener;
    private static boolean started;
    private static boolean stopped;
    public ActivityFromDeath(){ }
    public static void sendOnCreateSignal(Activity activity){
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        SoulSummoner.setActivityClass(activity.getClass());
    }
    public static void sendOnStopSignal(Activity activity){
        if(!started || stopped){
            return;
        }
        activity.finish();
        HashMap<String, Bundle> mapBundleActivity = activityFromDeathListener.onTimeToBuildBundle();
        setMapBundleActivity(mapBundleActivity);
        Intent intent = new Intent(activity, ActivityFromDeath.class);
        intent.setAction(SoulSummoner.ActivitySignal.ON_STOP.getValue());
        activity.sendOrderedBroadcast(intent, null);
    }
    public static void sendOnResumeSignal(Activity activity){
        if(!started || stopped){
            return;
        }
        if(getMapBundleActivity() != null){
            activityFromDeathListener.onTimeToGetBundle(getMapBundleActivity());
        }
        Intent intent = new Intent(activity, ActivityFromDeath.class);
        intent.setAction(SoulSummoner.ActivitySignal.ON_RESUME.getValue());
        activity.sendOrderedBroadcast(intent, null);
    }
    public static void start(ActivityFromDeathListener activityFromDeathListener){
        ActivityFromDeath.activityFromDeathListener = activityFromDeathListener;
        if(!started){
            activityFromDeathListener.onTimeToInitialize();
            started = true;
        }
    }
    public static void stop(){
        started = false;
        stopped = true;
        activityFromDeathListener = null;
    }
    public interface ActivityFromDeathListener {
        void onTimeToInitialize();
        HashMap<String, Bundle> onTimeToBuildBundle();
        void onTimeToGetBundle(@NonNull HashMap<String, Bundle> mapBundle);
    }
}