package com.example.androidftpclient;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidftpclient.Adapter.FileAdapter;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class LocalFileDialog extends DialogFragment {
    public interface OnClickListener{
        public void onSelect(String path);
    }
    public OnClickListener onClickListener;
    public void setOnClickListener(OnClickListener listener){
        this.onClickListener = listener;
    }
    private RecyclerView recyclerView;
    private String basePath = Environment.getExternalStorageDirectory().toString();
    private String filePath = "";
    private FileAdapter fileAdapter;
    private TextView viewReturn;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_upload, null);

        viewReturn = view.findViewById(R.id.view_return_d);
        viewReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = filePath.lastIndexOf("/");
                filePath = filePath.substring(0,i==-1?0:i);
                fileAdapter.setFiles(FilesToFTPFiles(FileUtils.getFiles(basePath + filePath)));
            }
        });
        recyclerView = view.findViewById(R.id.recycler_view_files_u);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        fileAdapter = new FileAdapter(getContext());
        fileAdapter.setOnItemClickListener(new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FTPFile file) {
                if (file.getType() == FTPFile.DIRECTORY_TYPE) {
                    filePath = filePath + "/" + file.getName();
                    fileAdapter.setFiles(FilesToFTPFiles(FileUtils.getFiles(basePath + filePath)));
                }
            }

        });
        fileAdapter.setFiles(FilesToFTPFiles(FileUtils.getFiles(basePath + filePath)));
        recyclerView.setAdapter(fileAdapter);
        builder.setView(view)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickListener.onSelect(basePath+filePath);
                    }
                })
                .setNeutralButton("取消", null);
        return builder.create();
    }

    public FTPFile[] FilesToFTPFiles(File[] files){
        FTPFile[] ftpFiles = new FTPFile[files.length];
        for (int i = 0;i<files.length;i++){
            ftpFiles[i] = FileToFTPFile(files[i]);
        }
        return ftpFiles;
    }

    public FTPFile FileToFTPFile(File file){
        FTPFile ftpFile = new FTPFile();
        ftpFile.setName(file.getName());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(file.lastModified());
        ftpFile.setTimestamp(calendar);
        if (file.isDirectory())
            ftpFile.setType(FTPFile.DIRECTORY_TYPE);
        else
            ftpFile.setType(FTPFile.FILE_TYPE);
        return ftpFile;
    }
}
