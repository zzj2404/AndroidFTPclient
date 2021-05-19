package com.example.androidftpclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.androidftpclient.Adapter.FileAdapter;

import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

public class DirectoryOperationActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText inputDirectoryName;
    private String filePath = "";
    private FTPOperationProcessor ftpProcessor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_operation);

        ftpProcessor = new FTPOperationProcessor(this);

        inputDirectoryName = findViewById(R.id.DirectoryName);
        recyclerView = findViewById(R.id.recycler_view_d);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        FileAdapter fileAdapter = new FileAdapter(this);
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
        fileAdapter.setOnDeleteClickListener(new FileAdapter.OnDeleteClickListener() {
            @Override
            public void onDeleteClick(FTPFile file) {
                Intent intent = new Intent();
                intent.putExtra("operation","delete");
                intent.putExtra("path",filePath+"/"+file.getName());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        fileAdapter.setFiles((FTPFile[]) getIntent().getSerializableExtra("list"));
        recyclerView.setAdapter(fileAdapter);
    }

    public void Create(View v){
        String directoryName = inputDirectoryName.getText().toString();
        Intent intent = new Intent();
        intent.putExtra("operation","create");
        intent.putExtra("path",filePath+"/"+directoryName+"/");
        setResult(RESULT_OK, intent);
        finish();
    }
}