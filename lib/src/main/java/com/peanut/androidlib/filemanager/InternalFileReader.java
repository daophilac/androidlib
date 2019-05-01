package com.peanut.androidlib.filemanager;

import android.content.Context;
import android.content.ContextWrapper;

import com.peanut.androidlib.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class InternalFileReader {
    private static final String CONTEXT_IS_NULL = "Context cannot be null.";
    private Context context;
    private String directoryContainer;
    private String fileName;
    private String fullPath;
    private boolean useBaseDirectory;
    private File file;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    public InternalFileReader(Context context, String fileName){
        if(context == null){
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        this.context = context;
        this.fileName = fileName;
        this.fullPath = this.context.getFilesDir() + "/" + this.fileName;
        this.useBaseDirectory = true;
    }
    public InternalFileReader(Context context, String directoryContainer, String fileName){
        if(context == null){
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        this.context = context;
        this.fileName = fileName;
        if(directoryContainer.charAt(0) == '/'){
            if(directoryContainer.charAt(directoryContainer.length() - 1) == '/'){
                this.directoryContainer = directoryContainer.substring(1, directoryContainer.length() - 1);
            }
            else{
                this.directoryContainer = directoryContainer.substring(1);
            }
        }
        else{
            if(directoryContainer.charAt(directoryContainer.length() - 1) == '/'){
                this.directoryContainer = directoryContainer.substring(0, directoryContainer.length() - 1);
            }
            else{
                this.directoryContainer = directoryContainer;
            }
        }
        this.fullPath = this.context.getFilesDir() + "/" + this.directoryContainer + "/" + this.fileName;
        this.useBaseDirectory = false;
    }
    public String getFullPath(){
        return this.fullPath;
    }
    public boolean exists(String fileName){
        File file = new File(this.context.getFilesDir() + "/" + fileName);
        return file.exists();
    }
    public boolean exists(String directoryContainer, String fileName){
        if(directoryContainer.charAt(0) == '/'){
            if(directoryContainer.charAt(directoryContainer.length() - 1) == '/'){
                directoryContainer = directoryContainer.substring(1, directoryContainer.length() - 1);
            }
            else{
                directoryContainer = directoryContainer.substring(1);
            }
        }
        else{
            if(directoryContainer.charAt(directoryContainer.length() - 1) == '/'){
                directoryContainer = directoryContainer.substring(0, directoryContainer.length() - 1);
            }
        }
        File file = new File(this.context.getFilesDir() + "/" + directoryContainer + "/" + fileName);
        return file.exists();
    }
    public String readLine(){
        String line = "";
        if(this.file == null){
            this.file = new File(this.fullPath);
            if(!this.file.exists()){
                throw new RuntimeException(String.format(this.context.getString(R.string.file_does_not_exist), this.fileName));
            }
            else{
                try {
                    this.fileInputStream = new FileInputStream(this.file);
                    this.inputStreamReader = new InputStreamReader(this.fileInputStream);
                    this.bufferedReader = new BufferedReader(this.inputStreamReader);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            line = this.bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }
    public void close(){
        try {
            this.bufferedReader.close();
            this.inputStreamReader.close();
            this.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
