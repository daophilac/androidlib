package com.peanut.androidlib.worker;

import android.os.Handler;
import android.os.HandlerThread;

public class SingleWorker extends HandlerThread {
    private Handler handler;
    public SingleWorker(String name) {
        super(name);
        start();
        this.handler = new Handler(getLooper());
    }
    public SingleWorker execute(Runnable task){
        this.handler.post(task);
        return this;
    }
}
