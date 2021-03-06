package com.example.androidftpclient;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidftpclient.Adapter.FileAdapter;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

public class UploadDialog extends DialogFragment {
    public interface OnClickListener{
        public void onUpload(String path);
    }
    public OnClickListener onClickListener;
    public void setOnClickListener(OnClickListener listener){
        this.onClickListener = listener;
    }
    private FTPFile[] files;
    private RecyclerView recyclerView;
    private String filePath = "";
    private FTPOperationProcessor ftpProcessor;
    private FileAdapter fileAdapter;
    private TextView viewReturn;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_upload, null);

        ftpProcessor =  new FTPOperationProcessor(getContext());

        viewReturn = view.findViewById(R.id.view_return_d);
        viewReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = filePath.lastIndexOf("/");
                filePath = filePath.substring(0,i==-1?0:i);
                try {
                    if (filePath.isEmpty())
                        fileAdapter.setFiles(ftpProcessor.GetFiles("/"));
                    else
                        fileAdapter.setFiles(ftpProcessor.GetFiles(filePath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                    try {
                        fileAdapter.setFiles(ftpProcessor.GetFiles(filePath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        fileAdapter.setFiles(files);
        recyclerView.setAdapter(fileAdapter);
        builder.setView(view)
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickListener.onUpload(filePath);
                    }
                })
                .setNeutralButton("??????", null);
        return builder.create();
    }

    public UploadDialog(FTPFile[] f){
        files = f;
    }
}
