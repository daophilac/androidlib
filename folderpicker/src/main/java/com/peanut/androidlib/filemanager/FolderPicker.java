package com.peanut.androidlib.filemanager;
import android.Manifest;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.peanut.androidlib.common.general.ExecutionTimeTracker;
import com.peanut.androidlib.common.permissionmanager.PermissionInquirer;
import com.peanut.androidlib.common.worker.MultipleWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class FolderPicker {
    public enum ParentMode {
        FULL_DIRECTORY, EXCLUDE_PARENT, NAME_ONLY
    }
    private static final String CONTAINS_INVALID_FILE_EXTENSION = "Map contains invalid file extensions. A valid file extension begins with a dot (.). For example: \".mp3\", \".txt\", \".mkv\", etc.";
    private static final String ICON_FOR_FILE_TYPE_HAS_ALREADY_BEEN_SET = "Icon for file type has already been set.";
    private static final String ICON_FOR_FOLDER_HAS_ALREADY_BEEN_SET = "Icon for folder has already been set.";
    private static final String NON_ZERO_SIZE_LIST_REQUIRED = "Non zero size list is required.";
    private static final String NON_ZERO_SIZE_SET_REQUIRED = "Non zero size set is required.";
    private static final String READ_EXTERNAL_STORAGE_PERMISSION_HAS_NOT_BEEN_GRANTED = "Read external storage permission has not been granted.";
    static final String TAG = "FolderPicker";
    private Context context;
    private int fragmentContainerId;
    private File startingDirectory;
    private Set<String> setFileCurrentlyChosen;
    private List<String> listDesiredFileExtension;
    private List<File> listAllFoundFile;
    private ParentFragment parentFragment;
    private ChildFragment childFragment;
    private FragmentManager fragmentManager;
    private Drawable iconFolder;
    private HashMap<String, Drawable> mapFileExtension;
    private boolean allowCheckFile = true;
    private boolean showFileToo;
    private ParentMode parentMode;
    private int parentFolderTextColor;
    private int folderTextColor;
    private int fileTextColor;
    private int navigationBarColor;
    private int bodyColor;
    private int itemBackgroundColor;
    private float parentFolderTextSize;
    private float folderTextSize;
    private float fileTextSize;
    private String validFileExtensionPattern;
    private FileComparator fileComparator;
    private MultipleWorker multipleWorker;
    private boolean preparing;
    private boolean prepared;
    private ExecutionTimeTracker executionTimeTracker;
    MultipleWorker getMultipleWorker() {
        return multipleWorker;
    }
    public FolderPicker(Context context, int fragmentContainerId, String startingDirectory) {
        this.multipleWorker = new MultipleWorker(TAG, 3);
        this.context = context;
        this.fragmentContainerId = fragmentContainerId;
        this.startingDirectory = new File(startingDirectory);
        this.fragmentManager = ((AppCompatActivity) this.context).getSupportFragmentManager();
        validate();

        this.multipleWorker.execute(new Runnable() {
            @Override
            public void run() {
                validFileExtensionPattern = "\\.[\\w\\d]+$";
                fileComparator = new FileComparator();
                listAllFoundFile = new ArrayList<>();
                listAllFoundFile.add(FolderPicker.this.startingDirectory);

                parentMode = ParentMode.FULL_DIRECTORY;
                parentFolderTextColor = FolderPicker.this.context.getResources().getColor(R.color.color_black);
                folderTextColor = FolderPicker.this.context.getResources().getColor(R.color.color_black);
                fileTextColor = FolderPicker.this.context.getResources().getColor(R.color.color_black);
                navigationBarColor = FolderPicker.this.context.getResources().getColor(R.color.color_white);
                bodyColor = FolderPicker.this.context.getResources().getColor(R.color.color_white);
                itemBackgroundColor = FolderPicker.this.context.getResources().getColor(R.color.color_white);
                parentFolderTextSize = FolderPicker.this.context.getResources().getDimension(R.dimen.text_size_normal);
                folderTextSize = FolderPicker.this.context.getResources().getDimension(R.dimen.text_size_small);
                fileTextSize = FolderPicker.this.context.getResources().getDimension(R.dimen.text_size_small);
            }
        });

        this.executionTimeTracker = new ExecutionTimeTracker();
    }
    private void validate() {
        PermissionInquirer permissionInquirer = new PermissionInquirer(this.context);
        if (!permissionInquirer.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            throw new IllegalStateException(READ_EXTERNAL_STORAGE_PERMISSION_HAS_NOT_BEEN_GRANTED);
        }
    }
    public void prepare() {
//        this.preparing = true;
//        this.prepared = false;
//
//        this.multipleWorker.execute(new Runnable() {
//            @Override
//            public void run() {
//                if(showFileToo){
//                    if(listDesiredFileExtension != null){
//                        listAllFoundFile.addAll(searchDesiredFileRecursively(startingDirectory));
//                    }
//                    else{
//                        listAllFoundFile.addAll(searchFileRecursively(startingDirectory));
//                    }
//                }
//                else{
//                    if(listDesiredFileExtension != null){
//                        listAllFoundFile.addAll(searchDesiredFolderRecursively(startingDirectory));
//                    }
//                    else{
//                        listAllFoundFile.addAll(searchFolderRecursively(startingDirectory));
//                    }
//                }
//                preparing = false;
//                prepared = true;
//            }
//        });
    }
    public void openFolderPicker() {
//        this.prepared = false;
//        this.preparing = false;
        this.multipleWorker.execute(new Runnable() {
            @Override
            public void run() {
                if (showFileToo) {
                    if (listDesiredFileExtension != null) {
                        listAllFoundFile.addAll(searchDesiredFileRecursively(startingDirectory));
                    } else {
                        listAllFoundFile.addAll(searchFileRecursively(startingDirectory));
                    }
                } else {
                    if (listDesiredFileExtension != null) {
                        listAllFoundFile.addAll(searchDesiredFolderRecursively(startingDirectory));
                    } else {
                        listAllFoundFile.addAll(searchFolderRecursively(startingDirectory));
                    }
                }
                preparing = false;
                prepared = true;
            }
        });
        this.parentFragment = new ParentFragment();
        this.childFragment = new ChildFragment();
        this.childFragment.configure(FolderPicker.this, this.fragmentManager, this.startingDirectory, this.startingDirectory, this.setFileCurrentlyChosen.contains(this.startingDirectory.getAbsolutePath()));
        FragmentTransaction fragmentTransaction = this.fragmentManager.beginTransaction();
        fragmentTransaction.add(this.fragmentContainerId, this.parentFragment);
        fragmentTransaction.add(R.id.frame_layout_child_container, this.childFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
//        if(!this.prepared){
//            if(!preparing){
//                prepare();
//            }
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    while(!prepared){
//                        try {
//                            Thread.sleep(500);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    prepared = false;
//                    preparing = false;
//                    parentFragment = new ParentFragment();
//                    childFragment = new ChildFragment();
//                    childFragment.configure(FolderPicker.this, fragmentManager, startingDirectory, startingDirectory, setFileCurrentlyChosen.contains(startingDirectory.getAbsolutePath()));
//                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//                    fragmentTransaction.add(fragmentContainerId, parentFragment);
//                    fragmentTransaction.add(R.id.frame_layout_child_container, childFragment);
//                    fragmentTransaction.addToBackStack(null);
//                    fragmentTransaction.commit();
//
//                }
//            }).start();
//        }
//        else{
//            this.prepared = false;
//            this.preparing = false;
//            this.parentFragment = new ParentFragment();
//            this.childFragment = new ChildFragment();
//            this.childFragment.configure(FolderPicker.this, this.fragmentManager, this.startingDirectory, this.startingDirectory, this.setFileCurrentlyChosen.contains(this.startingDirectory.getAbsolutePath()));
//            FragmentTransaction fragmentTransaction = this.fragmentManager.beginTransaction();
//            fragmentTransaction.add(this.fragmentContainerId, this.parentFragment);
//            fragmentTransaction.add(R.id.frame_layout_child_container, this.childFragment);
//            fragmentTransaction.addToBackStack(null);
//            fragmentTransaction.commit();
//        }
    }
    List<File> retrieveChildFile(File startingDirectory) {
        executionTimeTracker.startTracking();
        List<File> listChildFile = new ArrayList<>();
        int beginIndex = this.listAllFoundFile.indexOf(startingDirectory);
        boolean gotToFileSection = false;
        for (int i = beginIndex + 1; i < this.listAllFoundFile.size(); i++) {
            File file = this.listAllFoundFile.get(i);
            if (!file.getParent().equals(startingDirectory.getAbsolutePath())) {
                if (gotToFileSection) {
                    break;
                }
            } else {
                if (file.isFile()) {
                    gotToFileSection = true;
                }
                listChildFile.add(file);
            }
        }
        executionTimeTracker.stopTracking();
        FolderPicker.writeLogVerbose("retrieveChildFile: " + executionTimeTracker.getExecutionTime());
        return listChildFile;
    }
    private List<File> searchFolderRecursively(File startingDirectory) {
        List<File> listFolder = new ArrayList<>();
        File[] arrayFile = startingDirectory.listFiles();
        Arrays.sort(arrayFile, this.fileComparator);
        for (File file : arrayFile) {
            if (file.isDirectory()) {
                listFolder.add(file);
            }
        }
        return listFolder;
    }
    private List<File> searchDesiredFolderRecursively(File startingDirectory) {
        List<File> listFolder = new ArrayList<>();
        File[] arrayFile = startingDirectory.listFiles();
        Arrays.sort(arrayFile, this.fileComparator);
        for (File file : arrayFile) {
            if (file.isDirectory()) {
                if (isDirectoryContainDesiredFile(file)) {
                    listFolder.add(file);
                }
            }
        }
        return listFolder;
    }
    private List<File> searchFileRecursively(File startingDirectory) {
        List<File> listFile = new ArrayList<>();
        File[] arrayFile = startingDirectory.listFiles();
        Arrays.sort(arrayFile, this.fileComparator);
        for (File file : arrayFile) {
            if (file.isDirectory()) {
                List<File> listFileRecursiveCall = searchFileRecursively(file);
                if (listFileRecursiveCall.size() != 0) {
                    listFile.add(file);
                    listFile.addAll(listFileRecursiveCall);
                }
            }
            if (file.isFile()) {
                listFile.add(file);
            }
        }
        return listFile;
    }
    private List<File> searchDesiredFileRecursively(File startingDirectory) {
        List<File> listFile = new ArrayList<>();
        File[] arrayFile = startingDirectory.listFiles();
        Arrays.sort(arrayFile, this.fileComparator);
        for (File file : arrayFile) {
            if (file.isDirectory()) {
                List<File> listFileRecursiveCall = searchDesiredFileRecursively(file);
                if (listFileRecursiveCall.size() != 0) {
                    listFile.add(file);
                    listFile.addAll(listFileRecursiveCall);
                }
            } else if (isDesiredFile(file)) {
                listFile.add(file);
            }
        }
        return listFile;
    }
    private boolean isDirectoryContainDesiredFile(File startingDirectory) {
        File[] arrayFile = startingDirectory.listFiles();
        for (File file : arrayFile) {
            if (file.isDirectory()) {
                return isDirectoryContainDesiredFile(file);
            } else if (isDesiredFile(file)) {
                return true;
            }
        }
        return false;
    }
    private boolean isDesiredFile(File file) {
        String fileName = file.getName().toLowerCase();
        for (String s : this.listDesiredFileExtension) {
            if (fileName.endsWith(s)) {
                return true;
            }
        }
        return false;
    }
    private class FileComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            if ((o1.isDirectory() && o2.isDirectory()) || (o1.isFile() && o2.isFile())) {
                return o1.getAbsolutePath().compareToIgnoreCase(o2.getAbsolutePath());
            } else {
                return o1.isDirectory() ? -1 : 1;
            }
        }
    }
    public void setSetFileCurrentlyChosen(Set<String> setFileCurrentlyChosen) {
        this.setFileCurrentlyChosen = setFileCurrentlyChosen;
        if (this.setFileCurrentlyChosen != null) {
            if (this.setFileCurrentlyChosen.size() == 0) {
                throw new IllegalArgumentException(NON_ZERO_SIZE_SET_REQUIRED);
            }
        }
    }
    public void setSetFileCurrentlyChosen(List<String> listFileCurrentlyChosen) {
        if (listFileCurrentlyChosen != null) {
            if (listFileCurrentlyChosen.size() == 0) {
                throw new IllegalArgumentException(NON_ZERO_SIZE_LIST_REQUIRED);
            }
            this.setFileCurrentlyChosen = new HashSet<>(listFileCurrentlyChosen);
        }
    }
    public void setListDesiredFileExtension(List<String> listDesiredFileExtension) {
        this.listDesiredFileExtension = listDesiredFileExtension;
        if (this.listDesiredFileExtension != null) {
            if (this.listDesiredFileExtension.size() == 0) {
                throw new IllegalArgumentException(NON_ZERO_SIZE_LIST_REQUIRED);
            }
            for (int i = 0; i < this.listDesiredFileExtension.size(); i++) {
                this.listDesiredFileExtension.set(i, this.listDesiredFileExtension.get(i).toLowerCase());
            }
        }
    }
    private boolean isValidDesiredFileExtension(List<String> listFileExtension) {
        for (String extension : listFileExtension) {
            if (!extension.matches(this.validFileExtensionPattern)) {
                return false;
            }
        }
        return true;
    }
    void setParentFolderNameModeFull(ParentMode parentMode) {
        this.parentMode = parentMode;
    }
    void setIconFolderDrawableId(Integer iconFolderDrawableId) {
        if (this.iconFolder == null) {
            this.iconFolder = this.context.getResources().getDrawable(iconFolderDrawableId);
        } else {
            throw new IllegalStateException(ICON_FOR_FOLDER_HAS_ALREADY_BEEN_SET);
        }
    }
    void setIconFolder(Drawable iconFolder) {
        if (this.iconFolder == null) {
            this.iconFolder = iconFolder;
        } else {
            throw new IllegalStateException(ICON_FOR_FOLDER_HAS_ALREADY_BEEN_SET);
        }
    }
    public void setIconForFileTypeFromDrawableId(HashMap<String, Integer> mapFileExtensionDrawableId) {
        if (this.mapFileExtension == null) {
            if (!isValidDesiredFileExtension(new ArrayList<>(mapFileExtensionDrawableId.keySet()))) {
                throw new IllegalArgumentException(CONTAINS_INVALID_FILE_EXTENSION);
            }
            this.mapFileExtension = new HashMap<>();
            for (Map.Entry<String, Integer> entry : mapFileExtensionDrawableId.entrySet()) {
                this.mapFileExtension.put(entry.getKey(), this.context.getDrawable(entry.getValue()));
            }
        } else {
            throw new IllegalStateException(ICON_FOR_FILE_TYPE_HAS_ALREADY_BEEN_SET);
        }
    }
    public void setIconForFileTypeFromDrawable(HashMap<String, Drawable> mapFileExtensionDrawable) {
        if (this.mapFileExtension == null) {
            if (!isValidDesiredFileExtension(new ArrayList<>(mapFileExtensionDrawable.keySet()))) {
                throw new IllegalArgumentException(CONTAINS_INVALID_FILE_EXTENSION);
            }
            this.mapFileExtension = mapFileExtensionDrawable;
        } else {
            throw new IllegalStateException(ICON_FOR_FILE_TYPE_HAS_ALREADY_BEEN_SET);
        }
    }
    public void setParentFolderTextColor(int color) {
        this.parentFolderTextColor = color;
    }
    public void setFolderTextColor(int color) {
        this.folderTextColor = color;
    }
    public void setFileTextColor(int color) {
        this.fileTextColor = color;
    }
    public void setNavigationBarColor(int color) {
        this.navigationBarColor = color;
    }
    public void setBodyColor(int color) {
        this.bodyColor = color;
    }
    public void setItemBackgroundColor(int color) {
        this.itemBackgroundColor = color;
    }
    public void setParentFolderTextSize(int resId) {
        this.parentFolderTextSize = this.context.getResources().getDimension(resId);
    }
    public void setFolderTextSize(int resId) {
        this.folderTextSize = this.context.getResources().getDimension(resId);
    }
    public void setFileTextSize(int resId) {
        this.fileTextSize = this.context.getResources().getDimension(resId);
    }
    public void setParentFolderTextSize(float size) {
        this.parentFolderTextSize = size;
    }
    public void setFolderTextSize(float size) {
        this.folderTextSize = size;
    }
    public void setFileTextSize(float size) {
        this.fileTextSize = size;
    }
    public void setShowFileToo(boolean showFileToo) {
        this.showFileToo = showFileToo;
    }
    public List<File> getListAllFoundFile() {
        return listAllFoundFile;
    }
    public File getStartingDirectory() {
        return startingDirectory;
    }
    Set<String> getSetFileCurrentlyChosen() {
        return setFileCurrentlyChosen;
    }
    List<String> getListDesiredFileExtension() {
        return listDesiredFileExtension;
    }
    public ParentMode getParentMode() {
        return parentMode;
    }
    public Drawable getIconFolder() {
        return iconFolder;
    }
    public HashMap<String, Drawable> getMapFileExtension() {
        return mapFileExtension;
    }
    public int getParentFolderTextColor() {
        return parentFolderTextColor;
    }
    public int getFolderTextColor() {
        return folderTextColor;
    }
    public int getFileTextColor() {
        return fileTextColor;
    }
    public int getNavigationBarColor() {
        return navigationBarColor;
    }
    public int getBodyColor() {
        return bodyColor;
    }
    public int getItemBackgroundColor() {
        return itemBackgroundColor;
    }
    public float getParentFolderTextSize() {
        return parentFolderTextSize;
    }
    public float getFolderTextSize() {
        return folderTextSize;
    }
    public float getFileTextSize() {
        return fileTextSize;
    }
    public boolean getAllowCheckFile() {
        return allowCheckFile;
    }
    public boolean isShowFileToo() {
        return showFileToo;
    }
    static void writeLogVerbose(String message) {
        Log.v(TAG, message);
    }
}