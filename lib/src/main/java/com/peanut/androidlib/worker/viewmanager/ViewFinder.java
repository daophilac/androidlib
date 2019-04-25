package com.peanut.androidlib.worker.viewmanager;

public abstract class ViewFinder extends Thread {
    @Override
    public void run() {
        onStart();
        onFinish();
    }

    public abstract void onStart();
    public abstract void onFinish();
}