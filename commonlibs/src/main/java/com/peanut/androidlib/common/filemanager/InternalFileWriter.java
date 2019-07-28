package com.peanut.androidlib.common.filemanager;
import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
public class InternalFileWriter {
    private static final String CONTEXT_IS_NULL = "Context cannot be null.";
    private static final String FULL_PATH_IS_NULL = "Full path cannot be null.";
    private static final String PARENT_DIRECTORY_IS_NULL = "Parent directory cannot be null.";
    private static final String FILE_NAME_IS_NULL = "File name cannot be null.";
    private static final String CONFIGURATION_SEPARATOR_IS_NULL = "Configuration separator cannot be null. Did you forget to call the method setConfigurationSeparator?";
    private static final String SEPARATOR_IS_CONTAINED_IN_KEY = "Separator cannot be contained in key. Key: %s, separator: %s.";
    private static final String SIZE_OF_LIST_OF_KEYS_AND_VALUES_DOES_NOT_MATCH = "The size of list of keys and values must match. Keys size: %d, values size: %d.";
    private Context context;
    private String parentDirectory;
    private String fileName;
    private String configurationSeparator;
    private String fullPath;
    private boolean append;
    private File file;
    private FileOutputStream fileOutputStream;
    private OutputStreamWriter outputStreamWriter;
    // Constructors
    public InternalFileWriter() {
    }
    public InternalFileWriter(String fullPath) {
        if (fullPath == null) {
            throw new IllegalArgumentException(FULL_PATH_IS_NULL);
        }
    }
    public InternalFileWriter(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        this.context = context;
    }
    public InternalFileWriter(Context context, String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException(FILE_NAME_IS_NULL);
        }
        this.context = context;
        this.fileName = fileName;
    }
    public InternalFileWriter(Context context, String parentDirectory, String fileName) {
        if (context == null) {
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        if (parentDirectory == null) {
            throw new IllegalArgumentException(PARENT_DIRECTORY_IS_NULL);
        }
        if (fileName == null) {
            throw new IllegalArgumentException(FILE_NAME_IS_NULL);
        }
        this.context = context;
        this.parentDirectory = parentDirectory;
        this.fileName = fileName;
    }
    // Interactive methods
    public void write(String content) {
        prepare();
        try {
            this.outputStreamWriter.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void write(String[] contents) {
        prepare();
        try {
            for (String content : contents) {
                this.outputStreamWriter.append(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void write(String[] contents, char separator) {
        prepare();
        try {
            for (String content : contents) {
                this.outputStreamWriter.append(content);
                this.outputStreamWriter.append(separator);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeLine(String line) {
        prepare();
        try {
            this.outputStreamWriter.append(line);
            this.outputStreamWriter.append(System.getProperty("line.separator"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeLines(String[] lines) {
        prepare();
        try {
            for (String line : lines) {
                this.outputStreamWriter.append(line);
                this.outputStreamWriter.append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeConfiguration(String key, String value) {
        if (this.configurationSeparator == null) {
            throw new RuntimeException(CONFIGURATION_SEPARATOR_IS_NULL);
        }
        if (key.contains(this.configurationSeparator)) {
            throw new RuntimeException(String.format(SEPARATOR_IS_CONTAINED_IN_KEY, key, this.configurationSeparator));
        }
        prepare();
        try {
            this.outputStreamWriter.append(key);
            this.outputStreamWriter.append(this.configurationSeparator);
            this.outputStreamWriter.append(value);
            this.outputStreamWriter.append(System.getProperty("line.separator"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeConfigurations(List<String> keys, List<String> values) {
        if (this.configurationSeparator == null) {
            throw new RuntimeException(CONFIGURATION_SEPARATOR_IS_NULL);
        }
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException(String.format(SIZE_OF_LIST_OF_KEYS_AND_VALUES_DOES_NOT_MATCH, keys.size(), values.size()));
        }
        for (String key : keys) {
            if (key.contains(this.configurationSeparator)) {
                throw new RuntimeException(String.format(SEPARATOR_IS_CONTAINED_IN_KEY, key, this.configurationSeparator));
            }
        }
        prepare();
        try {
            for (int i = 0; i < keys.size(); i++) {
                this.outputStreamWriter.append(keys.get(i));
                this.outputStreamWriter.append(this.configurationSeparator);
                this.outputStreamWriter.append(values.get(i));
                this.outputStreamWriter.append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void close() {
        this.file = null;
        try {
            this.outputStreamWriter.close();
            this.fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Helpers
    private void prepare() {
        if (this.file == null) {
            if (this.fullPath == null) {
                this.fullPath = buildFullPath(this.context, this.parentDirectory, this.fileName);
            }
            this.file = new File(this.fullPath);
            if (!this.file.getParentFile().exists()) {
                this.file.getParentFile().mkdirs();
            }
            try {
                this.fileOutputStream = new FileOutputStream(this.file, this.append);
                this.outputStreamWriter = new OutputStreamWriter(this.fileOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    private String buildFullPath(Context context, String parentDirectory, String fileName) {
        if (context == null) {
            throw new RuntimeException(CONTEXT_IS_NULL);
        }
        if (fileName == null) {
            throw new RuntimeException(FILE_NAME_IS_NULL);
        }
        String fullPath;
        if (this.parentDirectory != null) {
            fullPath = context.getFilesDir() + "/" + parentDirectory + "/" + fileName;
        } else {
            fullPath = context.getFilesDir() + "/" + fileName;
        }
        return fullPath.replace("//", "/");
    }
    public boolean exists() {
        return new File(buildFullPath(this.context, this.parentDirectory, this.fileName)).exists();
    }
    public boolean exists(String fullPath) {
        return new File(fullPath).exists();
    }
    public boolean exists(Context context, String fileName) {
        return new File(buildFullPath(context, null, fileName)).exists();
    }
    public boolean exists(Context context, String parentDirectory, String fileName) {
        return new File(buildFullPath(context, parentDirectory, fileName)).exists();
    }
    // Getters, setters
    public void setContext(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(CONTEXT_IS_NULL);
        }
        this.context = context;
    }
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }
    public void setParentDirectory(String parentDirectory) {
        if (parentDirectory == null) {
            throw new IllegalArgumentException(PARENT_DIRECTORY_IS_NULL);
        }
        this.parentDirectory = parentDirectory;
    }
    public void setFileName(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException(FILE_NAME_IS_NULL);
        }
        this.fileName = fileName;
    }
    public void setConfigurationSeparator(String configurationSeparator) {
        this.configurationSeparator = configurationSeparator;
    }
    public void setAppend(boolean append) {
        this.append = append;
    }
    public String getFullPath() {
        return this.fullPath;
    }
}