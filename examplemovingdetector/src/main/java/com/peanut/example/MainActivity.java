package com.peanut.example;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.peanut.androidlib.common.permissionmanager.PermissionInquirer;
import com.peanut.androidlib.sensormanager.LocationTracker;
import com.peanut.androidlib.sensormanager.MovingDetector;
import com.peanut.example.movingdetector.R;

import java.util.Locale;
public class MainActivity extends AppCompatActivity {
    private EditText editTextDistanceToMove;
    private TextView textViewDistanceHasMoved;
    private Button buttonStart;
    private Button buttonPause;
    private Button buttonResume;
    private Button buttonStop;
    private TextView textViewFurtherDetails;
    private TextView textViewStatus;
    private MovingDetector.LocationDetector locationDetector;
    private float distanceToMove;
    private float distanceHasMoved;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionInquirer permissionInquirer = new PermissionInquirer(this);
        if (!permissionInquirer.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionInquirer.askPermission(Manifest.permission.ACCESS_FINE_LOCATION, 1);
        } else {
            configureDetector();
        }
    }
    private void configureDetector() {
        locationDetector = MovingDetector.newInstance(this, new LocationTracker.LocationServiceListener() {
            @Override
            public void onLocationServiceOff() {
                locationDetector.requestSelfLocationSettings(1);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                MainActivity.this.sendBroadcast(intent);
            }
            @Override
            public void onLocationServiceOn() {

            }
        });
        locationDetector.setMinDistanceThreshold(1);
        locationDetector.registerHighAccuracyModeListener(new LocationTracker.HighAccuracyModeListener() {
            @Override
            public void onEnter() {
            }
            @Override
            public void onExit() {
                locationDetector.requestSelfLocationSettings(1);
            }
        });
        locationDetector.checkLocationSetting(new LocationTracker.OnLocationSettingResultListener() {
            @Override
            public void onSatisfiedSetting() {
                configureControls();
            }
            @Override
            public void onUnsatisfiedSetting(Exception e) {
                locationDetector.requestSelfLocationSettings(1);
            }
        });
    }
    private void configureControls() {
        editTextDistanceToMove = findViewById(R.id.edit_text_distance_to_move);
        textViewDistanceHasMoved = findViewById(R.id.text_view_distance_has_moved);
        buttonStart = findViewById(R.id.button_start);
        buttonPause = findViewById(R.id.button_pause);
        buttonResume = findViewById(R.id.button_resume);
        buttonStop = findViewById(R.id.button_stop);
        textViewFurtherDetails = findViewById(R.id.text_view_further_details);
        textViewStatus = findViewById(R.id.text_view_status);

        buttonStart.setOnClickListener(v -> {
            if (!locationDetector.isRunning()) {
                locationDetector.checkLocationSetting(new LocationTracker.OnLocationSettingResultListener() {
                    @Override
                    public void onSatisfiedSetting() {
                        distanceToMove = Float.parseFloat(editTextDistanceToMove.getText().toString());
                        distanceHasMoved = 0;
                        textViewDistanceHasMoved.setText(String.format(Locale.US, getString(R.string.distance_has_moved), distanceHasMoved));
                        textViewStatus.setTextColor(getResources().getColor(R.color.colorBlue));
                        textViewStatus.setText(getString(R.string.tracking));
                        locationDetector.start(new MovingDetector.MovingDetectorListener() {
                            @Override
                            public void onMoved(float distance, String furtherDetails) {
                                textViewFurtherDetails.setText(furtherDetails);
                                distanceHasMoved += distance;
                                textViewDistanceHasMoved.setText(String.format(Locale.US, getString(R.string.distance_has_moved), distanceHasMoved));
                                distanceToMove -= distance;
                                if (distanceToMove <= 0) {
                                    locationDetector.stop();
                                    textViewStatus.setTextColor(getResources().getColor(R.color.colorGreen));
                                    textViewStatus.setText(getString(R.string.done));
                                }
                            }
                            @Override
                            public void onStop() {

                            }
                        });
                    }
                    @Override
                    public void onUnsatisfiedSetting(Exception e) {
                        locationDetector.requestSelfLocationSettings(2);
                    }
                });
            }
        });
        buttonPause.setOnClickListener(v -> {
            if (locationDetector.isRunning()) {
                locationDetector.pause();
                textViewStatus.setText(getString(R.string.pause));
            }
        });
        buttonResume.setOnClickListener(v -> {
            if (locationDetector.isRunning()) {
                locationDetector.requestSelfLocationSettings(1);
                locationDetector.resume();
                textViewStatus.setText(getString(R.string.tracking));
            }
        });
        buttonStop.setOnClickListener(v -> {
            if (locationDetector.isRunning()) {
                locationDetector.stop();
                textViewStatus.setTextColor(getResources().getColor(R.color.colorRed));
                textViewStatus.setText(getString(R.string.stopped));
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    configureDetector();
                } else {
                    new PermissionDeniedDialogFragment().show(getSupportFragmentManager(), this.getPackageName());
                }
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    configureControls();
                } else {
                    new UnsatisfiedSettingDialogFragment().show(getSupportFragmentManager(), this.getPackageName());
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    distanceToMove = Float.parseFloat(editTextDistanceToMove.getText().toString());
                    distanceHasMoved = 0;
                    textViewDistanceHasMoved.setText(String.format(Locale.US, getString(R.string.distance_has_moved), distanceHasMoved));
                    textViewStatus.setTextColor(getResources().getColor(R.color.colorBlue));
                    textViewStatus.setText(getString(R.string.tracking));
                    locationDetector.start(new MovingDetector.MovingDetectorListener() {
                        @Override
                        public void onMoved(float distance, String furtherDetails) {
                            textViewFurtherDetails.setText(furtherDetails);
                            distanceHasMoved += distance;
                            textViewDistanceHasMoved.setText(String.format(Locale.US, getString(R.string.distance_has_moved), distanceHasMoved));
                            distanceToMove -= distance;
                            if (distanceToMove <= 0) {
                                locationDetector.stop();
                                textViewStatus.setTextColor(getResources().getColor(R.color.colorGreen));
                                textViewStatus.setText(getString(R.string.done));
                            }
                        }
                        @Override
                        public void onStop() {

                        }
                    });
                } else {
                    new UnsatisfiedSettingDialogFragment().show(getSupportFragmentManager(), this.getPackageName());
                }
                break;
        }
    }
}
