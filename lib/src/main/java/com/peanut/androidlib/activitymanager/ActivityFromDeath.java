package com.peanut.androidlib.activitymanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import java.util.HashMap;

public class ActivityFromDeath extends SoulSummoner {
    private static OnActivityFromDeathListener onActivityFromDeathListener;
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
        HashMap<String, Bundle> mapBundleActivity = onActivityFromDeathListener.onTimeToBuildBundle();
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
            onActivityFromDeathListener.onTimeToGetBundle(getMapBundleActivity());
        }
        Intent intent = new Intent(activity, ActivityFromDeath.class);
        intent.setAction(SoulSummoner.ActivitySignal.ON_RESUME.getValue());
        activity.sendOrderedBroadcast(intent, null);
    }
    public static void start(OnActivityFromDeathListener onActivityFromDeathListener){
        ActivityFromDeath.onActivityFromDeathListener = onActivityFromDeathListener;
        if(!started){
            onActivityFromDeathListener.onTimeToInitialize();
            started = true;
        }
    }
    public static void stop(){
        started = false;
        stopped = true;
        onActivityFromDeathListener = null;
    }
    public interface OnActivityFromDeathListener {
        void onTimeToInitialize();
        HashMap<String, Bundle> onTimeToBuildBundle();
        void onTimeToGetBundle(HashMap<String, Bundle> mapBundle);
    }
}