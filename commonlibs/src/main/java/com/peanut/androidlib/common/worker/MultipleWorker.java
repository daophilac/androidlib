package com.peanut.androidlib.common.worker;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

public class MultipleWorker {
    private static final String DEFAULT_NAME = "MultipleWorker";
    private static List<MultipleWorker> listMultipleWorker = new ArrayList<>();
    private String name;
    private int numberOfWorker;
    private List<Handler> listHandler;
    private List<SingleWorker> listSingleWorker;
    private int autoIncrementId;
    private int busyWorkerSelector;
    private boolean quit;
    public MultipleWorker(){
        this.name = DEFAULT_NAME;
        this.numberOfWorker = 0;
        initializeGlobalVariables();
        prepare();
    }
    public MultipleWorker(String name){
        this.name = name;
        this.numberOfWorker = 0;
        initializeGlobalVariables();
        prepare();
    }
    public MultipleWorker(String name, int numberOfWorker){
        this.name = name;
        this.numberOfWorker = numberOfWorker;
        initializeGlobalVariables();
        prepare();
    }
    public MultipleWorker(int numberOfWorker){
        this.name = DEFAULT_NAME;
        this.numberOfWorker = numberOfWorker;
        initializeGlobalVariables();
        prepare();
    }
    public void addMoreWorker(int amount){
        this.numberOfWorker += amount;
        for(int i = 0; i < this.numberOfWorker; i++){
            this.listSingleWorker.add(new SingleWorker(this.name + this.autoIncrementId));
        }
    }
    private void initializeGlobalVariables(){
        this.autoIncrementId = 1;
        this.busyWorkerSelector = 0;
    }
    private void prepare(){
        this.quit = false;
        this.listHandler = new ArrayList<>();
        this.listSingleWorker = new ArrayList<>();
        for(int i = 0; i < this.numberOfWorker; i++){
            this.listSingleWorker.add(new SingleWorker(this.name + this.autoIncrementId));
        }
    }
    public MultipleWorker execute(Runnable task){
        if(!this.quit){
            boolean allAreBusy = true;
            for(SingleWorker sw : this.listSingleWorker){
                if(!sw.isBusy()){
                    sw.enqueueWork(task, this.autoIncrementId++);
                    allAreBusy = false;
                    break;
                }
            }
            if(allAreBusy){
                this.listSingleWorker.get(this.busyWorkerSelector++ % this.numberOfWorker).enqueueWork(task, ++this.autoIncrementId);
            }
            return this;
        }
        else{
            MultipleWorker multipleWorker = new MultipleWorker(name, numberOfWorker);
            listMultipleWorker.add(multipleWorker);
            multipleWorker.prepare();
            return multipleWorker.execute(task);
        }
    }
    public void quit(){
        for(SingleWorker sw : this.listSingleWorker){
            sw.quit();
        }
        this.quit = true;
        listMultipleWorker.remove(this);
    }
    public void quitSafely(){
        for(SingleWorker sw : this.listSingleWorker){
            sw.quitSafely();
        }
        this.quit = true;
        listMultipleWorker.remove(this);
    }

    private class SingleWorker extends HandlerThread{
        private Handler handler;
        private int mostCurrentRunnableId;
        private SingleWorker(String name) {
            super(name);
            start();
            this.handler = new Handler(getLooper());
            listHandler.add(this.handler);
        }
        private void enqueueWork(Runnable runnable, int runnableId){
            this.mostCurrentRunnableId = runnableId;
            Message message = Message.obtain(this.handler, runnable);
            message.what = runnableId;
            this.handler.sendMessage(message);
            this.handler.sendEmptyMessage(runnableId);
        }
        private boolean isBusy(){
            return this.handler.hasMessages(mostCurrentRunnableId);
        }
    }
}