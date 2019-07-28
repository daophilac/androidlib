package com.peanut.androidlib.sensormanager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;
public class ShakeDetector {
    private ShakeDetectorListener shakeDetectorListener;
    private SensorManager sensorManager;
    private Sensor sensor;
    private boolean running;
    private SensorEventListener sensorEventListener;
    private long minInterval;
    private float minForce;
    private boolean supported;
    public ShakeDetector(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> listSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (listSensor.size() == 0) {
            this.supported = false;
        } else {
            this.supported = true;
            this.sensorManager = sensorManager;
            this.sensor = listSensor.get(0);
            this.running = false;
            this.configureDefault();
        }
    }
    public void start(ShakeDetectorListener shakeDetectorListener) {
        this.shakeDetectorListener = shakeDetectorListener;
        if (!supported) {
            shakeDetectorListener.onNoSupportDetection();
        } else {
            shakeDetectorListener.onSupportDetection();
            if (!this.running) {
                this.running = true;
                this.sensorEventListener = new SensorEventListener() {
                    private long now;
                    private long previous = 0;
                    private float nowX;
                    private float nowY;
                    private float nowZ;
                    private float previousX;
                    private float previousY;
                    private float previousZ;
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        now = event.timestamp;
                        nowX = event.values[0];
                        nowY = event.values[1];
                        nowZ = event.values[2];
                        if (previous == 0) {
                            previous = now;
                            previousX = nowX;
                            previousY = nowY;
                            previousZ = nowZ;
                        } else {
                            if (now - previous > minInterval) {
                                float force = Math.abs(nowX + nowY + nowZ - previousX - previousY - previousZ);
                                if (force > minForce) {
                                    shakeDetectorListener.onAccelerationChange(nowX, nowY, nowZ);
                                    shakeDetectorListener.onShake(force);
                                    previous = now;
                                    previousX = nowX;
                                    previousY = nowY;
                                    previousZ = nowZ;
                                }
                            }
                        }
                    }
                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                };
                this.sensorManager.registerListener(this.sensorEventListener, this.sensor, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }
    public void stop() {
        if (this.isRunning()) {
            this.sensorManager.unregisterListener(this.sensorEventListener);
            this.shakeDetectorListener.onStopDetection();
        }
    }
    private void configureDefault() {
        this.minInterval = 200;
        this.minForce = 15;
    }
    public void configure(long minInterval, float minForce) {
        this.minInterval = minInterval;
        this.minForce = minForce;
    }
    public boolean isSupported() {
        return supported;
    }
    public boolean isRunning() {
        return this.running;
    }
    public interface ShakeDetectorListener {
        void onAccelerationChange(float x, float y, float z);
        void onShake(float force);
        void onSupportDetection();
        void onNoSupportDetection();
        void onStopDetection();
    }
}