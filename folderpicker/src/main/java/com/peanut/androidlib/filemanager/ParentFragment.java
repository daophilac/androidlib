package com.peanut.androidlib.filemanager;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ParentFragment extends Fragment {
    private Button buttonCancel;
    private Button buttonOk;
    private List<FolderPickerListener> listListener;
    public void registerListener(FolderPickerListener folderPickerListener){
        this.listListener.add(folderPickerListener);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent, container, false);
        this.listListener = new ArrayList<>();
        this.buttonCancel = view.findViewById(R.id.button_cancel);
        this.buttonOk = view.findViewById(R.id.button_ok);

        this.buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(FolderPickerListener listener : listListener){
                    listener.onCancel();
                }
                for(Fragment fragment : getFragmentManager().getFragments()){
                    getFragmentManager().beginTransaction().remove(fragment).commit();
                }
            }
        });
        return view;
    }

    public interface FolderPickerListener{
        void onCancel();
        List<File> onOk();
    }
}
