package com.peanut.exampleuploader;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.peanut.androidlib.common.client.Uploader;
import com.peanut.androidlib.common.permissionmanager.PermissionInquirer;

import java.io.IOException;
public class MainActivity extends AppCompatActivity {
    private EditText editTextFilePath;
    private TextView textViewPercent;
    private TextView textViewUploaded;
    private TextView textViewSpeed;
    private TextView textViewEstimated;
    private TextView textViewStatus;
    private Button buttonStart;
    private Button buttonPause;
    private Button buttonResume;
    private Button buttonCancel;
    private Button buttonReUpload;
    private Uploader uploader;
    private PermissionInquirer permissionInquirer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTextFilePath = findViewById(R.id.edit_text_file_path);
        textViewPercent = findViewById(R.id.text_view_percent);
        textViewUploaded = findViewById(R.id.text_view_uploaded);
        textViewSpeed = findViewById(R.id.text_view_speed);
        textViewEstimated = findViewById(R.id.text_view_estimated);
        textViewStatus = findViewById(R.id.text_view_status);
        buttonStart = findViewById(R.id.button_start);
        buttonPause = findViewById(R.id.button_pause);
        buttonResume = findViewById(R.id.button_resume);
        buttonCancel = findViewById(R.id.button_cancel);
        buttonReUpload = findViewById(R.id.button_re_upload);
        initializeUploader();
        permissionInquirer = new PermissionInquirer(this);
        if(!permissionInquirer.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)){
            permissionInquirer.askPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 1);
        }
        else{
            configureViews();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Read storage permission has not been granted. Terminate", Toast.LENGTH_LONG).show();
            }
            else{
                configureViews();
            }
        }
    }
    private void initializeUploader(){
        uploader = new Uploader();
        uploader.setOnPrepareListener(() -> {
            runOnUiThread(() -> textViewStatus.setText("Preparing"));
        }).setOnDoneListener(() -> {
            runOnUiThread(() -> textViewStatus.setText("Done"));
        }).setOnUploadListener(() -> {
            runOnUiThread(() -> textViewStatus.setText("Uploading"));
        }).setOnPauseListener(() -> {
            runOnUiThread(() -> textViewStatus.setText("Pausing"));
        }).setOnCancelListener(() -> {
            runOnUiThread(() -> textViewStatus.setText("Cancelled"));
        }).setOnUpdateProgressListener((percent, currentTotalBytes) -> {
            runOnUiThread(() -> {
                textViewPercent.setText("Percent: " + percent * 100 + " %");
                textViewUploaded.setText("Uploaded: " + currentTotalBytes / 1024 / 1024f + " MB");
            });
        }).setOnUpdateSpeedListener((speed, estimatedTime) -> {
            runOnUiThread(() -> {
                textViewSpeed.setText("Speed: " + speed / 1024 / 1024f + " MB/s");
                textViewEstimated.setText("Estimated: " + estimatedTime + " seconds");
            });
        }).setOnHttpFailListener(httpURLConnection -> {
            runOnUiThread(() -> {
                try {
                    textViewStatus.setText(httpURLConnection.getResponseMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }).setOnExceptionListener(e -> {
            runOnUiThread(() -> {
                textViewStatus.setText(e.getMessage());
            });
        });
    }
    private void configureViews(){
        buttonStart.setOnClickListener(v -> {
            uploader.setUploadUrl("http://10.0.2.2:55555/api/user/testupload");
            uploader.setFilePath(editTextFilePath.getText().toString());
            uploader.start();
        });
        buttonPause.setOnClickListener(v -> uploader.pause());
        buttonResume.setOnClickListener(v -> uploader.resume());
        buttonCancel.setOnClickListener(v -> uploader.cancel());
        buttonReUpload.setOnClickListener(v -> uploader.reUpload());
    }
}
