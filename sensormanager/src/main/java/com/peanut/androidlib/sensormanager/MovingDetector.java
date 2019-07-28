package com.peanut.androidlib.sensormanager;
import android.content.Context;
import android.location.Location;

import java.util.Locale;
public class MovingDetector {
    private Context context;
    private long minDistanceThreshold = 0;
    private boolean running = false;
    private LocationDetector locationDetector;
    private MovingDetector(Context context, LocationTracker.LocationServiceListener locationServiceListener) {
        this.context = context;
        this.locationDetector = new LocationDetector(locationServiceListener);
    }
    public static LocationDetector newInstance(Context context, LocationTracker.LocationServiceListener locationServiceListener) {
        return new MovingDetector(context, locationServiceListener).locationDetector;
    }
    public class LocationDetector {
        private static final String ACCURACY_RADIUS_FORMAT = "Accuracy radius: %f";
        private static final long DEFAULT_INTERVAL = 2000;
        private static final float DEFAULT_ACCURACY_RADIUS = 15;
        private boolean debug;
        private MovingDetectorListener movingDetectorListener;
        private MovingDetectorListenerDebug movingDetectorListenerDebug;
        private LocationTracker locationTracker;
        private Location currentLocation;
        private StateExceptionThrower stateExceptionThrower;
        private LocationDetector(LocationTracker.LocationServiceListener locationServiceListener) {
            locationTracker = new LocationTracker(context, locationServiceListener);
            locationTracker.setPriority(LocationTracker.Priority.HIGH_ACCURACY);
            locationTracker.setInterval(DEFAULT_INTERVAL);
            locationTracker.setMaxAccuracyRadiusThreshold(DEFAULT_ACCURACY_RADIUS);
            stateExceptionThrower = new StateExceptionThrower();
            stateExceptionThrower.validateInitial();
        }
        public void start(MovingDetectorListener movingDetectorListener) {
            if (stateExceptionThrower.getState() == StateExceptionThrower.State.START) {
                return;
            }
            stateExceptionThrower.validateStart();
            debug = false;
            running = true;
            this.movingDetectorListener = movingDetectorListener;
            locationTracker.start(location -> {
                if (currentLocation == null) {
                    currentLocation = location;
                    return;
                }
                float distance = currentLocation.distanceTo(location);
                if (distance >= minDistanceThreshold) {
                    movingDetectorListener.onMoved(distance, String.format(Locale.US, ACCURACY_RADIUS_FORMAT, location.getAccuracy()));
                    currentLocation = location;
                }
            });
        }
        public void startDebug(MovingDetectorListenerDebug movingDetectorListenerDebug) {
            if (stateExceptionThrower.getState() == StateExceptionThrower.State.START) {
                return;
            }
            stateExceptionThrower.validateStart();
            debug = true;
            running = true;
            this.movingDetectorListenerDebug = movingDetectorListenerDebug;
            locationTracker.start(location -> {
                if (currentLocation == null) {
                    currentLocation = location;
                    return;
                }
                movingDetectorListenerDebug.onUpdate(String.valueOf(location.getAccuracy()));
                float distance = currentLocation.distanceTo(location);
                if (distance >= minDistanceThreshold) {
                    movingDetectorListenerDebug.onMoved(distance, String.format(Locale.US, ACCURACY_RADIUS_FORMAT, location.getAccuracy()));
                    currentLocation = location;
                }
            });
        }
        public void resume() {
            if (stateExceptionThrower.getState() == StateExceptionThrower.State.RESUME) {
                return;
            }
            stateExceptionThrower.validateResume();
            locationTracker.resume();
        }
        public void pause() {
            if (stateExceptionThrower.getState() == StateExceptionThrower.State.PAUSE) {
                return;
            }
            stateExceptionThrower.validatePause();
            locationTracker.pause();
        }
        public void stop() {
            if (stateExceptionThrower.getState() == StateExceptionThrower.State.STOP) {
                return;
            }
            stateExceptionThrower.validateStop();
            locationTracker.stop();
            if (debug) {
                movingDetectorListenerDebug.onStop();
            } else {
                movingDetectorListener.onStop();
            }
            running = false;
        }
        public void checkLocationSetting(LocationTracker.OnLocationSettingResultListener onLocationSettingResultListener) {
            locationTracker.checkSelfLocationSettings(onLocationSettingResultListener);
        }
        public void setInterval(long interval) {
            locationTracker.setInterval(interval);
        }
        public void setMaxAccuracyRadius(float accuracyRadius) {
            locationTracker.setMaxAccuracyRadiusThreshold(accuracyRadius);
        }
        public void requestSelfLocationSettings(int requestCode) {
            locationTracker.requestSelfLocationSettings(requestCode);
        }
        public void requestLocationService(int requestCode) {
            LocationTracker.requestLocationService(context, requestCode);
        }
        public void requestHighAccuracyMode(int requestCode) {
            LocationTracker.requestHighAccuracyMode(context, requestCode);
        }
        public void requestBatterySavingMode(int requestCode) {
            LocationTracker.requestBatterySavingMode(context, requestCode);
        }
        public void requestSensorOnlyMode(int requestCode) {
            LocationTracker.requestSensorOnlyMode(context, requestCode);
        }
        public void registerHighAccuracyModeListener(LocationTracker.HighAccuracyModeListener highAccuracyModeListener) {
            locationTracker.registerHighAccuracyModeListener(highAccuracyModeListener);
        }
        public void registerBatterySavingModeListener(LocationTracker.BatterySavingModeListener batterySavingModeListener) {
            locationTracker.registerBatterySavingModeListener(batterySavingModeListener);
        }
        public void registerSensorOnlyModeListener(LocationTracker.SensorOnlyModeListener sensorOnlyModeListener) {
            locationTracker.registerSensorOnlyModeListener(sensorOnlyModeListener);
        }
        public void unregisterHighAccuracyModeListener() {
            locationTracker.unregisterHighAccuracyModeListener();
        }
        public void unregisterBatterySavingModeListener() {
            locationTracker.unregisterBatterySavingModeListener();
        }
        public void unregisterSensorOnlyModeListener() {
            locationTracker.unregisterSensorOnlyModeListener();
        }
        public void setMinDistanceThreshold(long minDistanceThreshold) {
            MovingDetector.this.setMinDistanceThreshold(minDistanceThreshold);
        }
        public boolean isRunning() {
            return MovingDetector.this.isRunning();
        }
    }
    private void setMinDistanceThreshold(long minDistanceThreshold) {
        this.minDistanceThreshold = minDistanceThreshold;
    }
    private boolean isRunning() {
        return running;
    }
    public interface MovingDetectorListener {
        void onMoved(float distance, String furtherDetails);
        void onStop();
    }
    public interface MovingDetectorListenerDebug {
        void onMoved(float distance, String furtherDetails);
        void onUpdate(String accuracy);
        void onStop();
    }
}