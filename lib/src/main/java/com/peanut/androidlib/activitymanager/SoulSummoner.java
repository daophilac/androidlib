package com.peanut.androidlib.activitymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;


class SoulSummoner extends BroadcastReceiver {
    private static Class activityClass;
    private static SoulSummonerListener soulSummonerListener;
    private static HashMap<String, Bundle> mapBundleActivity;
    private static State state;
    private Incantation incantation;
    private Thread threadReviver;
    public SoulSummoner(){}
    public static void setActivityClass(Class activityClass){
        SoulSummoner.activityClass = activityClass;
    }
    public static void setMapBundleActivity(HashMap<String, Bundle> mapBundleActivity){
        SoulSummoner.mapBundleActivity = mapBundleActivity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ActivitySignal.ON_STOP)){
            state = State.ON_STOP;
            if(soulSummonerListener == null){
                Intent intentRevive = new Intent(context, activityClass);
                for (Map.Entry<String, Bundle> pair : mapBundleActivity.entrySet()) {
                    intentRevive.putExtra(pair.getKey(), pair.getValue());
                }
                intentRevive.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                incantation = new Incantation(context, intentRevive);
                soulSummonerListener = incantation;
                this.threadReviver = new Thread(incantation);
                this.threadReviver.start();
            }
        }
        else if(intent.getAction().equals(ActivitySignal.ON_RESUME)){
            if(state == State.ON_STOP){
                // Safely stop the reviver;
                if(soulSummonerListener != null){
                    soulSummonerListener.onStop();
                    state = State.DONE;
                    soulSummonerListener = null;
                }
            }
        }
    }
    private class Incantation implements Runnable, SoulSummonerListener {
        @Override
        public void onStop() {
            this.stop = true;
        }
        private Context context;
        private Intent intent;
        private boolean stop;
        private Incantation(Context context, Intent intent){
            this.context = context;
            this.intent = intent;
            this.stop = false;
        }
        @Override
        public void run() {
            while(!stop){
                context.startActivity(intent);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private interface SoulSummonerListener {
        void onStop();
    }
    private enum State{
        ON_STOP, DONE
    }
    static final class ActivitySignal{
        static final String ON_STOP = "onStop";
        static final String ON_RESUME = "onResume";
    }
}