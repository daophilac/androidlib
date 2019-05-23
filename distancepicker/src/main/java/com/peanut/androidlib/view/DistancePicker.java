package com.peanut.androidlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class DistancePicker extends LinearLayout {
    private MeasurementPicker measurementPicker;
    private IntegerPicker integerPicker;
    public DistancePicker(Context context) {
        super(context);
        initializeViews(context);
    }
    public DistancePicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DistancePicker);
        measurementPicker.setUnitStyle(typedArray.getInt(R.styleable.DistancePicker_unitStyle, MeasurementPicker.defUnitStyle.getValue()));
        integerPicker.setBaseMinValue(typedArray.getInt(R.styleable.DistancePicker_baseMinValue, IntegerPicker.defBaseMinValue));
        integerPicker.setBaseMaxValue(typedArray.getInt(R.styleable.DistancePicker_baseMaxValue, IntegerPicker.defBaseMaxValue));
        integerPicker.setMultiplicationFactor(typedArray.getInt(R.styleable.DistancePicker_multiplicationFactor, IntegerPicker.defMultiplicationFactor));
        integerPicker.setSelectedIndex(typedArray.getInt(R.styleable.DistancePicker_selectedIndex, IntegerPicker.defSelectedIndex));
        integerPicker.perform();
        typedArray.recycle();
    }

    public DistancePicker(Context context, @NonNull AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeViews(context);
    }
    private void initializeViews(Context context){
        View view = LayoutInflater.from(context).inflate(R.layout.distance_picker_view, this);
        measurementPicker = view.findViewById(R.id.measurement_picker);
        integerPicker = view.findViewById(R.id.multiply_number_picker);
    }
    public void setWrapSelectorWheel(boolean wrapSelectorWheel){
        measurementPicker.setWrapSelectorWheel(wrapSelectorWheel);
        integerPicker.setWrapSelectorWheel(wrapSelectorWheel);
    }

    public MeasurementPicker getMeasurementPicker() {
        return measurementPicker;
    }

    public IntegerPicker getIntegerPicker() {
        return integerPicker;
    }
}
