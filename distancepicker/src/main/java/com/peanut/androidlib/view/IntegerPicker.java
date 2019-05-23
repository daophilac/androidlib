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
    private static final String NEGATIVE_SCROLL_AMOUNT = "scroll amount cannot be negative";
    private static final String SCROLL_PASS_FINAL_ELEMENT = "scroll passes final element. selectedIndex=%d, numElement=%d, scroll=%d";
    private static final String SCROLL_PASS_FIRST_ELEMENT = "scroll passes first element. selectedIndex=%d, scroll=%d";
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
        // TODO
        if(numElement > 5000){
            throw new RuntimeException("There are many elements");
        }
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
        setDisplayedValues(null);
        setMaxValue(numElement - 1);
        setValue(selectedIndex);
        setDisplayedValues(displayValues);
    }
    public void updateWrapSelectorWheel(boolean wrapSelectorWheel){
        setWrapSelectorWheel(wrapSelectorWheel);
        setDisplayedValues(displayValues);
    }
    public void perform(){
        validateValues();
        generateDisplayedValues();
    }
    public void scrollDown(int scroll){
        if(scroll < 0){
            throw new IllegalArgumentException(NEGATIVE_SCROLL_AMOUNT);
        }
        if(selectedIndex + scroll >= numElement){
            throw new RuntimeException(String.format(SCROLL_PASS_FINAL_ELEMENT, selectedIndex, numElement, scroll));
        }
        selectedIndex += scroll;
        setValue(selectedIndex);
    }
    public void scrollUp(int scroll){
        if(scroll < 0){
            throw new IllegalArgumentException(NEGATIVE_SCROLL_AMOUNT);
        }
        if(selectedIndex - scroll < 0){
            throw new RuntimeException(String.format(SCROLL_PASS_FIRST_ELEMENT, selectedIndex, scroll));
        }
        selectedIndex -= scroll;
        setValue(selectedIndex);
    }
    public IntegerPicker setBaseMinValue(int baseMinValue) {
        this.baseMinValue = baseMinValue;
        return this;
    }

    public IntegerPicker setBaseMaxValue(int baseMaxValue) {
        this.baseMaxValue = baseMaxValue;
        return this;
    }

    public IntegerPicker setMultiplicationFactor(int multiplicationFactor) {
        this.multiplicationFactor = multiplicationFactor;
        return this;
    }

    public IntegerPicker setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
        return this;
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