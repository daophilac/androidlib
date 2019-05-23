package com.peanut.androidlib.common.filemanager;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class InternalFileReader {
    private static final String CONTEXT_IS_NULL = "Context cannot be null.";
    private static final String FULL_PATH_IS_NULL = "Full path cannot be null.";
    private static final String PARENT_DIRECTORY_IS_NULL = "Parent directory cannot be null.";
    private static final String FILE_NAME_IS_NULL = "File name cannot be null.";
    private static final String FILE_DOES_NOT_EXIST = "File does not exist.";
    private static final String LINE_DOES_NOT_CONTAIN_SEPARATOR = "Detect a line that does not contain the separator. Line: %s, separator: %s.";
    private static final String CONFIGURATION_SEPARATOR_IS_NULL = "Configuration separator cannot be null. Did you forget to call the method setConfigurationSeparator?";
    private static final String KEY_NOT_FOUND = "Key %s not found.";
    private Context context;
    private String parentDirectory;
    private String fileName;

    private HashMap<String, String> mapConfiguration;
    private String configurationSeparator;
    private String fullPath;
    private File file;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;


    // Constructors
    public InternalFileReader(){}
    public InternalFileReader(String fullPath){
        if(fullPath == null){
            throw new IllegalArgumentException(FULL_PATH_IS_NULL);
        }
        this.fullPath = fullPath;
    }
    public InternalFileReader(Context context){
        if(context == null){
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        this.context = context;
    }
    public InternalFileReader(Context context, String fileName){
        if(fileName == null){
            throw new IllegalArgumentException(FILE_NAME_IS_NULL);
        }
        this.context = context;
        this.fileName = fileName;
    }
    public InternalFileReader(Context context, String parentDirectory, String fileName){
        if(context == null){
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        if(parentDirectory == null){
            throw new IllegalArgumentException(PARENT_DIRECTORY_IS_NULL);
        }
        if(fileName == null){
            throw new IllegalArgumentException(FILE_NAME_IS_NULL);
        }
        this.context = context;
        this.parentDirectory = parentDirectory;
        this.fileName = fileName;
    }


    // Interactive methods
    public String readLine(){
        if(this.file == null){
            if(this.fullPath == null){
                this.fullPath = buildFullPath(this.context, this.parentDirectory, this.fileName);
            }
            this.file = new File(this.fullPath);
            if(!this.file.exists()){
                throw new RuntimeException(FILE_DOES_NOT_EXIST);
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
            return this.bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String readConfiguration(String key){
        if(this.configurationSeparator == null){
            throw new RuntimeException(CONFIGURATION_SEPARATOR_IS_NULL);
        }
        if(this.mapConfiguration == null){
            prepareConfiguration();
        }
        if(!this.mapConfiguration.containsKey(key)){
            throw new RuntimeException(String.format(KEY_NOT_FOUND, key));
        }
        return this.mapConfiguration.get(key);
    }
    public HashMap<String, String> readConfigurations(List<String> keys){
        if(this.configurationSeparator == null){
            throw new RuntimeException(CONFIGURATION_SEPARATOR_IS_NULL);
        }
        if(this.mapConfiguration == null){
            prepareConfiguration();
        }
        HashMap<String, String> result = new HashMap<>();
        for(String key : keys){
            if(!this.mapConfiguration.containsKey(key)){
                throw new RuntimeException(String.format(KEY_NOT_FOUND, key));
            }
            result.put(key, this.mapConfiguration.get(key));
        }
        return result;
    }
    private void prepareConfiguration(){
        this.mapConfiguration = new HashMap<>();
        int separatorLength = this.configurationSeparator.length();
        String line = readLine();
        while(line != null){
            if(!line.contains(this.configurationSeparator)){
                throw new RuntimeException(String.format(LINE_DOES_NOT_CONTAIN_SEPARATOR, line, this.configurationSeparator));
            }
            String key = line.substring(0, line.indexOf(this.configurationSeparator));
            String value = line.substring(key.length() + separatorLength);
            this.mapConfiguration.put(key, value);
            line = readLine();
        }
        close();
    }
    public void close(){
        this.file = null;
        try {
            this.bufferedReader.close();
            this.inputStreamReader.close();
            this.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // Helpers
    private String buildFullPath(Context context, String parentDirectory, String fileName){
        if(context == null){
            throw new RuntimeException(CONTEXT_IS_NULL);
        }
        if(fileName == null){
            throw new RuntimeException(FILE_NAME_IS_NULL);
        }
        String fullPath;
        if(this.parentDirectory != null){
            fullPath = context.getFilesDir() + "/" + parentDirectory + "/" + fileName;
        }
        else{
            fullPath = context.getFilesDir() + "/" + fileName;
        }
        return fullPath.replace("//", "/");
    }
    public boolean exists(){
        return new File(buildFullPath(this.context, this.parentDirectory, this.fileName)).exists();
    }
    public boolean exists(String fullPath){
        return new File(fullPath).exists();
    }
    public boolean exists(Context context, String fileName){
        return new File(buildFullPath(context, null, fileName)).exists();
    }
    public boolean exists(Context context, String parentDirectory, String fileName){
        return new File(buildFullPath(context, parentDirectory, fileName)).exists();
    }



    // Getters, setters
    public void setContext(Context context) {
        if(context == null){
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        this.context = context;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public void setParentDirectory(String parentDirectory) {
        if(parentDirectory == null){
            throw new IllegalArgumentException(PARENT_DIRECTORY_IS_NULL);
        }
        this.parentDirectory = parentDirectory;
    }

    public void setFileName(String fileName) {
        if(fileName == null){
            throw new IllegalArgumentException(FILE_NAME_IS_NULL);
        }
        this.fileName = fileName;
    }

    public void setConfigurationSeparator(String configurationSeparator) {
        this.configurationSeparator = configurationSeparator;
    }

    public String getFullPath(){
        return this.fullPath;
    }
}
