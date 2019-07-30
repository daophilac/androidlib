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
public class MainActivity extends AppCompatActivity {
    private EditText editTextUrl;
    private TextView textViewPercent;
    private TextView textViewBytes;
    private TextView textViewSpeed;
    private Button buttonStart;
    private Button buttonPause;
    private Button buttonResume;
    private Button buttonCancel;
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
        buttonStart = findViewById(R.id.button_start);
        buttonPause = findViewById(R.id.button_pause);
        buttonResume = findViewById(R.id.button_resume);
        buttonCancel = findViewById(R.id.button_cancel);
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
        downloader = new Downloader("/sdcard/download", editTextUrl.getText().toString(), "file.zip", true);
        downloader.setUpdateInterval(500);
        downloader.setDownloaderListener(new Downloader.DownloaderListener() {
            @Override
            public void onPrepared() {
                fileSize = downloader.getFileSize();
                Toast.makeText(MainActivity.this, "On Prepared!", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onPause() {
                Toast.makeText(MainActivity.this, "On Pause!", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onResume() {
                Toast.makeText(MainActivity.this, "On Resume!", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onFinish() {
                Toast.makeText(MainActivity.this, "Finished!", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onCancel() {
                Toast.makeText(MainActivity.this, "On Cancel!", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onUpdatePercent(float percent) {
                runOnUiThread(() -> {
                    textViewPercent.setText("Percent: " + percent * 100 + "%");
                    float speed = downloader.getSpeed() / 1024 / 1024f;
                    textViewSpeed.setText("Speed: " + speed + "MB/s");
                });
            }
            @Override
            public void onUpdateBytes(long bytes) {
                runOnUiThread(() -> {
                    textViewBytes.setText("Bytes: " + bytes + "/" + fileSize);
                });
            }
            @Override
            public void onFailure(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
        buttonPause.setOnClickListener(v -> downloader.pause());
        buttonResume.setOnClickListener(v -> downloader.resume());
        buttonCancel.setOnClickListener(v -> downloader.cancel(true));
        buttonStart.setOnClickListener(v -> {
            downloader.start();
        });
    }
}
