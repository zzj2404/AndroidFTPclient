package com.example.androidftpclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.androidftpclient.Adapter.FileAdapter;

import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

public class DownloadActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private String filePath = "";
    private FileAdapter fileAdapter;
    private FTPOperationProcessor ftpProcessor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        ftpProcessor = new FTPOperationProcessor(this);

        recyclerView = findViewById(R.id.recycler_view_files_d);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        fileAdapter = new FileAdapter(this);
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
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("path",filePath+"/"+file.getName());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
        fileAdapter.setFiles((FTPFile[]) getIntent().getSerializableExtra("list"));
        recyclerView.setAdapter(fileAdapter);
    }

    public void ReturnLastDirectory(View v){
        if (filePath.isEmpty()){
            Intent intent = new Intent();
            setResult(0,intent);
            finish();
        }
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
}