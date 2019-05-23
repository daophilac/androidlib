package com.peanut.androidlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

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

    public void updateWrapSelectorWheel(boolean wrapSelectorWheel){
        measurementPicker.updateWrapSelectorWheel(wrapSelectorWheel);
        integerPicker.updateWrapSelectorWheel(wrapSelectorWheel);
    }
    public void perform(){
        integerPicker.perform();
    }
    public void setUnitStyle(MeasurementPicker.UnitStyle unitStyle) {
        measurementPicker.setUnitStyle(unitStyle);
    }
    public void setSelectedUnitIndex(int selectedIndex){
        measurementPicker.setSelectedIndex(selectedIndex);
    }

    public void setMeasurements(MeasurementPicker.Measurement[] measurements) {
        measurementPicker.setMeasurements(measurements);
    }

    public DistancePicker setBaseMinValue(int baseMinValue) {
        integerPicker.setBaseMinValue(baseMinValue);
        return this;
    }

    public DistancePicker setBaseMaxValue(int baseMaxValue) {
        integerPicker.setBaseMaxValue(baseMaxValue);
        return this;
    }

    public DistancePicker setMultiplicationFactor(int multiplicationFactor) {
        integerPicker.setMultiplicationFactor(multiplicationFactor);
        return this;
    }

    public DistancePicker setSelectedValueIndex(int selectedIndex) {
        integerPicker.setSelectedIndex(selectedIndex);
        return this;
    }

    public void setOnMeasurementChangeListener(MeasurementPicker.OnMeasurementChangeListener onMeasurementChangeListener) {
        measurementPicker.setOnMeasurementChangeListener(onMeasurementChangeListener);
    }

    public void setOnValueChangeListener(NumberPicker.OnValueChangeListener onValueChangeListener) {
        integerPicker.setOnValueChangeListener(onValueChangeListener);
    }
    public MeasurementPicker.UnitStyle getUnitStyle() {
        return measurementPicker.getUnitStyle();
    }

    public MeasurementPicker.Measurement getMeasurement() {
        return measurementPicker.getMeasurement();
    }

    public MeasurementPicker.Measurement[] getMeasurements() {
        return measurementPicker.getMeasurements();
    }

    public String[] getShortMeasurements() {
        return measurementPicker.getShortMeasurements();
    }

    public String[] getLongMeasurements() {
        return measurementPicker.getLongMeasurements();
    }
    public int getBaseMinValue() {
        return integerPicker.getBaseMinValue();
    }

    public int getBaseMaxValue() {
        return integerPicker.getBaseMaxValue();
    }

    public int getMultiplicationFactor() {
        return integerPicker.getMultiplicationFactor();
    }

    public int getSelectedIndex() {
        return integerPicker.getSelectedIndex();
    }

    public int getNumElement() {
        return integerPicker.getNumElement();
    }

    public int[] getValues() {
        return integerPicker.getValues();
    }

    public String[] getDisplayValues() {
        return integerPicker.getDisplayValues();
    }

    public MeasurementPicker getMeasurementPicker() {
        return measurementPicker;
    }

    public IntegerPicker getIntegerPicker() {
        return integerPicker;
    }
}
