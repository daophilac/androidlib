package com.peanut.androidlib.common.worker;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class UIWorker extends HandlerThread {
    private static String DEFAULT_NAME = "UIWorker";
    private static List<UIWorker> listUIWorker = new ArrayList<>();
    private String name;
    private Handler handler;

    public UIWorker() {
        super(DEFAULT_NAME);
        listUIWorker.add(this);
        start();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public UIWorker(String name) {
        super(name);
        this.name = name;
        listUIWorker.add(this);
        start();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public UIWorker execute(Runnable task) {
        if (isAlive()) {
            this.handler.post(task);
            return this;
        } else {
            UIWorker uiWorker = new UIWorker(name);
            listUIWorker.add(uiWorker);
            return uiWorker.execute(task);
        }
    }

    @Override
    public boolean quit() {
        listUIWorker.remove(this);
        return super.quit();
    }

    @Override
    public boolean quitSafely() {
        listUIWorker.remove(this);
        return super.quitSafely();
    }

    public static void quitAllWorkers() {
        while(!listUIWorker.isEmpty()){
            listUIWorker.get(0).quit();
        }
    }

    public static void quitAllWorkersSafely() {
        while(!listUIWorker.isEmpty()){
            listUIWorker.get(0).quitSafely();
        }
    }
}
