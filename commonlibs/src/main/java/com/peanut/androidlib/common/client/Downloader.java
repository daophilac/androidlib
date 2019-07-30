package com.peanut.androidlib.common.client;
import android.Manifest;
import android.content.Context;

import com.peanut.androidlib.common.permissionmanager.PermissionInquirer;
import com.peanut.androidlib.common.worker.SingleWorker;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
public class Downloader {
    private static ConcurrentLinkedQueue<Downloader> downloaderConcurrentLinkedQueue = new ConcurrentLinkedQueue<>();
    private static int defaultRangeRequestSize = 1048576;
    private static int defaultDeleteAttempt = 100;
    private static int defaultDeleteFailDelay = 1000;
    private static int maxDownloaderCount = 10000;
    private String saveDirectory;
    private SingleWorker singleWorker;
    private long updateInterval = 1000;

    private String downloadUrl;
    private String fileName;
    private String fileType;
    private String filePath;
    private int fileSize;
    private boolean override;
    private boolean resumable;
    private File file;
    private FileOutputStream fileOutputStream;
    private int currentTotalBytes;
    private int speed;
    private State state;
    private Timer timerUpdater;
    private Timer timerSpeedCalculator;
    private DownloaderListener downloaderListener;
    public Downloader(String saveDirectory, String downloadUrl, String fileName, boolean override){
        if(saveDirectory == null){
            throw new IllegalArgumentException("saveDirectory cannot be null.");
        }
        if(downloadUrl == null){
            throw new IllegalArgumentException("downloadUrl cannot be null.");
        }
        setSaveDirectory(saveDirectory);
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.override = override;
        singleWorker = new SingleWorker();
        state = State.Initial;
    }
    public static void cancelAllDownloader(boolean deleteFiles){
        for(Downloader downloader : downloaderConcurrentLinkedQueue){
            downloader.cancel(deleteFiles);
        }
        downloaderConcurrentLinkedQueue.clear();
    }
    private static void refresherDownloaderList(){
        List<Downloader> list = new ArrayList<>();
        for(Downloader d : downloaderConcurrentLinkedQueue){
            if(d.state != State.AlreadyDownloaded && d.state != State.Done && d.state != State.Cancel && d.state != State.Failure){
                list.add(d);
            }
        }
        downloaderConcurrentLinkedQueue.clear();
        downloaderConcurrentLinkedQueue.addAll(list);
    }
    private void prepare(){
        if(downloaderListener == null){
            throw new RuntimeException("downloaderListener is null. Please set this listener first.");
        }
        if(state != State.Initial){
            return;
        }
        state = State.Preparing;
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Range", "bytes=" + currentTotalBytes + "-" + 100);
            if(httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
                resumable = true;
                fileSize = Integer.parseInt(httpURLConnection.getHeaderField("Content-Range").split("/")[1]);
            }
            else if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                resumable = false;
                fileSize = httpURLConnection.getContentLength();
            }
            else{
                if(downloaderListener != null){
                    downloaderListener.onFailure("Error connection with status code: " + httpURLConnection.getResponseCode());
                }
                state = State.Failure;
                return;
            }
            String contentDisposition = httpURLConnection.getHeaderField("Content-Disposition");
            fileType = httpURLConnection.getHeaderField("Content-Type");
            if(fileName == null){
                fileName = contentDisposition.split("; ")[1].split("=")[1];
            }
            filePath = saveDirectory + "/" + fileName;
            filePath = filePath.replace("//", "/");
            downloaderListener.onPrepared();
            file = new File(filePath);
            if(!isOverride()){
                if(file.exists()){
                    state = State.AlreadyDownloaded;
                    return;
                }
            }
            file = new File(saveDirectory);
            if(!file.exists()){
                file.getParentFile().mkdirs();
            }
            file = new File(filePath);
            try {
                if(!file.exists()){
                    file.createNewFile();
                }
                fileOutputStream = new FileOutputStream(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            state = State.Prepared;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void start(){
        if(downloaderListener == null){
            throw new RuntimeException("downloaderListener is null. Please set this listener first.");
        }
        if(state != State.Initial){
            return;
        }
        singleWorker.execute(this::prepare);
        singleWorker.execute(this::enterDownloadingState);
    }
    private void runTimers(){
        timerUpdater = new Timer();
        timerSpeedCalculator = new Timer();
        TimerTask timerTaskUpdater = new TimerTask() {
            @Override
            public void run() {
                float percent = (float) currentTotalBytes / fileSize;
                downloaderListener.onUpdatePercent(percent);
                downloaderListener.onUpdateBytes(currentTotalBytes);
                if(state == State.Done || state == State.Pausing || state == State.Cancel){
                    timerUpdater.cancel();
                }
            }
        };
        TimerTask timerTaskSpeedCalculator = new TimerTask() {
            private int nowBytes = currentTotalBytes;
            @Override
            public void run() {
                speed = currentTotalBytes - nowBytes;
                nowBytes = currentTotalBytes;
                if(state == State.Done || state == State.Pausing || state == State.Cancel){
                    speed = 0;
                    timerSpeedCalculator.cancel();
                }
            }
        };
        timerUpdater.schedule(timerTaskUpdater, 0, updateInterval);
        timerSpeedCalculator.schedule(timerTaskSpeedCalculator, 0, 1000);
    }
    private void enterDownloadingState(){
        if(state == State.AlreadyDownloaded){
            return;
        }
        state = State.Downloading;
        runTimers();
        downloaderConcurrentLinkedQueue.add(this);
        if(downloaderConcurrentLinkedQueue.size() > maxDownloaderCount){
            refresherDownloaderList();
        }
        if(isResumable()){
            resumableDownload();
        }
        else{
            nonResumableDownload();
        }
    }
    private void resumableDownload(){
        // TODO: Check reusable HttpURLConnection;
        try{
            while(state == State.Downloading){
                URL url = new URL(downloadUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Range", "bytes=" + currentTotalBytes + "-" + (currentTotalBytes + defaultRangeRequestSize));
                if(httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL){
                    state = State.Failure;
                    downloaderListener.onFailure("Error connection with status code: " + httpURLConnection.getResponseCode());
                    return;
                }
                else{
                    InputStream inputStream = httpURLConnection.getInputStream();
                    DataInputStream dataInputStream = new DataInputStream(inputStream);
                    byte[] buffer = new byte[defaultRangeRequestSize];
                    int length;
                    while((length = dataInputStream.read(buffer)) > 0){
                        fileOutputStream.write(buffer, 0, length);
                        currentTotalBytes += length;
                    }
                    dataInputStream.close();
                    inputStream.close();
                }
                if(currentTotalBytes >= fileSize){
                    fileOutputStream.close();
                    state = State.Done;
                    downloaderListener.onFinish();
                    return;
                }
            }
        } catch(IOException e){
            state = State.Failure;
            downloaderListener.onFailure("Exception: " + e.getMessage());
            return;
        }
    }
    private void nonResumableDownload(){
        try{
            URL url = new URL(downloadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            if(httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK){
                state = State.Failure;
                downloaderListener.onFailure("Error connection with status code: " + httpURLConnection.getResponseCode());
                return;
            }
            else{
                InputStream inputStream = httpURLConnection.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                byte[] buffer = new byte[defaultRangeRequestSize];
                int length;
                while((length = dataInputStream.read(buffer)) > 0){
                    fileOutputStream.write(buffer, 0, length);
                    currentTotalBytes += length;
                    if(currentTotalBytes >= fileSize){
                        fileOutputStream.close();
                        state = State.Done;
                        downloaderListener.onFinish();
                        break;
                    }
                    if(state == State.Cancel){
                        fileOutputStream.close();
                        downloaderListener.onCancel();
                        break;
                    }
                }
                dataInputStream.close();
                inputStream.close();
            }
        } catch(IOException e){
            state = State.Failure;
            downloaderListener.onFailure("Exception: " + e.getMessage());
            return;
        }
    }
    public void pause(){
        if(!isResumable()){
            return;
        }
        if(state != State.Downloading){
            return;
        }
        state = State.Pausing;
        downloaderListener.onPause();
    }
    public void resume(){
        if(state == State.Pausing){
            singleWorker.execute(this::enterDownloadingState);
            downloaderListener.onResume();
        }
    }
    public void cancel(boolean deleteFile){
        if(state == State.Downloading || state == State.Pausing){
            state = State.Cancel;
            if(deleteFile){
                file.delete();
            }
            downloaderListener.onCancel();
        }
    }
    public enum State{
        Initial, Preparing, Prepared, Downloading, Pausing, Done, Cancel, AlreadyDownloaded, Failure
    }
    public interface DownloaderListener {
        void onPrepared();
        void onPause();
        void onResume();
        void onFinish();
        void onCancel();
        void onUpdatePercent(float percent);
        void onUpdateBytes(long bytes);
        void onFailure(String message);
    }
    public void setDownloaderListener(DownloaderListener downloaderListener) {
        if(this.downloaderListener != null){
            throw new IllegalStateException("downloaderListener has already been set.");
        }
        this.downloaderListener = downloaderListener;
    }
    public String getSaveDirectory() {
        return saveDirectory;
    }
    public void setSaveDirectory(String saveDirectory) {
        if(saveDirectory == null){
            throw new IllegalArgumentException("saveDirectory cannot be null.");
        }
        this.saveDirectory = saveDirectory;
    }
    public static int getDefaultRangeRequestSize() {
        return defaultRangeRequestSize;
    }
    public static void setDefaultRangeRequestSize(int defaultRangeRequestSize) {
        Downloader.defaultRangeRequestSize = defaultRangeRequestSize;
    }
    public static int getDefaultDeleteAttempt() {
        return defaultDeleteAttempt;
    }
    public static void setDefaultDeleteAttempt(int defaultDeleteAttempt) {
        Downloader.defaultDeleteAttempt = defaultDeleteAttempt;
    }
    public static int getDefaultDeleteFailDelay() {
        return defaultDeleteFailDelay;
    }
    public static void setDefaultDeleteFailDelay(int defaultDeleteFailDelay) {
        Downloader.defaultDeleteFailDelay = defaultDeleteFailDelay;
    }
    public static int getMaxDownloaderCount() {
        return maxDownloaderCount;
    }
    public static void setMaxDownloaderCount(int maxDownloaderCount) {
        Downloader.maxDownloaderCount = maxDownloaderCount;
    }
    public long getUpdateInterval() {
        return updateInterval;
    }
    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }
    public String getDownloadUrl() {
        return downloadUrl;
    }
    public String getFileName() {
        return fileName;
    }
    public String getFileType() {
        return fileType;
    }
    public String getFilePath() {
        return filePath;
    }
    public int getFileSize() {
        return fileSize;
    }
    public boolean isOverride() {
        return override;
    }
    public boolean isResumable() {
        return resumable;
    }
    public int getSpeed() {
        return speed;
    }
    public State getState() {
        return state;
    }
}
