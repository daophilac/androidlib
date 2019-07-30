package com.peanut.androidlib.filemanager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.support.v4.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {
    private Context context;
    private FolderPicker folderPicker;
    private FragmentManager fragmentManager;
    private File startingDirectory;
    private CheckBox checkBoxSelectAll;
    private List<File> listAllFoundFile;
    private List<File> listChildFile;
    private Set<String> setFileCurrentlyChosen;
    private Set<File> setFileChecked;
    private Set<CheckBox> setCheckBox;
    private boolean allowCheckFile;
    private boolean isSelectedAll;
//    private int totalCheckedFile;
    private Drawable iconFolderDrawable;
    private HashMap<String, Drawable> mapFileExtensionDrawable;
    private int folderTextColor;
    private int fileTextColor;
    private int itemBackgroundColor;
    private float folderTextSize;
    private float fileTextSize;
    private Set<String> checkFolderRecursively(File startingDirectory) {
        Set<String> setPath = new HashSet<>();
        File[] arrayFile = startingDirectory.listFiles();
        for (File file : arrayFile) {
            if (this.listAllFoundFile.contains(file)) {
                this.setFileCurrentlyChosen.add(file.getAbsolutePath());
                if (file.isDirectory()) {
                    this.setFileCurrentlyChosen.addAll(checkFolderRecursively(file));
                }
            }
        }
        return setPath;
    }
    FolderAdapter(Context context, FolderPicker folderPicker, FragmentManager fragmentManager, final File startingDirectory, final CheckBox checkBoxSelectAll, boolean isSelectedAll) {
        this.context = context;
        this.folderPicker = folderPicker;
        this.fragmentManager = fragmentManager;
        this.startingDirectory = startingDirectory;
        this.checkBoxSelectAll = checkBoxSelectAll;
        this.isSelectedAll = isSelectedAll;

        this.allowCheckFile = this.folderPicker.getAllowCheckFile();
        this.listChildFile = this.folderPicker.retrieveChildFile(this.startingDirectory);
        this.listAllFoundFile = this.folderPicker.getListAllFoundFile();
        this.setFileCurrentlyChosen = this.folderPicker.getSetFileCurrentlyChosen();
        this.iconFolderDrawable = this.folderPicker.getIconFolder();
        this.mapFileExtensionDrawable = this.folderPicker.getMapFileExtension();
        this.folderTextColor = this.folderPicker.getFolderTextColor();
        this.fileTextColor = this.folderPicker.getFileTextColor();
        this.itemBackgroundColor = this.folderPicker.getItemBackgroundColor();
        this.folderTextSize = this.folderPicker.getFolderTextSize();
        this.fileTextSize = this.folderPicker.getFileTextSize();

        if (this.isSelectedAll) {
            this.checkBoxSelectAll.setChecked(true);
        }

        this.checkBoxSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBoxSelectAll.isChecked()) {
                    checkBoxSelectAll.setText(R.string.select_none);
                } else {
                    checkBoxSelectAll.setText(R.string.select_all);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        checkFolderRecursively(startingDirectory);
                    }
                }).start();
                for (CheckBox cb : setCheckBox) {
                    cb.setChecked(checkBoxSelectAll.isChecked());
                }
            }
        });
        this.setFileChecked = new HashSet<>();
        this.setCheckBox = new HashSet<>();
//        for(File file : this.listChildFile){
//            this.mapFileChecked.put(file, false);
//        }
        for (String s : this.setFileCurrentlyChosen) {
            File f = new File(s);
            if (this.allowCheckFile || f.isDirectory()) {
                if (f.getParent().equals(startingDirectory.getAbsolutePath())) {
                    this.setFileChecked.add(f);
                }
            }
        }
    }
    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(this.context);
        View itemView = layoutInflater.inflate(R.layout.item_folder_picker, viewGroup, false);
        return new FolderViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder folderViewHolder, int i) {
        final File file = this.listChildFile.get(i);
        ConstraintLayout constraintLayoutFolderItem = folderViewHolder.constraintLayoutFolderItem;
        CheckBox checkBoxSelect = folderViewHolder.checkBoxSelect;
        ImageView imageViewFile = folderViewHolder.imageViewFile;
        TextView textViewFile = folderViewHolder.textViewFile;

        checkBoxSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                CheckBox checkBox = (CheckBox)v;
                if (((CheckBox) v).isChecked()) {
                    if (allowCheckFile) {
                        setFileChecked.add(file);
                        if (file.isDirectory()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    if (file.isDirectory()) {
                                        setFileChecked.add(file);
                                        checkFolderRecursively(file);
                                    }
                                }
                            }).start();
                        }
                        if (setFileChecked.size() == listChildFile.size()) {
                            checkBoxSelectAll.setChecked(true);
                        }
                    } else {
                        setFileChecked.add(file);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (file.isDirectory()) {
                                    setFileChecked.add(file);
                                    checkFolderRecursively(file);
                                }
                            }
                        }).start();
                        for (int i = 0, n = 0; i < listChildFile.size(); i++) {
                            if (listChildFile.get(i).isDirectory()) {
                                n++;
                                if (n == setFileChecked.size()) {
                                    checkBoxSelectAll.setChecked(true);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    setFileChecked.remove(file);
                    checkBoxSelectAll.setChecked(false);

//                    Boolean b = mapFileChecked.get(file);
//                    if(b != null && b){
//                        mapFileChecked.put(file, false);
//                        totalCheckedFile--;
//                        if(checkBoxSelectAll.isChecked()){
//                            checkBoxSelectAll.setChecked(false);
//                            setFileCurrentlyChosen.remove(startingDirectory.getAbsolutePath());
//                        }
//                    }
                }
            }
        });

        if (file.isDirectory()) {
            this.setCheckBox.add(checkBoxSelect);
            if (this.setFileChecked.contains(file)) {
                checkBoxSelect.setChecked(true);
            } else {
                checkBoxSelect.setChecked(false);
            }
            textViewFile.setTextColor(this.folderTextColor);
            textViewFile.setTextSize(this.folderTextSize);
            imageViewFile.setImageDrawable(this.iconFolderDrawable);
        } else {
            if (!this.allowCheckFile) {
                checkBoxSelect.setVisibility(View.INVISIBLE);
            } else {
                this.setCheckBox.add(checkBoxSelect);
                if (this.setFileChecked.contains(file)) {
                    checkBoxSelect.setChecked(true);
                } else {
                    checkBoxSelect.setChecked(false);
                }
            }
            textViewFile.setTextColor(this.fileTextColor);
            textViewFile.setTextSize(this.fileTextSize);
            String extension = file.getName().substring(file.getName().lastIndexOf("."));
            Drawable drawable = this.mapFileExtensionDrawable.get(extension);
            if (drawable != null) {
                imageViewFile.setImageDrawable(drawable);
            }
        }

        textViewFile.setText(file.getName());
        constraintLayoutFolderItem.setBackgroundColor(this.itemBackgroundColor);
        constraintLayoutFolderItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file.isDirectory()) {
                    ChildFragment childFragment = new ChildFragment();
                    childFragment.configure(folderPicker, fragmentManager, startingDirectory, file, setFileChecked.contains(file));
                    fragmentManager.beginTransaction().add(R.id.frame_layout_child_container, childFragment).addToBackStack(null).commit();
                }
            }
        });
    }
    @Override
    public int getItemCount() {
        return this.listChildFile.size();
    }
    class FolderViewHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout constraintLayoutFolderItem;
        private CheckBox checkBoxSelect;
        private ImageView imageViewFile;
        private TextView textViewFile;
        private FolderViewHolder(View itemView) {
            super(itemView);
            this.constraintLayoutFolderItem = itemView.findViewById(R.id.constraint_layout_folder_item);
            this.checkBoxSelect = itemView.findViewById(R.id.check_box_select);
            this.imageViewFile = itemView.findViewById(R.id.image_view_file);
            this.textViewFile = itemView.findViewById(R.id.text_view_file);
        }
    }
}