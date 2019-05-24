package com.peanut.androidlib.common.worker;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.List;

public class SingleWorker extends HandlerThread {
    private static String DEFAULT_NAME = "SingleWorker";
    private static List<SingleWorker> listSingleWorker = new ArrayList<>();
    private String name;
    private Handler handler;
    public SingleWorker(){
        super(DEFAULT_NAME);
        listSingleWorker.add(this);
        start();
        this.handler = new Handler(getLooper());
    }
    public SingleWorker(String name) {
        super(name);
        this.name = name;
        listSingleWorker.add(this);
        start();
        this.handler = new Handler(getLooper());
    }
    public SingleWorker execute(Runnable task){
        if(isAlive()){
            this.handler.post(task);
            return this;
        }
        else{
            SingleWorker singleWorker = new SingleWorker(name);
            listSingleWorker.add(singleWorker);
            return singleWorker.execute(task);
        }
    }

    @Override
    public boolean quit() {
        listSingleWorker.remove(this);
        return super.quit();
    }

    @Override
    public boolean quitSafely() {
        listSingleWorker.remove(this);
        return super.quitSafely();
    }

    public static void quitAllWorkers(){
        for(SingleWorker singleWorker : listSingleWorker){
            singleWorker.quit();
        }
        listSingleWorker = new ArrayList<>();
    }
    public static void quitAllWorkersSafely(){
        for(SingleWorker singleWorker : listSingleWorker){
            singleWorker.quitSafely();
        }
        listSingleWorker = new ArrayList<>();
    }
}
