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
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
public class Downloader {
    //region public static read-only properties
    private static ConcurrentLinkedQueue<Downloader> downloaders = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<Downloader> getDownloaders() {
        return downloaders;
    }
    //endregion
    //region public static properties
    private static int defaultRangeRequestSize = 1048576;
    public static int getDefaultRangeRequestSize() {
        return defaultRangeRequestSize;
    }
    public static void setDefaultRangeRequestSize(int defaultRangeRequestSize) {
        Downloader.defaultRangeRequestSize = defaultRangeRequestSize;
    }
    private static int defaultDeleteAttempt = 100;
    public static int getDefaultDeleteAttempt() {
        return defaultDeleteAttempt;
    }
    public static void setDefaultDeleteAttempt(int defaultDeleteAttempt) {
        Downloader.defaultDeleteAttempt = defaultDeleteAttempt;
    }
    private static int defaultDeleteFailDelay = 1000;
    public static int getDefaultDeleteFailDelay() {
        return defaultDeleteFailDelay;
    }
    public static void setDefaultDeleteFailDelay(int defaultDeleteFailDelay) {
        Downloader.defaultDeleteFailDelay = defaultDeleteFailDelay;
    }
    private static int maxDownloaderCount = 10000;
    public static int getMaxDownloaderCount() {
        return maxDownloaderCount;
    }
    public static void setMaxDownloaderCount(int maxDownloaderCount) {
        Downloader.maxDownloaderCount = maxDownloaderCount;
    }
    //endregion
    //region public properties
    private String saveDirectory;
    public String getSaveDirectory() {
        return saveDirectory;
    }
    public void setSaveDirectory(String saveDirectory) {
        if(saveDirectory == null){
            throw new IllegalArgumentException("saveDirectory cannot be null.");
        }
        File file = new File(saveDirectory);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("Cannot make directory");
            }
        }
        this.saveDirectory = saveDirectory;
    }
    private long updateInterval = 1000;
    public long getUpdateInterval() {
        return updateInterval;
    }
    public void setUpdateInterval(long updateInterval) {
        if(updateInterval <= 0){
            throw new IllegalArgumentException("updateInterval must be a positive number.");
        }
        this.updateInterval = updateInterval;
    }
    //endregion
    //region public read-only properties
    private String downloadUrl;
    public String getDownloadUrl() {
        return downloadUrl;
    }
    private String fileName;
    public String getFileName() {
        return fileName;
    }
    private String fileExtension;
    public String getFileExtension() {
        return fileExtension;
    }
    private String fileType;
    public String getFileType() {
        return fileType;
    }
    private String filePath;
    public String getFilePath() {
        return filePath;
    }
    private int fileSize;
    public int getFileSize() {
        return fileSize;
    }
    private boolean override;
    public boolean isOverride() {
        return override;
    }
    private boolean resumable;
    public boolean isResumable() {
        return resumable;
    }
    private long speed;
    public long getSpeed() {
        return speed;
    }
    private long estimatedTime;
    public long getEstimatedTime() {
        return estimatedTime;
    }
    private State state;
    public State getState() {
        return state;
    }
    //endregion
    //region private properties
    private long currentTotalBytes;
    private File file;
    private FileOutputStream fileOutputStream;
    private HttpURLConnection httpURLConnection;
    private Timer timerUpdater;
    private Timer timerSpeedCalculator;
    private SingleWorker singleWorker;
    //endregion
    //region listeners
    private OnPrepareListener onPrepareListener;
    private OnDownloadListener onDownloadListener;
    private OnDoneListener onDoneListener;
    private OnPauseListener onPauseListener;
    private OnCancelListener onCancelListener;
    private OnUpdateProgressListener onUpdateProgressListener;
    private OnUpdateSpeedListener onUpdateSpeedListener;
    private OnHttpFailListener onHttpFailListener;
    private OnExceptionListener onExceptionListener;
    //endregion
    //region constructors
    public Downloader(String saveDirectory, String downloadUrl, String fileName, boolean override) {
        if (saveDirectory == null) {
            throw new IllegalArgumentException("saveDirectory cannot be null.");
        }
        if (downloadUrl == null) {
            throw new IllegalArgumentException("downloadUrl cannot be null.");
        }
        setSaveDirectory(saveDirectory);
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.override = override;
        singleWorker = new SingleWorker();
        downloaders.add(this);
        if (downloaders.size() > maxDownloaderCount) {
            refresherDownloaderList();
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
        singleWorker.execute(this::enterDownloadingState);
    }
    public void pause() {
        if (!isResumable()) {
            return;
        }
        if (state != State.Downloading) {
            return;
        }
        state = State.Pausing;
        triggerOnPauseEvent();
    }
    public void resume() {
        if (state == State.Pausing) {
            singleWorker.execute(this::enterDownloadingState);
        }
    }
    public void cancel(boolean deleteFile) {
        if (state == State.Downloading || state == State.Pausing) {
            state = State.Cancel;
            if (deleteFile) {
                file.delete();
            }
            triggerOnCancelEvent();
        } else if (state == State.Preparing) {
            httpURLConnection.disconnect();
        }
    }
    public void reDownload() {
        if(state == State.Initial || state == State.Preparing || state == State.Prepared || state == State.Downloading){
            return;
        }
        cancel(false);
        state = State.Initial;
        start();
    }
    //endregion
    //region private methods
    private void prepare() {
        state = State.Preparing;
        triggerOnPrepareEvent();
        try {
            URL url = new URL(downloadUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Range", "bytes=0-100");
            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
                resumable = true;
                fileSize = Integer.parseInt(httpURLConnection.getHeaderField("Content-Range").split("/")[1]);
            } else if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                resumable = false;
                fileSize = httpURLConnection.getContentLength();
            } else {
                state = State.HttpFail;
                triggerOnHttpFailEvent();
                return;
            }
            String contentDisposition = httpURLConnection.getHeaderField("Content-Disposition");
            fileType = httpURLConnection.getHeaderField("Content-Type");
            if (fileName == null) {
                fileName = contentDisposition.split("; ")[1].split("=")[1];
            }
            int dotIndex = fileName.lastIndexOf(".");
            fileExtension = dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
            fileExtension = fileExtension.toLowerCase();
            filePath = saveDirectory + "/" + fileName;
            filePath = filePath.replace("//", "/");
            file = new File(filePath);
            if (!isOverride()) {
                if (file.exists()) {
                    state = State.AlreadyDownloaded;
                    return;
                } else if (!file.createNewFile()) {
                    state = State.Exception;
                    IOException e = new IOException("cannot create the file.");
                    triggerOnExceptionEvent(e);
                    return;
                }
            }
            fileOutputStream = new FileOutputStream(file);
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
    private void enterDownloadingState() {
        if (state != State.Prepared && state != State.Pausing) {
            return;
        }
        state = State.Downloading;
        runTimers();
        if (isResumable()) {
            resumableDownload();
        } else {
            nonResumableDownload();
        }
    }
    private void resumableDownload() {
        triggerOnDownloadEvent();
        try {
            while (state == State.Downloading) {
                URL url = new URL(downloadUrl);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Range", "bytes=" + currentTotalBytes + "-" + (currentTotalBytes + defaultRangeRequestSize));
                if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL) {
                    state = State.HttpFail;
                    triggerOnHttpFailEvent();
                    return;
                } else {
                    InputStream inputStream = httpURLConnection.getInputStream();
                    DataInputStream dataInputStream = new DataInputStream(inputStream);
                    byte[] buffer = new byte[defaultRangeRequestSize];
                    int length;
                    while ((length = dataInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, length);
                        currentTotalBytes += length;
                    }
                    dataInputStream.close();
                    inputStream.close();
                }
                if (currentTotalBytes >= fileSize) {
                    fileOutputStream.close();
                    state = State.Done;
                    triggerOnDoneEvent();
                    return;
                }
            }
        } catch (ProtocolException e) {
            state = State.Exception;
            triggerOnExceptionEvent(e);
        } catch (MalformedURLException e) {
            state = State.Exception;
            triggerOnExceptionEvent(e);
        } catch (IOException e) {
            state = State.Exception;
            triggerOnExceptionEvent(e);
        }
    }
    private void nonResumableDownload() {
        triggerOnDownloadEvent();
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                state = State.HttpFail;
                triggerOnHttpFailEvent();
            } else {
                InputStream inputStream = httpURLConnection.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                byte[] buffer = new byte[defaultRangeRequestSize];
                int length;
                while ((length = dataInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                    currentTotalBytes += length;
                    if (currentTotalBytes >= fileSize) {
                        fileOutputStream.close();
                        state = State.Done;
                        triggerOnDoneEvent();
                        break;
                    }
                    if (state == State.Cancel) {
                        fileOutputStream.close();
                        triggerOnCancelEvent();
                        break;
                    }
                }
                dataInputStream.close();
                inputStream.close();
            }
        } catch (ProtocolException e) {
            state = State.HttpFail;
            triggerOnExceptionEvent(e);
        } catch (MalformedURLException e) {
            state = State.HttpFail;
            triggerOnExceptionEvent(e);
        } catch (IOException e) {
            state = State.HttpFail;
            triggerOnExceptionEvent(e);
        }
    }
    private void runTimers() {
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
    private void triggerOnPrepareEvent(){
        if (onPrepareListener != null) {
            onPrepareListener.onPrepare();
        }
    }
    private void triggerOnDownloadEvent(){
        if(onDownloadListener != null){
            onDownloadListener.onDownload();
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
    public static void startDownloaders(Downloader[] downloaders){
        for(Downloader downloader : downloaders){
            Downloader.downloaders.add(downloader);
            downloader.start();
        }
    }
    public static void startAll(){
        for(Downloader downloader : downloaders){
            downloader.start();
        }
    }
    public static void pauseAll(){
        for(Downloader downloader : downloaders){
            downloader.pause();
        }
    }
    public static void resumeAll(){
        for(Downloader downloader : downloaders){
            downloader.resume();
        }
    }
    public static void cancelAll(boolean deleteFiles) {
        for (Downloader downloader : downloaders) {
            downloader.cancel(deleteFiles);
        }
        downloaders.clear();
    }
    public static void reDownloadAll(){
        for(Downloader downloader : downloaders){
            downloader.reDownload();
        }
    }
    //endregion
    //region private static methods
    private static void refresherDownloaderList() {
        List<Downloader> list = new ArrayList<>();
        for (Downloader d : downloaders) {
            if (d.state != State.AlreadyDownloaded && d.state != State.Done && d.state != State.Cancel && d.state != State.HttpFail) {
                list.add(d);
            }
        }
        downloaders.clear();
        downloaders.addAll(list);
    }
    //endregion
    //region listener setters
    public Downloader setOnPrepareListener(OnPrepareListener onPrepareListener) {
        this.onPrepareListener = onPrepareListener;
        return this;
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
    //endregion
    //region listener interfaces
    public interface OnPrepareListener {
        void onPrepare();
    }
    public interface OnDownloadListener {
        void onDownload();
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
        Initial, Preparing, Prepared, Downloading, Pausing, Done, Cancel, AlreadyDownloaded, HttpFail, Exception
    }
    //endregion
}
