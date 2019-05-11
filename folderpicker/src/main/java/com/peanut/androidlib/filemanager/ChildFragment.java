package com.peanut.androidlib.filemanager;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.peanut.androidlib.worker.MultipleWorker;

import java.io.File;

public class ChildFragment extends Fragment {
    private static final String IS_NOT_OPENED_FROM_FOLDER_PICKER = "You should use the class FolderPicker and its openFolderPicker method. Avoid directly creating and using an instance of ChildFragment like this.";
    private Context context;
    private FragmentManager fragmentManager;
    private FolderPicker folderPicker;
    private File parentDirectory;
    private File startingDirectory;
    private Boolean isSelectedAll;

    private ConstraintLayout constraintLayoutOperation;
    private ImageButton imageButtonBack;
    private TextView textViewFolderName;
    private CheckBox checkBoxSelectAll;
    private RecyclerView recyclerViewListFile;
    private FolderAdapter folderAdapter;
    private FolderPicker.ParentMode parentMode;
    private MultipleWorker multipleWorker;

    private boolean isOpenedFromFolderPicker;
    void configure(FolderPicker folderPicker, FragmentManager fragmentManager, File parentDirectory, File startingDirectory, Boolean isSelectedAll){
        this.isOpenedFromFolderPicker = true;
        this.fragmentManager = fragmentManager;
        this.folderPicker = folderPicker;
        this.parentDirectory = parentDirectory;
        this.startingDirectory = startingDirectory;
        this.isSelectedAll = isSelectedAll;

        this.parentMode = this.folderPicker.getParentMode();
        this.multipleWorker = this.folderPicker.getMultipleWorker();
//        this.multipleWorker.execute(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(!this.isOpenedFromFolderPicker){
            throw new IllegalStateException(IS_NOT_OPENED_FROM_FOLDER_PICKER);
        }
        this.context = context;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        switch(this.parentMode){
            case FULL_DIRECTORY:
                this.textViewFolderName.setText(this.parentDirectory.getPath());
                break;
            case EXCLUDE_PARENT:
                File parent = this.folderPicker.getStartingDirectory();
                String result = this.parentDirectory.getPath().replace(parent.getParent(), "").substring(1);
                this.textViewFolderName.setText(result);
                break;
            case NAME_ONLY:
                this.textViewFolderName.setText(this.parentDirectory.getName());
                break;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_child, container, false);
        this.constraintLayoutOperation = view.findViewById(R.id.constraint_layout_operation);
        this.imageButtonBack = view.findViewById(R.id.image_button_back);
        this.textViewFolderName = view.findViewById(R.id.text_view_folder_name);
        this.checkBoxSelectAll = view.findViewById(R.id.check_box_select_all);
        this.recyclerViewListFile = view.findViewById(R.id.recycler_view_list_file);
        this.folderAdapter = new FolderAdapter(this.context, this.folderPicker, this.fragmentManager, this.startingDirectory, this.checkBoxSelectAll, this.isSelectedAll);
        this.recyclerViewListFile.setAdapter(this.folderAdapter);
        this.recyclerViewListFile.setLayoutManager(new LinearLayoutManager(this.context));
        this.recyclerViewListFile.setBackgroundColor(this.folderPicker.getBodyColor());
        this.constraintLayoutOperation.setBackgroundColor(this.folderPicker.getNavigationBarColor());

        switch(this.parentMode){
            case FULL_DIRECTORY:
                this.textViewFolderName.setText(this.startingDirectory.getPath());
                break;
            case EXCLUDE_PARENT:
                File parent = this.folderPicker.getStartingDirectory();
                String result = this.startingDirectory.getPath().replace(parent.getParent(), "").substring(1);
                this.textViewFolderName.setText(result);
                break;
            case NAME_ONLY:
                this.textViewFolderName.setText(this.startingDirectory.getName());
                break;
        }

        this.imageButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentManager.popBackStack();
            }
        });
//        if(this.isSelectedAll){
//            this.checkBoxSelectAll.setChecked(true);
//        }
//        this.checkBoxSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                folderAdapter.checkAll(isChecked);
//            }
//        });
        return view;
    }
}