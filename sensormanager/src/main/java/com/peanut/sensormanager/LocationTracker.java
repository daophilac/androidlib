package com.peanut.sensormanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.peanut.commonlibs.permissionmanager.PermissionInquirer;

public class LocationTracker {
    private static final String ACCESS_FINE_LOCATION_PERMISSION_HAS_NOT_BEEN_GRANTED = "Access fine location permission has not been granted";
    private static final String ACCESS_COARSE_LOCATION_PERMISSION_HAS_NOT_BEEN_GRANTED = "Access coarse location permission has not been granted";
    private static final String LOCATION_SETTING_IS_NOT_SATISFIED = "Location settings is not satisfied";
    private Context context;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LocationModeReceiver locationModeReceiver;
    private OnLocationUpdateListener onLocationUpdateListener;
    private float maxAccuracyRadiusThreshold;
    private StateExceptionThrower stateExceptionThrower;
    public LocationTracker(Context context, LocationServiceListener locationServiceListener){
        PermissionInquirer permissionInquirer = new PermissionInquirer(context);
        if(!permissionInquirer.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)){
            throw new RuntimeException(ACCESS_FINE_LOCATION_PERMISSION_HAS_NOT_BEEN_GRANTED);
        }
        if(!permissionInquirer.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)){
            throw new RuntimeException(ACCESS_COARSE_LOCATION_PERMISSION_HAS_NOT_BEEN_GRANTED);
        }
        this.stateExceptionThrower = new StateExceptionThrower();
        this.context = context;
        this.maxAccuracyRadiusThreshold = 5;
        fusedLocationProviderClient = new FusedLocationProviderClient(context);
        locationRequest = LocationRequest.create();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
        locationModeReceiver = new LocationModeReceiver(context, locationServiceListener, intentFilter);
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult == null){
                    return;
                }
                for(Location location : locationResult.getLocations()){
                    if(location.getAccuracy() <= maxAccuracyRadiusThreshold){
                        onLocationUpdateListener.onLocationUpdate(location);
                    }
                }
            }
        };
        stateExceptionThrower.validateInitial();
    }
    public void checkSelfLocationSettings(OnLocationSettingResultListener onLocationSettingResultListener){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(context);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        task.addOnSuccessListener((Activity) context, locationSettingsResponse -> onLocationSettingResultListener.onSatisfiedSetting());
        task.addOnFailureListener((Activity) context, onLocationSettingResultListener::onUnsatisfiedSetting);
    }
    public void requestSelfLocationSettings(int requestCode){
        requestSettings(context, locationRequest, requestCode);
    }
    public static void requestLocationService(Context context, int requestCode){
        requestSettings(context, LocationRequest.create(), requestCode);
    }
    public static void requestHighAccuracyMode(Context context, int requestCode){
        requestSettings(context, new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY), requestCode);
    }
    public static void requestBatterySavingMode(Context context, int requestCode){
        requestSettings(context, new LocationRequest().setPriority(LocationRequest.PRIORITY_LOW_POWER), requestCode);
    }
    public static void requestSensorOnlyMode(Context context, int requestCode){
        requestSettings(context, new LocationRequest().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY), requestCode);
    }
    private static void requestSettings(Context context, LocationRequest locationRequest, int requestCode){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(context);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        task.addOnFailureListener((Activity) context, e -> {
            try {
                ResolvableApiException resolvableApiException = (ResolvableApiException)e;
                resolvableApiException.startResolutionForResult((Activity) context, requestCode);
            } catch (IntentSender.SendIntentException e1) {
                e1.printStackTrace();
            }
        });
    }


    @SuppressLint("MissingPermission")
    public void start(OnLocationUpdateListener onLocationUpdateListener){
        stateExceptionThrower.validateStart();
        checkSelfLocationSettings(new OnLocationSettingResultListener() {
            @Override
            public void onSatisfiedSetting() {
                LocationTracker.this.onLocationUpdateListener = onLocationUpdateListener;
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                locationModeReceiver.start();
            }
            @Override
            public void onUnsatisfiedSetting(Exception e) {
                throw new RuntimeException(LOCATION_SETTING_IS_NOT_SATISFIED);
            }
        });
    }
    @SuppressLint("MissingPermission")
    public void resume(){
        stateExceptionThrower.validateResume();
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        locationModeReceiver.resume();
    }
    public void pause(){
        stateExceptionThrower.validatePause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        locationModeReceiver.pause();
    }
    public void stop(){
        stateExceptionThrower.validateStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        locationModeReceiver.stop();
    }
    public void setMaxAccuracyRadiusThreshold(float maxAccuracyRadiusThreshold) {
        this.maxAccuracyRadiusThreshold = maxAccuracyRadiusThreshold;
    }
    public void setPriority(Priority priority){
        switch (priority){
            case NO_POWER:
                locationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
                break;
            case LOW_POWER:
                locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                break;
            case BALANCED_POWER_ACCURACY:
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                break;
            case HIGH_ACCURACY:
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                break;
        }
    }
    public void setInterval(long interval){
        locationRequest.setInterval(interval);
    }
    public void setFastestInterval(long fastestInterval){
        locationRequest.setFastestInterval(fastestInterval);
    }
    public void setNumUpdates(int numUpdates){
        locationRequest.setNumUpdates(numUpdates);
    }
    public void registerHighAccuracyModeListener(HighAccuracyModeListener highAccuracyModeListener){
        this.locationModeReceiver.registerActionOnHighAccuracyMode(highAccuracyModeListener);
    }
    public void registerBatterySavingModeListener(BatterySavingModeListener batterySavingModeListener){
        this.locationModeReceiver.registerActionOnBatterySavingMode(batterySavingModeListener);
    }
    public void registerSensorOnlyModeListener(SensorOnlyModeListener sensorOnlyModeListener){
        this.locationModeReceiver.registerActionOnSensorOnlyMode(sensorOnlyModeListener);
    }
    public void unregisterHighAccuracyModeListener(){
        this.locationModeReceiver.unregisterActionOnHighAccuracyMode();
    }
    public void unregisterBatterySavingModeListener(){
        this.locationModeReceiver.unregisterActionOnBatterySavingMode();
    }
    public void unregisterSensorOnlyModeListener(){
        this.locationModeReceiver.unregisterActionOnSensorOnlyMode();
    }
    public interface OnLocationSettingResultListener {
        void onSatisfiedSetting();
        void onUnsatisfiedSetting(Exception e);
    }
    public interface OnLocationUpdateListener {
        void onLocationUpdate(Location location);
    }
    public interface LocationServiceListener {
        void onLocationServiceOff();
        void onLocationServiceOn();
    }
    public interface HighAccuracyModeListener{
        void onEnter();
        void onExit();
    }
    public interface BatterySavingModeListener{
        void onEnter();
        void onExit();
    }
    public interface SensorOnlyModeListener {
        void onEnter();
        void onExit();
    }
    public enum Priority{
        NO_POWER, LOW_POWER, BALANCED_POWER_ACCURACY, HIGH_ACCURACY
    }
}