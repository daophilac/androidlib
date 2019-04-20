package com.peanut.androidlib.sensormanager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

public class ShakeDetector {
    private Context context;
    private SensorManager sensorManager;
    private List<Sensor> listSensor;
    private Sensor sensor;
    private ShakeListener shakeListener;
    private boolean running;
    private SensorEventListener sensorEventListener;
    private long minInterval;
    private float minForce;
    private static boolean supported;
    private ShakeDetector(){}
    public static ShakeDetector newInstance(Context context){
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> listSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(listSensor.size() == 0){
            supported = false;
            return new ShakeDetector();
        }
        else{
            supported = true;
            ShakeDetector shakeDetector = new ShakeDetector();
            shakeDetector.context = context;
            shakeDetector.sensorManager = sensorManager;
            shakeDetector.listSensor = listSensor;
            shakeDetector.sensor = listSensor.get(0);
            shakeDetector.running = false;
            shakeDetector.configureDefault();
            return shakeDetector;
        }
    }
    public void start(ShakeListener shakeListener){
        if(!ShakeDetector.supported){
            shakeListener.onNoSupportDetection();
        }
        else{
            if(!this.running){
                shakeListener.onSupportDetection();
                this.running = true;
                this.shakeListener = shakeListener;
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
                        if(previous == 0){
                            previous = now;
                            previousX = nowX;
                            previousY = nowY;
                            previousZ = nowZ;
                        }
                        else{
                            if(now - previous > minInterval){
                                float force = Math.abs(nowX + nowY + nowZ - previousX - previousY - previousZ);
                                if(force > minForce){
                                    ShakeDetector.this.shakeListener.onAccelerationChange(nowX, nowY, nowZ);
                                    ShakeDetector.this.shakeListener.onShake(force);
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
    public void stop(){
        if(this.isRunning()){
            this.sensorManager.unregisterListener(this.sensorEventListener);
            this.shakeListener.onStopDetection();
        }
    }
    private void configureDefault(){
        this.minInterval = 200;
        this.minForce = 15;
    }
    public void configure(long minInterval, float minForce){
        this.minInterval = minInterval;
        this.minForce = minForce;
    }
    public boolean isSupported(){
        return supported;
    }
    public boolean isRunning(){
        return this.running;
    }
    public boolean checkSupport(Context context){
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> listSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(listSensor.size() == 0){
            supported = false;
            return false;
        }
        else{
            supported = true;
            return true;
        }
    }

    public interface ShakeListener {
        void onAccelerationChange(float x, float y, float z);
        void onShake(float force);
        void onSupportDetection();
        void onNoSupportDetection();
        void onStopDetection();
    }
}