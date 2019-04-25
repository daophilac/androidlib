package com.peanut.androidlib.viewmanager;

public abstract class ViewFinder extends Thread {
    @Override
    public void run() {
        onStart();
        onFinish();
    }

    public abstract void onStart();
    public abstract void onFinish();
}