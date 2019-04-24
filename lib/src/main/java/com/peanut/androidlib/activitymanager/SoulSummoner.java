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
            // But we don't have a clue whether the activity will be destroyed too.
            state = State.NEED_TO_DETERMINE;

            // So we revive the activity if we have not done that yet.
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
        else if(intent.getAction().equals(ActivitySignal.ON_DESTROY)){
            // The activity is actually be destroyed in this case.
            state = State.WAIT_FOR_CREATE;

            // We don't need to run a reviver because the onStop has taken care of it.
        }
        else if(intent.getAction().equals(ActivitySignal.ON_RESUME)){
            // We are going to stop the reviver. Are we?
            // Note that if the activity is not destroyed, then we can stop it easily.
            // But if it is actually be destroyed, then we still have to wait until the onCreate have called.
            // However, we can see that we are sure to tell whether the activity is destroyed or not,
            // based on the stateCurrent.
            if(state == State.NEED_TO_DETERMINE){
                // Safely stop the reviver;
                if(soulSummonerListener != null){
                    soulSummonerListener.onStop();
                    state = State.DONE;
                    soulSummonerListener = null;
                }
            }
            else if(state == State.WAIT_FOR_CREATE){
                // Do nothing, just waiting until onCreate have called
            }
        }
        else if(intent.getAction().equals(ActivitySignal.ON_CREATE)){
            // The onCreate can be a mess because it it called not only after we revive
            // the activity from its death, but also after the OS create the activity from its birth.
            // So to determine whether we should do something or not, we have to check the stateCurrent.
            if(state == State.WAIT_FOR_CREATE){
                // Safely stop the reviver;
                if(soulSummonerListener != null){
                    soulSummonerListener.onStop();
                    state = State.DONE;
                    soulSummonerListener = null;
                }
            }
            else if(state == State.DONE){
                // Do nothing of course.
            }
        }
    }

    public class Incantation implements Runnable, SoulSummonerListener {
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
    public interface SoulSummonerListener {
        void onStop();
    }
    private enum State{
        WAIT_FOR_CREATE, NEED_TO_DETERMINE, DONE
    }
    static final class ActivitySignal{
        static final String ON_STOP = "onStop";
        static final String ON_DESTROY = "onDestroy";
        static final String ON_RESUME = "onResume";
        static final String ON_CREATE = "onCreate";
    }
}