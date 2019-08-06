package com.peanut.exampledownloader;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.peanut.androidlib.common.client.Downloader;
import com.peanut.androidlib.common.permissionmanager.PermissionInquirer;

import java.io.IOException;
public class MainActivity extends AppCompatActivity {
    private EditText editTextUrl;
    private TextView textViewPercent;
    private TextView textViewBytes;
    private TextView textViewSpeed;
    private TextView textViewEstimated;
    private Button buttonStart;
    private Button buttonPause;
    private Button buttonResume;
    private Button buttonCancel;
    private Button buttonReDownload;
    private Downloader downloader;
    private PermissionInquirer permissionInquirer;
    private int fileSize;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTextUrl = findViewById(R.id.edit_text_url);
        textViewPercent = findViewById(R.id.text_view_percent);
        textViewBytes = findViewById(R.id.text_view_bytes);
        textViewSpeed = findViewById(R.id.text_view_speed);
        textViewEstimated = findViewById(R.id.text_view_estimated);
        buttonStart = findViewById(R.id.button_start);
        buttonPause = findViewById(R.id.button_pause);
        buttonResume = findViewById(R.id.button_resume);
        buttonCancel = findViewById(R.id.button_cancel);
        buttonReDownload = findViewById(R.id.button_re_download);
        permissionInquirer = new PermissionInquirer(this);
        if(Build.VERSION.SDK_INT >= 23){
            if(permissionInquirer.checkPermission(Manifest.permission.INTERNET) && permissionInquirer.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) && permissionInquirer.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)){
                initializeDownloader();
            }
            else{
                permissionInquirer.askPermission(new String[]{Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1){
            if(grantResults[0] == grantResults[1] && grantResults[1] == grantResults[2] && grantResults[2] == PackageManager.PERMISSION_GRANTED){
                initializeDownloader();
            }
            else{
                Toast.makeText(this, "Permissions have not been granted. Terminate!", Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }
    private void initializeDownloader(){
        downloader = new Downloader();
        downloader.setUpdateInterval(500);
        downloader.setOnPrepareListener(() -> Toast.makeText(this, "Preparing!", Toast.LENGTH_LONG).show());
        downloader.setOnDoneListener(() -> Toast.makeText(this, "Done!", Toast.LENGTH_LONG).show());
        downloader.setOnPauseListener(() -> Toast.makeText(this, "Pause!", Toast.LENGTH_LONG).show());
        downloader.setOnDownloadListener(() -> Toast.makeText(this, "Downloading!", Toast.LENGTH_LONG).show());
        downloader.setOnCancelListener(() -> Toast.makeText(this, "Cancel!", Toast.LENGTH_LONG).show());
        downloader.setOnHttpFailListener(httpURLConnection -> {
            try {
                Toast.makeText(this, String.valueOf(httpURLConnection.getResponseCode()), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        downloader.setOnExceptionListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
        downloader.setOnUpdateProgressListener((percent, currentTotalBytes) -> {
            runOnUiThread(() -> textViewPercent.setText("Percent: " + percent * 100 + "%"));
        });
        downloader.setOnUpdateSpeedListener((s, e) -> {
            runOnUiThread(() -> {
                float speed = s / 1024 / 1024f;
                textViewSpeed.setText("Speed: " + speed + " MB/s");
                textViewEstimated.setText("Estimated: " + e + " seconds");
            });
        });
        buttonPause.setOnClickListener(v -> downloader.pause());
        buttonResume.setOnClickListener(v -> downloader.resume());
        buttonCancel.setOnClickListener(v -> downloader.cancel(true));
        buttonStart.setOnClickListener(v -> {
            downloader.setDownloadUrl(editTextUrl.getText().toString());
            downloader.setSaveDirectory("/sdcard/download");
            downloader.setOverride(true);
            downloader.start();
        });
        buttonReDownload.setOnClickListener(v -> downloader.reDownload());
    }
}