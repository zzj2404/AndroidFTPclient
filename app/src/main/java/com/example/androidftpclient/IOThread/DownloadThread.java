package com.example.androidftpclient.IOThread;

import android.os.Handler;
import android.os.Message;

import com.example.androidftpclient.FTPOperationProcessor;

import org.apache.commons.net.ftp.FTP;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class DownloadThread extends Thread{
    private static final int DOWNLOAD_SUCCESS = 3;
    private static final int DOWNLOAD_FAIL = 4;

    private String remoteFilePath;
    private String localPath;
    private FTPOperationProcessor FTPProcessor;
    private Handler handler;

    public DownloadThread(String remoteFilePath,String localPath,FTPOperationProcessor FTPProcessor,Handler handler){
        this.remoteFilePath = remoteFilePath;
        this.localPath = localPath;
        this.FTPProcessor = FTPProcessor;
        this.handler = handler;
    }

    public void run(){
        try {
            FTPOperationProcessor.DownloadStatus status = null;

            status = FTPProcessor.download(remoteFilePath,localPath);
            Message msg = new Message();
            System.out.println(status);
            if (status == FTPOperationProcessor.DownloadStatus.DOWNLOAD_FROM_BREAK_SUCCESS || status == FTPOperationProcessor.DownloadStatus.DOWNLOAD_NEW_SUCCESS) {
                msg.what = DOWNLOAD_SUCCESS;
            }
            else{
                msg.what = DOWNLOAD_FAIL;
            }
            handler.sendMessage(msg);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
