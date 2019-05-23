package com.peanut.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.peanut.androidlib.view.DistancePicker;
import com.peanut.androidlib.view.IntegerPicker;
import com.peanut.androidlib.view.MeasurementPicker;

public class MainActivity extends AppCompatActivity {
    private DistancePicker distancePicker;
    private MeasurementPicker measurementPicker;
    private IntegerPicker integerPicker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        distancePicker = findViewById(R.id.distance_picker);
        measurementPicker = distancePicker.getMeasurementPicker();
        integerPicker = distancePicker.getIntegerPicker();
        integerPicker.setOnValueChangeListener((picker, oldVal, newVal) -> Toast.makeText(this, newVal + "", Toast.LENGTH_LONG).show());
        measurementPicker.setOnMeasurementChangeListener((measurementPicker, oldValue, newValue) -> Toast.makeText(this, newValue.getLongValue(), Toast.LENGTH_LONG).show());
        measurementPicker.setMeasurements(new MeasurementPicker.Measurement[]{MeasurementPicker.Measurement.KILOMETER, MeasurementPicker.Measurement.CENTIMETER});
    }
}
