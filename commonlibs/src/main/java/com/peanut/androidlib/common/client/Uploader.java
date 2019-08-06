package com.peanut.androidlib.common.client;
import android.webkit.MimeTypeMap;

import com.peanut.androidlib.common.worker.SingleWorker;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
public class Uploader {
    //region public static read-only properties
    private static ConcurrentLinkedQueue<Uploader> uploaders = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<Uploader> getUploaders() {
        return uploaders;
    }
    //endregion
    //region public static properties
    private static int maxUploaderCount = 10000;
    public static int getMaxUploaderCount() {
        return maxUploaderCount;
    }
    public static void setMaxUploaderCount(int maxUploaderCount) {
        Uploader.maxUploaderCount = maxUploaderCount;
    }
    //endregion
    //region public properties
    private int bufferSize = 81920;
    public int getBufferSize() {
        return bufferSize;
    }
    public void setBufferSize(int bufferSize) {
        if(bufferSize < 4096){
            throw new IllegalArgumentException("bufferSize must be equal or greater than 4096.");
        }
        this.bufferSize = bufferSize;
    }
    private long updateInterval = 1000;
    public long getUpdateInterval() {
        return updateInterval;
    }
    public void setUpdateInterval(long updateInterval) {
        if(updateInterval <= 0){
            throw new IllegalArgumentException("bufferSize must be a positive number.");
        }
        this.updateInterval = updateInterval;
    }
    //endregion
    //region public read-only properties
    private State state;
    public State getState() {
        return state;
    }
    private String uploadUrl;
    public String getUploadUrl() {
        return uploadUrl;
    }
    private String filePath;
    public String getFilePath() {
        return filePath;
    }
    private String fileName;
    public String getFileName() {
        return fileName;
    }
    private String fileExtension;
    public String getFileExtension() {
        return fileExtension;
    }
    private String mimeType;
    public String getMimeType() {
        return mimeType;
    }
    private long fileSize;
    public long getFileSize() {
        return fileSize;
    }
    private File file;
    public File getFile() {
        return file;
    }
    private long speed;
    public long getSpeed() {
        return speed;
    }
    private long estimatedTime;
    public long getEstimatedTime() {
        return estimatedTime;
    }
    //endregion
    //region private properties
    private String boundary;
    private String twoHyphens = "--";
    private String lineFeed = "\r\n";
    private String startingContentWrapper;
    private String endingContentWrapper;
    private String contentType;
    private String contentDisposition;
    private long contentLength;
    private long currentTotalBytes;
    private URL url;
    private HttpURLConnection httpURLConnection;
    private FileInputStream fileInputStream;
    private DataOutputStream dataOutputStream;
    private Timer timerUpdater;
    private Timer timerSpeedCalculator;
    private CountDownLatch countDownLatch;
    private SingleWorker singleWorker;
    //endregion
    //region listeners
    private OnPrepareListener onPrepareListener;
    private OnUploadListener onUploadListener;
    private OnDoneListener onDoneListener;
    private OnPauseListener onPauseListener;
    private OnCancelListener onCancelListener;
    private OnUpdateProgressListener onUpdateProgressListener;
    private OnUpdateSpeedListener onUpdateSpeedListener;
    private OnHttpFailListener onHttpFailListener;
    private OnExceptionListener onExceptionListener;
    //endregion
    //region constructors
    public Uploader(String uploadUrl, String filePath) {
        this.uploadUrl = uploadUrl;
        try {
            url = new URL(uploadUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL detected.");
        }
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found.");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("it's not a file path, it looks more like a directory.");
        }
        this.file = file;
        this.filePath = filePath;
        initializeGlobalVariables();
        uploaders.add(this);
        if(uploaders.size() > maxUploaderCount){
            refreshUploaderList();
        }
        state = State.Initial;
    }
    //endregion
    //region public methods
    public void start() {
        if (state != State.Initial) {
            return;
        }
        singleWorker.execute(this::prepare);
        singleWorker.execute(this::enterUploadingState);
    }
    public void pause(){
        if (state != State.Uploading) {
            return;
        }
        state = State.Pausing;
        triggerOnPauseEvent();
    }
    public void resume(){
        if (state == State.Pausing) {
            state = State.Uploading;
            triggerOnUploadEvent();
            countDownLatch.countDown();
        }
    }
    public void cancel(){
        if (state == State.Uploading){
            state = State.Cancel;
            triggerOnCancelEvent();
        }
        else if(state == State.Pausing){
            state = State.Cancel;
            triggerOnCancelEvent();
            countDownLatch.countDown();
        }
    }
    public void reUpload(){
        if(state == State.Initial || state == State.Preparing || state == State.Prepared || state == State.Uploading){
            return;
        }
        cancel();
        state = State.Initial;
        start();
    }
    //endregion
    //region private methods
    private void prepare() {
        state = State.Preparing;
        triggerOnPrepareEvent();
        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_NOT_FOUND || responseCode == HttpURLConnection.HTTP_BAD_GATEWAY){
                state = State.HttpFail;
                triggerOnHttpFailEvent();
                return;
            }
            if(!isBigRequestAllowed()){
                if(state == State.Exception){
                    return;
                }
                state = State.HttpFail;
                triggerOnHttpFailEvent();
                return;
            }
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(0);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-Type", contentType);
            httpURLConnection.setRequestProperty("Content-Disposition", contentDisposition);
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(contentLength));
            httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
            httpURLConnection.setFixedLengthStreamingMode(contentLength);
            currentTotalBytes = 0;
            state = State.Prepared;
        } catch (SocketException e) {
            state = State.Cancel;
            triggerOnCancelEvent();
        } catch (IOException e) {
            state = State.Exception;
            triggerOnExceptionEvent(e);
        }
    }
    private void enterUploadingState(){
        if(state != State.Prepared && state != State.Pausing){
            return;
        }
        state = State.Uploading;
        runTimers();
        upload();
    }
    private void upload(){
        triggerOnUploadEvent();
        try {
            fileInputStream = new FileInputStream(file); // TODO check timeout on big requests
            dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes(startingContentWrapper);
            byte[] buffer;
            int bufferSize;
            int bytesRead;
            int byteAvailable;
            byteAvailable = fileInputStream.available();
            bufferSize = Math.min(byteAvailable, this.bufferSize);
            buffer = new byte[bufferSize];
            bytesRead = fileInputStream.read(buffer);
            while(bytesRead > 0){
                dataOutputStream.write(buffer, 0, bytesRead);
                dataOutputStream.flush();
                currentTotalBytes += bytesRead;
                if(state == State.Pausing){
                    countDownLatch = new CountDownLatch(1);
                    try {
                        countDownLatch.await();
                        if(state == State.Uploading){
                            runTimers();
                        }
                        else if(state == State.Cancel || state == State.Initial){
                            dataOutputStream.close();
                            fileInputStream.close();
                            httpURLConnection.disconnect();
                            return;
                        }
                    } catch (InterruptedException ignored) { }
                }
                if(state == State.Cancel){
                    dataOutputStream.close();
                    fileInputStream.close();
                    httpURLConnection.disconnect();
                    return;
                }
                byteAvailable = fileInputStream.available();
                bufferSize = Math.min(byteAvailable, this.bufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer);
            }
            dataOutputStream.writeBytes(endingContentWrapper);
            dataOutputStream.flush();
            dataOutputStream.close();
            fileInputStream.close();
            if(httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                state = State.Done;
                triggerOnDoneEvent();
            }
            else{
                state = State.HttpFail;
                triggerOnHttpFailEvent();
            }
        } catch (FileNotFoundException e) {
            triggerOnExceptionEvent(e);
        } catch (IOException e) {
            if(state == State.Cancel || state == State.Initial){
                return;
            }
            triggerOnExceptionEvent(e);
        }
    }
    private boolean isBigRequestAllowed(){
        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(contentLength));
            httpURLConnection.setRequestProperty("Content-Type", "text/plain");
            httpURLConnection.setChunkedStreamingMode(10);
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes("");
            dataOutputStream.flush();
            return httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND;
        } catch (IOException e) {
            state = State.Exception;
            triggerOnExceptionEvent(e);
            return false;
        }
    }
    private void runTimers(){
        if(timerUpdater != null){
            timerUpdater.cancel();
            timerSpeedCalculator.cancel();
        }
        timerUpdater = new Timer();
        timerSpeedCalculator = new Timer();
        TimerTask timerTaskUpdater = new TimerTask() {
            @Override
            public void run() {
                float percent = (float) currentTotalBytes / fileSize;
                triggerOnUpdateProgressEvent(percent);
                if (state == State.Done || state == State.Pausing || state == State.Cancel) {
                    timerUpdater.cancel();
                }
            }
        };
        TimerTask timerTaskSpeedCalculator = new TimerTask() {
            private long previousBytes = currentTotalBytes;
            private long nowBytes = currentTotalBytes;
            @Override
            public void run() {
                nowBytes = currentTotalBytes;
                speed = nowBytes - previousBytes;
                if (speed != 0) {
                    estimatedTime = (fileSize - currentTotalBytes) / speed;
                } else {
                    estimatedTime = Integer.MAX_VALUE;
                }
                previousBytes = nowBytes;
                if (state == State.Done || state == State.Pausing || state == State.Cancel) {
                    speed = 0;
                    estimatedTime = Integer.MAX_VALUE;
                    timerSpeedCalculator.cancel();
                }
                triggerOnUpdateSpeedEvent();
            }
        };
        timerUpdater.schedule(timerTaskUpdater, 0, updateInterval);
        timerSpeedCalculator.schedule(timerTaskSpeedCalculator, 0, 1000);
    }
    private void initializeGlobalVariables(){
        fileName = file.getName();
        fileSize = file.length();
        boundary = UUID.randomUUID().toString();
        int dotIndex = fileName.lastIndexOf(".");
        fileExtension = dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
        fileExtension = fileExtension.toLowerCase();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        mimeType = mimeTypeMap.getMimeTypeFromExtension(fileExtension);
        mimeType = mimeType == null ? "unknown" : mimeType;
        contentType = "multipart/form-data; boundary=\"" + boundary + "\"";
        contentDisposition = "form-data; name=\"\"; filename=\"" + fileName + "\"";
        startingContentWrapper = twoHyphens + boundary + lineFeed +
                "Content-Disposition: " + contentDisposition + lineFeed +
                "Content-Type: " + mimeType + lineFeed + lineFeed;
        endingContentWrapper = lineFeed + twoHyphens + boundary + twoHyphens + lineFeed;
        contentLength = fileSize + startingContentWrapper.length() + endingContentWrapper.length();
        singleWorker = new SingleWorker();
    }
    private void triggerOnPrepareEvent(){
        if (onPrepareListener != null) {
            onPrepareListener.onPrepare();
        }
    }
    private void triggerOnUploadEvent(){
        if(onUploadListener != null){
            onUploadListener.onUpload();
        }
    }
    private void triggerOnDoneEvent(){
        if(onDoneListener != null){
            onDoneListener.onDone();
        }
    }
    private void triggerOnPauseEvent(){
        if (onPauseListener != null) {
            onPauseListener.onPause();
        }
    }
    private void triggerOnCancelEvent(){
        if (onCancelListener != null) {
            onCancelListener.onCancel();
        }
    }
    private void triggerOnUpdateProgressEvent(float percent){
        if(onUpdateProgressListener != null){
            onUpdateProgressListener.onUpdateProgress(percent, currentTotalBytes);
        }
    }
    private void triggerOnUpdateSpeedEvent(){
        if (onUpdateSpeedListener != null) {
            onUpdateSpeedListener.onUpdateSpeed(speed, estimatedTime);
        }
    }
    private void triggerOnHttpFailEvent(){
        if(onHttpFailListener != null){
            onHttpFailListener.onHttpFail(httpURLConnection);
        }
    }
    private void triggerOnExceptionEvent(Exception e){
        if(onExceptionListener != null){
            onExceptionListener.onException(e);
        }
    }
    //endregion
    //region public static methods
    public static void startUploaders(Uploader[] uploaders){
        for(Uploader uploader : uploaders){
            Uploader.uploaders.add(uploader);
            uploader.start();
        }
    }
    public static void startAll(){
        for(Uploader uploader : uploaders){
            uploader.start();
        }
    }
    public static void pauseAll(){
        for(Uploader uploader : uploaders){
            uploader.pause();
        }
    }
    public static void resumeAll(){
        for(Uploader uploader : uploaders){
            uploader.resume();
        }
    }
    public static void cancelAll(){
        for(Uploader uploader : uploaders){
            uploader.cancel();
        }
        uploaders.clear();
    }
    public static void reUploadAll(){
        for(Uploader uploader : uploaders){
            uploader.reUpload();
        }
    }
    //endregion
    //region private static methods
    private static void refreshUploaderList(){
        List<Uploader> list = new ArrayList<>();
        for (Uploader u : uploaders) {
            if (u.state != State.Done && u.state != State.Cancel && u.state != State.HttpFail) {
                list.add(u);
            }
        }
        uploaders.clear();
        uploaders.addAll(list);
    }
    //endregion
    //region listeners setters
    public Uploader setOnPrepareListener(OnPrepareListener onPrepareListener) {
        this.onPrepareListener = onPrepareListener;
        return this;
    }
    public Uploader setOnUploadListener(OnUploadListener onUploadListener) {
        this.onUploadListener = onUploadListener;
        return this;
    }
    public Uploader setOnDoneListener(OnDoneListener onDoneListener) {
        this.onDoneListener = onDoneListener;
        return this;
    }
    public Uploader setOnPauseListener(OnPauseListener onPauseListener) {
        this.onPauseListener = onPauseListener;
        return this;
    }
    public Uploader setOnCancelListener(OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
        return this;
    }
    public Uploader setOnUpdateProgressListener(OnUpdateProgressListener onUpdateProgressListener) {
        this.onUpdateProgressListener = onUpdateProgressListener;
        return this;
    }
    public Uploader setOnUpdateSpeedListener(OnUpdateSpeedListener onUpdateSpeedListener) {
        this.onUpdateSpeedListener = onUpdateSpeedListener;
        return this;
    }
    public Uploader setOnHttpFailListener(OnHttpFailListener onHttpFailListener) {
        this.onHttpFailListener = onHttpFailListener;
        return this;
    }
    public Uploader setOnExceptionListener(OnExceptionListener onExceptionListener) {
        this.onExceptionListener = onExceptionListener;
        return this;
    }
    //endregion
    //region listener interfaces
    public interface OnPrepareListener {
        void onPrepare();
    }
    public interface OnUploadListener {
        void onUpload();
    }
    public interface OnDoneListener {
        void onDone();
    }
    public interface OnPauseListener {
        void onPause();
    }
    public interface OnCancelListener {
        void onCancel();
    }
    public interface OnUpdateProgressListener {
        void onUpdateProgress(float percent, long currentTotalBytes);
    }
    public interface OnUpdateSpeedListener {
        /**
         *
         * @param speed
         * @param estimatedTime Estimated time in seconds
         */
        void onUpdateSpeed(long speed, long estimatedTime);
    }
    public interface OnHttpFailListener {
        void onHttpFail(HttpURLConnection httpURLConnection);
    }
    public interface OnExceptionListener {
        void onException(Exception e);
    }
    //endregion
    //region enums
    public enum State {
        Initial, Preparing, Prepared, Uploading, Pausing, Done, Cancel, HttpFail, Exception
    }
    //endregion
}
