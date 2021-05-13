package com.example.androidftpclient.IOThread;

import android.os.Handler;
import android.os.Message;

import com.example.androidftpclient.FTPOperationProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UploadThread extends Thread{
    private static final int UPLOAD_SUCCESS = 1;
    private static final int UPLOAD_FAIL = 2;


    private String remotePath;
    private List<File> files;
    private FTPOperationProcessor FTPProcessor;
    private Handler handler;


    public UploadThread(String remotePath,List<File> files,FTPOperationProcessor FTPProcessor,Handler handler){
        this.files = files;
        this.remotePath = remotePath;
        this.FTPProcessor = FTPProcessor;
        this.handler = handler;
    }

    public void run(){
        try {
            FTPOperationProcessor.UploadStatus status;
            for (int i=0;i<files.size();i++){
                System.out.println(files.get(i).getAbsolutePath());
                String remotedirectory = remotePath;
                status = FTPProcessor.upload(files.get(i).getAbsolutePath(),remotedirectory+"/"+files.get(i).getName());
                Message msg = new Message();
                if (status == FTPOperationProcessor.UploadStatus.UPLOAD_NEW_FILE_SUCCESS || status == FTPOperationProcessor.UploadStatus.UPLOAD_FROM_BREAK_SUCCESS) {
                    msg.what = UPLOAD_SUCCESS;
                }
                else{
                    msg.what = UPLOAD_FAIL;
                }
                handler.sendMessage(msg);
                System.out.println("no."+i+"finish");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
