package com.peanut.androidlib.viewmanager;

import android.os.Handler;
import android.os.Looper;

public abstract class ViewDecorator extends Thread{
    @Override
    public void run() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                onStart();
                onFinish();
            }
        });
    }

    public abstract void onStart();
    public abstract void onFinish();
}