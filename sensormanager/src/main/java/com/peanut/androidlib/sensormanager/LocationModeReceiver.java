package com.peanut.androidlib.sensormanager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
class LocationModeReceiver extends BroadcastReceiver {
    private Context context;
    private IntentFilter intentFilter;
    private LocationTracker.LocationServiceListener locationServiceListener;
    private LocationTracker.HighAccuracyModeListener highAccuracyModeListener;
    private LocationTracker.BatterySavingModeListener batterySavingModeListener;
    private LocationTracker.SensorOnlyModeListener sensorOnlyModeListener;
    private LocationManager locationManager;
    private boolean locationServiceIsOn;
    private boolean running;
    LocationModeReceiver(Context context, LocationTracker.LocationServiceListener locationServiceListener, IntentFilter intentFilter) {
        this.context = context;
        this.intentFilter = intentFilter;
        this.locationServiceListener = locationServiceListener;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    void start() {
        running = true;
        context.registerReceiver(this, intentFilter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationServiceIsOn = locationManager.isLocationEnabled();
        } else {
            int serviceMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            locationServiceIsOn = serviceMode != Settings.Secure.LOCATION_MODE_OFF;
        }
    }
    void resume() {
        running = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationServiceIsOn = locationManager.isLocationEnabled();
        } else {
            int serviceMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            locationServiceIsOn = serviceMode != Settings.Secure.LOCATION_MODE_OFF;
        }
    }
    void pause() {
        running = false;
    }
    void stop() {
        unregisterListeners();
        context.unregisterReceiver(this);
        running = false;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LocationManager.MODE_CHANGED_ACTION.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (locationManager.isLocationEnabled()) {
                    if (!locationServiceIsOn) {
                        locationServiceIsOn = true;
                        locationServiceListener.onLocationServiceOn();
                        resume();
                    }
                } else {
                    if (!running) {
                        return;
                    }
                    if (locationServiceIsOn) {
                        locationServiceIsOn = false;
                        locationServiceListener.onLocationServiceOff();
                        pause();
                    }
                }
            } else {
                int serviceMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
                if (serviceMode != Settings.Secure.LOCATION_MODE_OFF) {
                    if (!locationServiceIsOn) {
                        locationServiceIsOn = true;
                        locationServiceListener.onLocationServiceOn();
                        resume();
                    }
                } else {
                    if (!running) {
                        return;
                    }
                    if (locationServiceIsOn) {
                        locationServiceIsOn = false;
                        locationServiceListener.onLocationServiceOff();
                        pause();
                    }
                }
                if (!locationServiceIsOn) {
                    return;
                }
                if (highAccuracyModeListener != null) {
                    int highAccuracyMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
                    if (highAccuracyMode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                        highAccuracyModeListener.onEnter();
                    } else {
                        highAccuracyModeListener.onExit();
                    }
                }
                if (batterySavingModeListener != null) {
                    int batterSavingMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_BATTERY_SAVING);
                    if (batterSavingMode == Settings.Secure.LOCATION_MODE_BATTERY_SAVING) {
                        batterySavingModeListener.onEnter();
                    } else {
                        batterySavingModeListener.onExit();
                    }
                }
                if (sensorOnlyModeListener != null) {
                    int sensorOnlyMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
                    if (sensorOnlyMode == Settings.Secure.LOCATION_MODE_SENSORS_ONLY) {
                        sensorOnlyModeListener.onEnter();
                    } else {
                        sensorOnlyModeListener.onExit();
                    }
                }
            }
        }
    }
    void unregisterListeners() {
        unregisterActionOnHighAccuracyMode();
        unregisterActionOnBatterySavingMode();
        unregisterActionOnSensorOnlyMode();
    }
    void registerActionOnHighAccuracyMode(LocationTracker.HighAccuracyModeListener highAccuracyModeListener) {
        this.highAccuracyModeListener = highAccuracyModeListener;
    }
    void registerActionOnBatterySavingMode(LocationTracker.BatterySavingModeListener batterySavingModeListener) {
        this.batterySavingModeListener = batterySavingModeListener;
    }
    void registerActionOnSensorOnlyMode(LocationTracker.SensorOnlyModeListener sensorOnlyModeListener) {
        this.sensorOnlyModeListener = sensorOnlyModeListener;
    }
    void unregisterActionOnHighAccuracyMode() {
        this.highAccuracyModeListener = null;
    }
    void unregisterActionOnBatterySavingMode() {
        this.batterySavingModeListener = null;
    }
    void unregisterActionOnSensorOnlyMode() {
        this.sensorOnlyModeListener = null;
    }
}
