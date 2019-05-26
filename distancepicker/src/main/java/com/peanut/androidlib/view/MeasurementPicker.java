package com.peanut.androidlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.NumberPicker;

public class MeasurementPicker extends NumberPicker {
    private static final String NEGATIVE_INDEX = "index cannot be negative";
    private static final String OUT_OF_RANGE_INDEX = "index is out of range. index=%d, numElement=%d";
    private static final String INVALID_INT_VALUE = "intValue is invalid. Valid values are from 0 to 4";
    private static final String INVALID_MEASUREMENT = "Invalid measurement=%s";
    public static final UnitStyle defUnitStyle = UnitStyle.SHORT;
    private UnitStyle unitStyle;
    private Measurement measurement;
    private int selectedIndex;
    private Measurement[] measurements = {Measurement.MILLIMETER, Measurement.CENTIMETER, Measurement.DECIMETER, Measurement.METER, Measurement.KILOMETER};
    private String[] shortMeasurements = {Measurement.MILLIMETER.shortDisplayedValue, Measurement.CENTIMETER.shortDisplayedValue, Measurement.DECIMETER.shortDisplayedValue, Measurement.METER.shortDisplayedValue, Measurement.KILOMETER.shortDisplayedValue};
    private String[] longMeasurements = {Measurement.MILLIMETER.longDisplayedValue, Measurement.CENTIMETER.longDisplayedValue, Measurement.DECIMETER.longDisplayedValue, Measurement.METER.longDisplayedValue, Measurement.KILOMETER.longDisplayedValue};
    private int numElement;
    private boolean wrapSelectorWheel;
    public MeasurementPicker(Context context) {
        super(context);
    }

    public MeasurementPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        numElement = measurements.length;
        setMaxValue(numElement - 1);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MeasurementPicker);
        setUnitStyle(UnitStyle.newInstanceFromValue(typedArray.getInt(R.styleable.MeasurementPicker_unitStyle, defUnitStyle.value)));
        typedArray.recycle();
    }

    public MeasurementPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    final void setUnitStyle(int value){
        setUnitStyle(UnitStyle.newInstanceFromValue(value));
    }
    public void updateWrapSelectorWheel(boolean wrapSelectorWheel){
        setWrapSelectorWheel(wrapSelectorWheel);
        setUnitStyle(unitStyle);
    }

    // Setters
    public void setUnitStyle(UnitStyle unitStyle) {
        this.unitStyle = unitStyle;
        switch(unitStyle){
            case SHORT:
                setDisplayedValues(shortMeasurements);
                break;
            case LONG:
                setDisplayedValues(longMeasurements);
                break;
        }
    }
    public void setSelectedIndex(int selectedIndex){
        if(selectedIndex < 0){
            throw new IllegalArgumentException(NEGATIVE_INDEX);
        }
        if(selectedIndex >= numElement){
            throw new IllegalArgumentException(String.format(OUT_OF_RANGE_INDEX, selectedIndex, numElement));
        }
        this.selectedIndex = selectedIndex;
        this.measurement = measurements[selectedIndex];
        setValue(selectedIndex);
    }
    public boolean setMeasurement(Measurement measurement){
        for(int i = 0; i < numElement; i++){
            if(measurements[i] == measurement){
                this.selectedIndex = i;
                this.measurement = measurement;
                setValue(selectedIndex);
                return true;
            }
        }
        return false;
    }

    public void setMeasurements(Measurement[] measurements) {
        this.measurements = measurements;
        this.numElement = measurements.length;
        setMaxValue(numElement - 1);
        this.shortMeasurements = new String[numElement];
        this.longMeasurements = new String[numElement];
        for(int i = 0; i < numElement; i++){
            shortMeasurements[i] = measurements[i].shortDisplayedValue;
            longMeasurements[i] = measurements[i].longDisplayedValue;
        }
        if(selectedIndex >= numElement){
            selectedIndex = 0;
            setValue(selectedIndex);
        }
        switch(unitStyle){
            case SHORT:
                setDisplayedValues(shortMeasurements);
                break;
            case LONG:
                setDisplayedValues(longMeasurements);
                break;
        }
    }

    public void setOnMeasurementChangeListener(OnMeasurementChangeListener onMeasurementChangeListener) {
        super.setOnValueChangedListener((picker, oldVal, newVal) -> onMeasurementChangeListener.onMeasurementChange(this, measurements[oldVal], measurements[newVal]));
    }

    // Getters
    public UnitStyle getUnitStyle() {
        return unitStyle;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public Measurement[] getMeasurements() {
        return measurements;
    }

    public String[] getShortMeasurements() {
        return shortMeasurements;
    }

    public String[] getLongMeasurements() {
        return longMeasurements;
    }

    public interface OnMeasurementChangeListener{
        void onMeasurementChange(MeasurementPicker measurementPicker, Measurement oldValue, Measurement newValue);
    }
    public enum UnitStyle{
        SHORT(0), LONG(1);
        private final int value;
        UnitStyle(int value){
            this.value = value;
        }
        public int getValue() {
            return value;
        }
        public static UnitStyle newInstanceFromValue(int value){
            switch (value){
                case 0:
                    return SHORT;
                case 1:
                    return LONG;
                default:
                    return null;
            }
        }
    }
    public enum Measurement{
        MILLIMETER("Millimeter"), CENTIMETER("Centimeter"), DECIMETER("Decimeter") ,METER("Meter"), KILOMETER("Kilometer");
        private final int intValue;
        private final String shortDisplayedValue;
        private final String longDisplayedValue;
        Measurement(String longDisplayedValue){
            this.longDisplayedValue = longDisplayedValue;
            switch (longDisplayedValue){
                case "Millimeter":
                    intValue = 0;
                    shortDisplayedValue = "mm";
                    break;
                case "Centimeter":
                    intValue = 1;
                    shortDisplayedValue = "cm";
                    break;
                case "Decimeter":
                    intValue = 2;
                    shortDisplayedValue = "dm";
                    break;
                case "Meter":
                    intValue = 3;
                    shortDisplayedValue = "m";
                    break;
                case "Kilometer":
                    intValue = 4;
                    shortDisplayedValue = "mm";
                    break;
                default:
                    intValue = -1;
                    shortDisplayedValue = "";
            }
        }

        public int getIntValue() {
            return intValue;
        }
        public String getShortDisplayedValue() {
            return shortDisplayedValue;
        }
        public String getLongDisplayedValue() {
            return longDisplayedValue;
        }
        public static Measurement newInstanceFromIntValue(int intValue){
            switch(intValue){
                case 0:
                    return MILLIMETER;
                case 1:
                    return CENTIMETER;
                case 2:
                    return DECIMETER;
                case 3:
                    return METER;
                case 4:
                    return KILOMETER;
                default:
                    throw new IllegalArgumentException(INVALID_INT_VALUE);

            }
        }
        public static Measurement newInstanceFromDisplayedValue(String value){
            value = value.toLowerCase();
            switch(value){
                case "mm":
                case "millimeter":
                    return MILLIMETER;
                case "cm":
                case "centimeter":
                    return CENTIMETER;
                case "dm":
                case "decimeter":
                    return DECIMETER;
                case "m":
                case "meter":
                    return METER;
                case "km":
                case "kilometer":
                    return KILOMETER;
                default:
                    throw new IllegalArgumentException(String.format(INVALID_MEASUREMENT, value));
            }
        }
    }
}
