package com.peanut.example;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.peanut.androidlib.common.permissionmanager.PermissionInquirer;
import com.peanut.androidlib.filemanager.FolderPicker;
import com.peanut.example.folderpicker.R;

import java.util.Arrays;
import java.util.HashMap;
public class FolderPickerActivity extends AppCompatActivity {
    private Button buttonOpenPicker;
    private FolderPicker folderPicker;
    private PermissionInquirer permissionInquirer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_picker);
        this.folderPicker = new FolderPicker(this, R.id.full_screen_fragment_container, Environment.getExternalStorageDirectory().getAbsolutePath());
        this.folderPicker.setSetFileCurrentlyChosen(Arrays.asList(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music/FFII"));
        this.folderPicker.setListDesiredFileExtension(Arrays.asList(".mp3"));
        HashMap<String, Integer> map = new HashMap<>();
        map.put(".mp3", R.drawable.ic_music_note_black_32dp);
        map.put(".flac", R.drawable.ic_music_note_black_32dp);
        folderPicker.setIconForFileTypeFromDrawableId(map);
        this.folderPicker.setShowFileToo(true);

        this.permissionInquirer = new PermissionInquirer(this);
        this.buttonOpenPicker = findViewById(R.id.button_open_picker);

        this.buttonOpenPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionInquirer.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    folderPicker.openFolderPicker();
                } else {
                    permissionInquirer.askPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 1);
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    folderPicker.openFolderPicker();
                } else {
                    Toast.makeText(this, "Read external permission has not been granted.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}