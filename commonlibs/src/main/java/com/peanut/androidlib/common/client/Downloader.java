package com.peanut.androidlib.common.client;
import com.peanut.androidlib.common.worker.SingleWorker;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
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
    private OnDownloadListener onDownloadListener;
    private OnDoneListener onDoneListener;
    private OnPauseListener onPauseListener;
    private OnCancelListener onCancelListener;
    private OnUpdateProgressListener onUpdateProgressListener;
    private OnUpdateSpeedListener onUpdateSpeedListener;
    private OnHttpFailListener onHttpFailListener;
    private OnExceptionListener onExceptionListener;
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
        if(state != State.Initial){
            return;
        }
        state = State.Preparing;
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Range", "bytes=0-100");
            if(httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
                resumable = true;
                fileSize = Integer.parseInt(httpURLConnection.getHeaderField("Content-Range").split("/")[1]);
            }
            else if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                resumable = false;
                fileSize = httpURLConnection.getContentLength();
            }
            else{
                state = State.Failure;
                if(onHttpFailListener != null){
                    onHttpFailListener.onHttpFail(httpURLConnection);
                }
                return;
            }
            String contentDisposition = httpURLConnection.getHeaderField("Content-Disposition");
            fileType = httpURLConnection.getHeaderField("Content-Type");
            if(fileName == null){
                fileName = contentDisposition.split("; ")[1].split("=")[1];
            }
            filePath = saveDirectory + "/" + fileName;
            filePath = filePath.replace("//", "/");
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
            currentTotalBytes = 0;
            state = State.Prepared;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void start(){
        if(state != State.Initial){
            return;
        }
        singleWorker.execute(this::prepare);
        singleWorker.execute(this::enterDownloadingState);
    }
    private void runTimers(){
        if(onUpdateProgressListener != null){
            timerUpdater = new Timer();
            TimerTask timerTaskUpdater = new TimerTask() {
                @Override
                public void run() {
                    float percent = (float) currentTotalBytes / fileSize;
                    onUpdateProgressListener.onUpdateProgress(percent, currentTotalBytes);
                    if(state == State.Done || state == State.Pausing || state == State.Cancel){
                        timerUpdater.cancel();
                    }
                }
            };
            timerUpdater.schedule(timerTaskUpdater, 0, updateInterval);
        }
        timerSpeedCalculator = new Timer();
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
                if(onUpdateSpeedListener != null){
                    onUpdateSpeedListener.onUpdateSpeed(speed);
                }
            }
        };
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
        if(onDownloadListener != null){
            onDownloadListener.onDownload();
        }
        try{
            while(state == State.Downloading){
                URL url = new URL(downloadUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Range", "bytes=" + currentTotalBytes + "-" + (currentTotalBytes + defaultRangeRequestSize));
                if(httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL){
                    state = State.Failure;
                    if(onHttpFailListener != null){
                        onHttpFailListener.onHttpFail(httpURLConnection);
                    }
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
                    if(onDoneListener != null){
                        onDoneListener.onDone();
                    }
                    return;
                }
            }
        } catch (ProtocolException e) {
            state = State.Exception;
            if(onExceptionListener != null){
                onExceptionListener.onException(e);
            }
        } catch (MalformedURLException e) {
            state = State.Exception;
            if(onExceptionListener != null){
                onExceptionListener.onException(e);
            }
        } catch (IOException e) {
            state = State.Exception;
            if(onExceptionListener != null){
                onExceptionListener.onException(e);
            }
        }
    }
    private void nonResumableDownload(){
        if(onDownloadListener != null){
            onDownloadListener.onDownload();
        }
        try{
            URL url = new URL(downloadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            if(httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK){
                state = State.Failure;
                if(onHttpFailListener != null){
                    onHttpFailListener.onHttpFail(httpURLConnection);
                }
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
                        if(onDoneListener != null){
                            onDoneListener.onDone();
                        }
                        break;
                    }
                    if(state == State.Cancel){
                        fileOutputStream.close();
                        if(onCancelListener != null){
                            onCancelListener.onCancel();
                        }
                        break;
                    }
                }
                dataInputStream.close();
                inputStream.close();
            }
        } catch (ProtocolException e) {
            state = State.Failure;
            if(onExceptionListener != null){
                onExceptionListener.onException(e);
            }
        } catch (MalformedURLException e) {
            state = State.Failure;
            if(onExceptionListener != null){
                onExceptionListener.onException(e);
            }
        } catch (IOException e) {
            state = State.Failure;
            if(onExceptionListener != null){
                onExceptionListener.onException(e);
            }
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
        if(onPauseListener != null){
            onPauseListener.onPause();
        }
    }
    public void resume(){
        if(state == State.Pausing){
            singleWorker.execute(this::enterDownloadingState);
        }
    }
    public void cancel(boolean deleteFile){
        if(state == State.Downloading || state == State.Pausing) {
            state = State.Cancel;
            if(deleteFile){
                file.delete();
            }
            if(onCancelListener != null){
                onCancelListener.onCancel();
            }
        }
    }
    public void reDownload(){
        cancel(false);
        state = State.Initial;
        start();
    }
    public enum State{
        Initial, Preparing, Prepared, Downloading, Pausing, Done, Cancel, AlreadyDownloaded, Failure, Exception
    }
    public interface OnDownloadListener{
        void onDownload();
    }
    public interface OnDoneListener{
        void onDone();
    }
    public interface OnPauseListener{
        void onPause();
    }
    public interface OnCancelListener{
        void onCancel();
    }
    public interface OnUpdateProgressListener{
        void onUpdateProgress(float percent, int currentTotalByte);
    }
    public interface OnUpdateSpeedListener{
        void onUpdateSpeed(int speed);
    }
    public interface OnHttpFailListener{
        void onHttpFail(HttpURLConnection httpURLConnection);
    }
    public interface OnExceptionListener{
        void onException(Exception e);
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
    public Downloader setOnDownloadListener(OnDownloadListener onDownloadListener) {
        this.onDownloadListener = onDownloadListener;
        return this;
    }
    public Downloader setOnDoneListener(OnDoneListener onDoneListener) {
        this.onDoneListener = onDoneListener;
        return this;
    }
    public Downloader setOnPauseListener(OnPauseListener onPauseListener) {
        this.onPauseListener = onPauseListener;
        return this;
    }
    public Downloader setOnCancelListener(OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
        return this;
    }
    public Downloader setOnUpdateProgressListener(OnUpdateProgressListener onUpdateProgressListener) {
        this.onUpdateProgressListener = onUpdateProgressListener;
        return this;
    }
    public Downloader setOnUpdateSpeedListener(OnUpdateSpeedListener onUpdateSpeedListener) {
        this.onUpdateSpeedListener = onUpdateSpeedListener;
        return this;
    }
    public Downloader setOnHttpFailListener(OnHttpFailListener onHttpFailListener) {
        this.onHttpFailListener = onHttpFailListener;
        return this;
    }
    public Downloader setOnExceptionListener(OnExceptionListener onExceptionListener) {
        this.onExceptionListener = onExceptionListener;
        return this;
    }
}
