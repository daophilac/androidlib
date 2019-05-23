package com.peanut.androidlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.NumberPicker;

public class IntegerPicker extends NumberPicker {
    private static final String MAX_SMALLER_THAN_MIN = "baseMaxValue cannot be smaller than baseMinValue. baseMaxValue=%d, baseMinValue=%d";
    private static final String NEGATIVE_SELECTED_INDEX = "selectedIndex cannot be negative";
    private static final String OUT_OF_RANGE_SELECTED_INDEX = "selectedIndex is out of range. selectedIndex=%d, numElement=%d";
    public static final int defBaseMinValue = 0;
    public static final int defBaseMaxValue = 0;
    public static final int defMultiplicationFactor = 1;
    public static final int defSelectedIndex = 0;
    private int baseMinValue;
    private int baseMaxValue;
    private int multiplicationFactor;
    private int selectedIndex;
    private int numElement;
    private int[] values;
    private String[] displayValues;
    public IntegerPicker(Context context) {
        super(context);
    }
    public IntegerPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.IntegerPicker);
        baseMinValue = typedArray.getInt(R.styleable.IntegerPicker_baseMinValue, defBaseMinValue);
        baseMaxValue = typedArray.getInt(R.styleable.IntegerPicker_baseMaxValue, defBaseMaxValue);
        multiplicationFactor = typedArray.getInt(R.styleable.IntegerPicker_multiplicationFactor, defMultiplicationFactor);
        selectedIndex = typedArray.getInt(R.styleable.IntegerPicker_selectedIndex, defSelectedIndex);
        typedArray.recycle();
        perform();
    }
    public IntegerPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    private void validateValues(){
        if(baseMaxValue < baseMinValue){
            throw new IllegalArgumentException(String.format(MAX_SMALLER_THAN_MIN, baseMaxValue, baseMinValue));
        }
        if(selectedIndex < 0){
            throw new IllegalArgumentException(NEGATIVE_SELECTED_INDEX);
        }
        numElement = baseMaxValue - baseMinValue + 1;
        if(selectedIndex >= numElement){
            throw new IllegalArgumentException(String.format(OUT_OF_RANGE_SELECTED_INDEX, selectedIndex, numElement));
        }
    }
    private void generateDisplayedValues(){
        values = new int[numElement];
        displayValues = new String[numElement];
        for(int i = 0, value = (i + baseMinValue) * multiplicationFactor; i < numElement; i++, value += multiplicationFactor){
            values[i] = value;
            displayValues[i] = String.valueOf(value);
        }
        setDisplayedValues(displayValues);
        setMaxValue(numElement - 1);
        setValue(selectedIndex);
    }
    public void perform(){
        validateValues();
        generateDisplayedValues();
    }
    public void setBaseMinValue(int baseMinValue) {
        this.baseMinValue = baseMinValue;
    }

    public void setBaseMaxValue(int baseMaxValue) {
        this.baseMaxValue = baseMaxValue;
    }

    public void setMultiplicationFactor(int multiplicationFactor) {
        this.multiplicationFactor = multiplicationFactor;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        super.setOnValueChangedListener((picker, oldVal, newVal) -> onValueChangeListener.onValueChange(IntegerPicker.this, values[oldVal], values[newVal]));
    }

    public int getBaseMinValue() {
        return baseMinValue;
    }

    public int getBaseMaxValue() {
        return baseMaxValue;
    }

    public int getMultiplicationFactor() {
        return multiplicationFactor;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public int getNumElement() {
        return numElement;
    }

    public int[] getValues() {
        return values;
    }

    public String[] getDisplayValues() {
        return displayValues;
    }
}