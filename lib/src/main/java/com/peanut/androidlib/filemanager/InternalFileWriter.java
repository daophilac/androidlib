package com.peanut.androidlib.filemanager;

import android.Manifest;
import android.content.Context;

import com.peanut.androidlib.permissionmanager.PermissionInquirer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class InternalFileWriter {
    private static final String CONTEXT_IS_NULL = "Context cannot be null.";
    private static final String READ_EXTERNAL_STORAGE_PERMISSION_HAS_NOT_BEEN_GRANTED = "Read external storage permission has not been granted.";
    private Context context;
    private String directoryContainer;
    private String fileName;
    private String fullPath;
    private boolean useBaseDirectory;

    private PermissionInquirer permissionInquirer;
    public InternalFileWriter(Context context, String fileName){
        if(context == null){
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        this.context = context;
        this.permissionInquirer = new PermissionInquirer(this.context);
        if(!this.permissionInquirer.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)){
            throw new IllegalStateException(READ_EXTERNAL_STORAGE_PERMISSION_HAS_NOT_BEEN_GRANTED);
        }
        this.fileName = fileName;
        this.fullPath = this.context.getFilesDir().getAbsolutePath() + "/" + this.fileName;
        this.useBaseDirectory = true;
    }
    public InternalFileWriter(Context context, String directoryContainer, String fileName){
        if(context == null){
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        this.context = context;
        this.permissionInquirer = new PermissionInquirer(this.context);
        if(!this.permissionInquirer.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)){
            throw new IllegalStateException(READ_EXTERNAL_STORAGE_PERMISSION_HAS_NOT_BEEN_GRANTED);
        }
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
    public void write(String content, boolean append){
        if(!this.useBaseDirectory){
            File directory = new File(this.context.getFilesDir() + "/" + this.directoryContainer);
            if(!directory.exists()){
                directory.mkdirs();
            }
        }
        File file = new File(this.fullPath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, append);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.append(content);
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void write(String[] contents, boolean append){
        if(!this.useBaseDirectory){
            File directory = new File(this.context.getFilesDir() + "/" + this.directoryContainer);
            if(!directory.exists()){
                directory.mkdirs();
            }
        }
        File file = new File(this.fullPath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, append);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (String content : contents) {
                outputStreamWriter.append(content);
            }
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void write(String[] contents, char separator, boolean append){
        if(!this.useBaseDirectory){
            File directory = new File(this.context.getFilesDir() + "/" + this.directoryContainer);
            if(!directory.exists()){
                directory.mkdirs();
            }
        }
        File file = new File(this.fullPath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, append);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (String content : contents) {
                outputStreamWriter.append(content);
                outputStreamWriter.append(separator);
            }
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeLine(String content, boolean append){
        if(!this.useBaseDirectory){
            File directory = new File(this.context.getFilesDir() + "/" + this.directoryContainer);
            if(!directory.exists()){
                directory.mkdirs();
            }
        }
        File file = new File(this.fullPath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, append);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            String separator = System.getProperty("line.separator");
            outputStreamWriter.append(content);
            outputStreamWriter.append(separator);
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeLines(String[] contents, boolean append){
        if(!this.useBaseDirectory){
            File directory = new File(this.context.getFilesDir() + "/" + this.directoryContainer);
            if(!directory.exists()){
                directory.mkdirs();
            }
        }
        File file = new File(this.fullPath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, append);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            String separator = System.getProperty("line.separator");
            for (String content : contents) {
                outputStreamWriter.append(content);
                outputStreamWriter.append(separator);
            }
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}